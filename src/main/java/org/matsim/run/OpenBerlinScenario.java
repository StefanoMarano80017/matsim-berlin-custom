package org.matsim.run;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.matsim.analysis.QsimTimingModule;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.SampleOptions;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleLinkSpeedCalculator;
import org.matsim.contrib.bicycle.BicycleLinkSpeedCalculatorDefaultImpl;
import org.matsim.contrib.bicycle.BicycleTravelTime;
import org.matsim.contrib.ev.EvConfigGroup; // Import del gruppo di configurazione EV
import org.matsim.contrib.ev.EvModule; // Import del Modulo EV
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecificationDefaultImpl;
import org.matsim.contrib.vsp.scoring.RideScoringParamsFromCarParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.run.scoring.AdvancedScoringConfigGroup;
import org.matsim.run.scoring.AdvancedScoringModule;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import picocli.CommandLine;
import playground.vsp.ev.UrbanEVConfigGroup;
import playground.vsp.ev.UrbanEVModule;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;
import com.google.inject.Provider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.CustomMonitor.TimeStepMonitor;
import org.matsim.CustomMonitor.ChargingHub.HubManager;
import org.matsim.CustomMonitor.EVfleet.EvFleetManager;
import org.matsim.CustomMonitor.EVfleet.QuickLinkDebugHandler;
// IMPORTS AGGIUNTI
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.CustomMonitor.EVfleet.EvConsumptionModelFactory;
// END IMPORTS AGGIUNTI


@CommandLine.Command(header = ":: Open Berlin Scenario ::", version = OpenBerlinScenario.VERSION, mixinStandardHelpOptions = true, showDefaultValues = true)
public class OpenBerlinScenario extends MATSimApplication {

    private HubManager hubManager;
    private ChargingInfrastructureSpecification infraSpec;
    private EvFleetManager evFleetManager;

    public static final String VERSION = "6.4";
    public static final String CRS = "EPSG:25832";

    @CommandLine.Mixin
    private final SampleOptions sample = new SampleOptions(10, 25, 3, 1);

    @CommandLine.Option(names = "--plan-selector",
        description = "Plan selector to use.",
        defaultValue = DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
    private String planSelector;

    public OpenBerlinScenario() {
        super(String.format("input/v%s/berlin-v%s.config.xml", VERSION, VERSION));
    }

    public static void main(String[] args) {
        MATSimApplication.run(OpenBerlinScenario.class, args);
    }

    @Override
    protected Config prepareConfig(Config config) {

        Path csvPath_hub = Path.of("input/CustomInput/charging_hub.csv");
        if (!Files.exists(csvPath_hub)) {
            throw new RuntimeException("File HUB non trovato: " + csvPath_hub.toAbsolutePath());
        }

        Path csvPath_ev = Path.of("input/CustomInput/ev-dataset.csv");
        if (!Files.exists(csvPath_ev)) {
            throw new RuntimeException("File EV non trovato: " + csvPath_ev.toAbsolutePath());
        }

        // --- Creazione EV config---
        EvConfigGroup evConfig = ConfigUtils.addOrGetModule(config, EvConfigGroup.class);
        evConfig.chargersFile = "fake_chargers.xml"; // Placeholder, i charger saranno gestiti dal HubManager
        UrbanEVConfigGroup urbanEVConfig = ConfigUtils.addOrGetModule( config, UrbanEVConfigGroup.class );
		urbanEVConfig.setCriticalSOC(0.4);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.none );
		//register charging interaction activities for car
		config.scoring().addActivityParams(
				new ActivityParams(TransportMode.car + UrbanEVModule.PLUGOUT_INTERACTION).setScoringThisActivityAtAll(false ) );
		config.scoring().addActivityParams(
				new ActivityParams( TransportMode.car + UrbanEVModule.PLUGIN_INTERACTION).setScoringThisActivityAtAll( false ) );

        config.scoring().addActivityParams(
            new ActivityParams("charging")
                .setScoringThisActivityAtAll(true) // La si valuta
                .setTypicalDuration(3600 * 2) // 8 ore di durata tipica (per il calcolo del punteggio)
                .setMinimalDuration(300) // Durata minima di 5 minuti
                // Se hai dei parametri di scoring specifici per "work", usali qui:
        );

        SimWrapperConfigGroup sw = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

        // --- Configurazione sample se presente ---
        if (sample.isSet()) {
            double sampleSize = sample.getSample();

            config.qsim().setFlowCapFactor(sampleSize);
            config.qsim().setStorageCapFactor(sampleSize);

            // Counts can be scaled with sample size
            config.counts().setCountsScaleFactor(sampleSize);
            sw.sampleSize = sampleSize;

            config.controller().setRunId(sample.adjustName(config.controller().getRunId()));
            config.controller().setOutputDirectory(sample.adjustName(config.controller().getOutputDirectory()));
            config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));
        }

        config.qsim().setUsingTravelTimeCheckInTeleportation(true);

        // overwrite ride scoring params with values derived from car
        RideScoringParamsFromCarParams.setRideScoringParamsBasedOnCarParams(config.scoring(), 1.0);
        Activities.addScoringParams(config, true);

        // Required for all calibration strategies
		for (String subpopulation : List.of("person", "freight", "goodsTraffic", "commercialPersonTraffic", "commercialPersonTraffic_service")) {
			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings()
					.setStrategyName(planSelector)
					.setWeight(1.0)
					.setSubpopulation(subpopulation)
			);

			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings()
					.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute)
					.setWeight(0.15)
					.setSubpopulation(subpopulation)
			);
		}

		config.replanning().addStrategySettings(
			new ReplanningConfigGroup.StrategySettings()
				.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator)
				.setWeight(0.15)
				.setSubpopulation("person")
		);

		config.replanning().addStrategySettings(
			new ReplanningConfigGroup.StrategySettings()
				.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice)
				.setWeight(0.15)
				.setSubpopulation("person")
		);

        // --- Forziamo il Re-routing su TUTTI gli agenti nell'Iterazione 0 ---
        ReplanningConfigGroup replanningConfig = config.replanning();

        StrategySettings initialReRoute = new StrategySettings();
        initialReRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
        initialReRoute.setWeight(1.0); // 100% degli agenti nell'iterazione 0
        initialReRoute.setDisableAfter(1); // Eseguito solo nell'iterazione 0

        // Questo sovrascriverà temporaneamente la strategia di routing a peso 0.15 
        // per la prima iterazione, garantendo che anche i nuovi agenti vengano processati.
        replanningConfig.addStrategySettings(initialReRoute);
        config.qsim().setUsePersonIdForMissingVehicleId(false);
        // --- Aggiungi Configurazione EV (file esterni) ---
        ConfigUtils.addOrGetModule(config, EvConfigGroup.class);
        
        // Bicycle config must be present
        ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);

        // Forza l'esecuzione singola (Solo 1 mobsim, niente loop)
        config.controller().setLastIteration(1);

        // disattivo la scrittura della popolazione e dei piani poiché sono già in un stato ottimizzato
        //config.controller().setWritePlansInterval(1); // Non scrivere i plans ad ogni iterazione
        //config.controller().setDumpDataAtEnd(false); // Non riscrivere nulla a fine run
        config.controller().setWriteEventsInterval(1); // Mantieni gli eventi


        PlansConfigGroup plansConfig = config.plans();
        // Imposta la gestione della modalità di routing mancante.
        plansConfig.setHandlingOfPlansWithoutRoutingMode(PlansConfigGroup.HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);

        return config;
    }

    @Override
    protected void prepareScenario(Scenario scenario) {
        // --- MODIFICA EV: Creazione Hub e Manager ---
        this.infraSpec = new ChargingInfrastructureSpecificationDefaultImpl ();
        this.hubManager = new HubManager(scenario.getNetwork(), infraSpec);
        // Carica hub da CSV
        this.hubManager.createHub(Path.of("input/CustomInput/charging_hub.csv")); 
        System.out.println(">>> HubManager: " + hubManager.getHubOccupancyMap().size() + " hub registrati.");
        // Genera la flotta EV
        this.evFleetManager = new EvFleetManager();
        this.evFleetManager.generateFleetFromCsv(Path.of("input/CustomInput/ev-dataset.csv"), scenario, 1, 0.6, 0.15);
        System.out.println(">>> Scenario preparato con Flotta EV e Hub.");

        // === DEBUG: VERIFICA INFRASTRUTTURA DI RICARICA (Aggiornato per l'interfaccia) ===
        System.out.println("--- DEBUG INFRASTRUTTURA ---");
        int totalChargers = 0;
        // Mappa temporanea per raggruppare le colonnine per Link ID
        Map<Id<Link>, List<ChargerSpecification>> chargersByLink = new HashMap<>();

        // 1. Itera su tutte le specifiche delle colonnine
        for (ChargerSpecification chargerSpec : infraSpec.getChargerSpecifications().values()) {
            Id<Link> linkId = chargerSpec.getLinkId();
            // Raggruppa le specifiche per Link ID
            chargersByLink.computeIfAbsent(linkId, k -> new ArrayList<>()).add(chargerSpec);
            totalChargers += chargerSpec.getPlugCount(); // Contiamo il numero di plugs, non solo di Charger ID
        }

        // 2. Itera sui Link che ora sappiamo contengono colonnine
        for (Id<Link> linkId : chargersByLink.keySet()) {
            
            List<ChargerSpecification> specsOnLink = chargersByLink.get(linkId);
            
            // Calcoliamo il totale delle prese su questo link sommando i plugCount
            int totalPlugsOnLink = specsOnLink.stream().mapToInt(ChargerSpecification::getPlugCount).sum();
            
            System.out.printf("## Link ID: %s | Colonnine univoche: %d | Totale Prese: %d%n", 
                linkId, 
                specsOnLink.size(),
                totalPlugsOnLink);
            
            // 3. Itera su ogni singola colonnina (Charger) su questo Link
            for (ChargerSpecification chargerSpec : specsOnLink) {
                
                Id<Charger> chargerId = chargerSpec.getId();
                String chargerType = chargerSpec.getChargerType();
                double plugPower = chargerSpec.getPlugPower();
                int plugCount = chargerSpec.getPlugCount();
                String hubId = (String) chargerSpec.getAttributes().getAttribute("hubId"); 

                System.out.printf("  - ID Colonnina: %s, Tipo: %s, Potenza per presa: %.1f kW, Prese: %d, Hub ID: %s%n",
                    chargerId,
                    chargerType,
                    plugPower / 1000.0, // Conversione da W a kW
                    plugCount,
                    hubId != null ? hubId : "N/D"
                );
            }
        }

        System.out.println("---------------------------------");
        System.out.println(">>> TOTALE Link con Hub: " + chargersByLink.size());
        System.out.println(">>> TOTALE Prese (Plugs) Registrate: " + totalChargers);
        System.out.println("---------------------------------");
        //throw new RuntimeException("FINE DEBUG INFRASTRUTTURA - FERMO ESECUZIONE");

        registerDefaultVehicles(scenario);
    }

    @Override
    protected void prepareControler(Controler controler) {
        // --- MODIFICA EV: Modulo e Listener EV ---
        // Installa il Modulo EV principale (gestisce la fisica della batteria e ricarica)
       	controler.addOverridingModule(new UrbanEVModule());

        // Registra il Monitor (Gestisce log periodico del SOC e conteggio Hub)
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                // Sovrascrive l'implementazione di default di MATSim con la tua istanza
                bind(ChargingInfrastructureSpecification.class).toInstance(infraSpec);
                // Binding dell'EvFleetManager già istanziato in prepareScenario
                bind(EvFleetManager.class).toInstance(evFleetManager);
                bind(HubManager.class).toInstance(hubManager);
                bind(TimeStepMonitor.class).asEagerSingleton();
                //Setto il parametro step size per il monitor
                bind(Double.class).annotatedWith(Names.named("timeStepMonitorStep")).toInstance(900.0);
                // Listener che scatta ogni TimeStep per monitorare SOC e Hub
                addMobsimListenerBinding().to(TimeStepMonitor.class);
                addEventHandlerBinding().toInstance(hubManager);
                addEventHandlerBinding().toInstance(evFleetManager); 
                // === INSERIMENTO MODELLO DI CONSUMO CUSTOM ===
                bind(DriveEnergyConsumption.Factory.class).toProvider(new Provider<DriveEnergyConsumption.Factory>() {
                    // B. Guice inietterà l'EvFleetManager qui (che è bindato toInstance)
                    @Inject
                    private EvFleetManager providerEvFleetManager; 
                    // C. Questo metodo crea e restituisce l'istanza della Factory
                    @Override
                    public DriveEnergyConsumption.Factory get() {
                        // Qui istanziamo la Factory, passandole il Manager iniettato sopra (B)
                        return new EvConsumptionModelFactory(this.providerEvFleetManager);
                    }
                }).asEagerSingleton();
            }
        });

        controler.addOverridingModule(new SimWrapperModule());
        controler.addOverridingModule(new TravelTimeBinding());
        controler.addOverridingModule(new QsimTimingModule());

        // ================= DEBUG ==========================
        Set<Id<Vehicle>> electricVehicleIds = Collections.unmodifiableSet(this.evFleetManager.getFleet().keySet());
        QuickLinkDebugHandler debugHandler = new QuickLinkDebugHandler(electricVehicleIds);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(debugHandler);
            }
        });

        // AdvancedScoring is specific to matsim-berlin!
        if (ConfigUtils.hasModule(controler.getConfig(), AdvancedScoringConfigGroup.class)) {
            controler.addOverridingModule(new AdvancedScoringModule());
            controler.getConfig().scoring().setExplainScores(true);
        } else {
            // if the above config group is not present we still need income dependent scoring
            // this implementation also allows for person specific asc
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).asEagerSingleton();
                }
            });
        }
        controler.addOverridingModule(new PersonMoneyEventsAnalysisModule());
    }

    /**
     * Add travel time bindings for ride and freight modes, which are not actually network modes.
     */
    public static final class TravelTimeBinding extends AbstractModule {

        private final boolean carOnly;

        public TravelTimeBinding() {
            this.carOnly = false;
        }

        public TravelTimeBinding(boolean carOnly) {
            this.carOnly = carOnly;
        }

        @Override
        public void install() {
            addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
            addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());

            if (!carOnly) {
                addTravelTimeBinding("freight").to(Key.get(TravelTime.class, Names.named(TransportMode.truck)));
                addTravelDisutilityFactoryBinding("freight").to(Key.get(TravelDisutilityFactory.class, Names.named(TransportMode.truck)));


                bind(BicycleLinkSpeedCalculator.class).to(BicycleLinkSpeedCalculatorDefaultImpl.class);

                // Bike should use free speed travel time
                addTravelTimeBinding(TransportMode.bike).to(BicycleTravelTime.class);
                addTravelDisutilityFactoryBinding(TransportMode.bike).to(OnlyTimeDependentTravelDisutilityFactory.class);
            }
        }
    }


// Rinomino la funzione per riflettere che gestisce più modi default
    static void registerDefaultVehicles(Scenario scenario) { 
        VehiclesFactory vehicleFactory = scenario.getVehicles().getFactory();

        // =======================================================
        // 1. GESTIONE WALK (Come risolto precedentemente)
        // =======================================================
        
        VehicleType walkVehicleType = vehicleFactory.createVehicleType(Id.create("default_walk_type", VehicleType.class));
        walkVehicleType.setNetworkMode(TransportMode.walk);
        scenario.getVehicles().addVehicleType(walkVehicleType);
        
        Vehicle defaultWalkVehicle = vehicleFactory.createVehicle(Id.create("default_walk_vehicle", Vehicle.class), walkVehicleType);
        scenario.getVehicles().addVehicle(defaultWalkVehicle);
        
        Id<Vehicle> walkVehicleId = defaultWalkVehicle.getId();

        // =======================================================
        // 2. NUOVO: GESTIONE PT (Trasporto Pubblico)
        // =======================================================
        
        // Crea un veicolo fittizio per il Trasporto Pubblico (PT)
        VehicleType ptVehicleType = vehicleFactory.createVehicleType(Id.create("default_pt_type", VehicleType.class));
        ptVehicleType.setNetworkMode(TransportMode.pt); // Importante: deve essere "pt"
        scenario.getVehicles().addVehicleType(ptVehicleType);
        
        // Un veicolo unico di default per PT da assegnare a tutti
        Vehicle defaultPtVehicle = vehicleFactory.createVehicle(Id.create("default_pt_vehicle", Vehicle.class), ptVehicleType);
        scenario.getVehicles().addVehicle(defaultPtVehicle);
        
        Id<Vehicle> ptVehicleId = defaultPtVehicle.getId();


        // =======================================================
        // 3. ASSEGNAZIONE A TUTTI GLI AGENTI
        // =======================================================
        
        for (Person person : scenario.getPopulation().getPersons().values()) {
            
            Map<String, Id<Vehicle>> mode2Vehicle;
            
            try {
                // Tenta di recuperare la mappa esistente (dovrebbe contenere car/bike)
                mode2Vehicle = VehicleUtils.getVehicleIds(person);
            } catch (RuntimeException e) {
                // Se non esiste, crea una nuova mappa
                mode2Vehicle = new HashMap<>();
            }
            
            // Aggiungi l'associazione walk
            mode2Vehicle.put(TransportMode.walk, walkVehicleId); 
            
            // NUOVO: Aggiungi l'associazione pt
            mode2Vehicle.put(TransportMode.pt, ptVehicleId); 
            
            VehicleUtils.insertVehicleIdsIntoAttributes(person, mode2Vehicle);
        }
    }

}