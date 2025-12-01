package org.matsim.run;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.matsim.analysis.QsimTimingModule;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.SampleOptions;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleLinkSpeedCalculator;
import org.matsim.contrib.bicycle.BicycleLinkSpeedCalculatorDefaultImpl;
import org.matsim.contrib.bicycle.BicycleTravelTime;
import org.matsim.contrib.ev.EvConfigGroup; // Import del gruppo di configurazione EV
import org.matsim.contrib.ev.EvModule; // Import del Modulo EV
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecificationDefaultImpl;
import org.matsim.contrib.vsp.scoring.RideScoringParamsFromCarParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
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

import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.matsim.CustomMonitor.TimeStepMonitor;
import org.matsim.CustomMonitor.ChargingHub.HubManager;
import org.matsim.CustomMonitor.EVfleet.EvFleetManager;
import org.matsim.CustomMonitor.EVfleet.QuickLinkDebugHandler;
// IMPORTS AGGIUNTI
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
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
        config.controller().setWritePlansInterval(1); // Non scrivere i plans ad ogni iterazione
        config.controller().setDumpDataAtEnd(false); // Non riscrivere nulla a fine run
        config.controller().setWriteEventsInterval(1); // Mantieni gli eventi

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
        this.evFleetManager.generateFleetFromCsv(Path.of("input/CustomInput/ev-dataset.csv"), scenario, 1, 0.7, 0.15);
        System.out.println(">>> Scenario preparato con Flotta EV e Hub.");
    }

    @Override
    protected void prepareControler(Controler controler) {
        
        // --- MODIFICA EV: Modulo e Listener EV ---
        // Installa il Modulo EV principale (gestisce la fisica della batteria e ricarica)
        controler.addOverridingModule(new EvModule());
        controler.addOverridingModule(new SimWrapperModule());
        controler.addOverridingModule(new TravelTimeBinding());
        controler.addOverridingModule(new QsimTimingModule());
        // Registra il Monitor (Gestisce log periodico del SOC e conteggio Hub)
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                // Binding dell'EvFleetManager già istanziato in prepareScenario
                // Questo è cruciale per l'iniezione nel modello di consumo.
                bind(EvFleetManager.class).toInstance(evFleetManager);
                bind(HubManager.class).toInstance(hubManager);
                bind(TimeStepMonitor.class).asEagerSingleton();
                //Setto il parametro step size per il monitor
                bind(Double.class).annotatedWith(Names.named("timeStepMonitorStep")).toInstance(300.0);
                // Listener che scatta ogni TimeStep per monitorare SOC e Hub
                addMobsimListenerBinding().to(TimeStepMonitor.class);
                addEventHandlerBinding().toInstance(hubManager);
                addEventHandlerBinding().toInstance(evFleetManager); 

                // === INSERIMENTO MODELLO DI CONSUMO CUSTOM ===
                bind(DriveEnergyConsumption.Factory.class).to(EvConsumptionModelFactory.class).asEagerSingleton();

                // 2. Imposta AuxEnergyConsumption a 0.0. 
                // Questo disabilita il consumo ausiliario separato, assumendo che sia 
                // già incluso nel dato medio kWh/km fornito dal dataset.
                bind(AuxEnergyConsumption.Factory.class).toInstance(
                    electricVehicle -> ( beginTime, duration, linkId ) -> 0.0 // Consumo ausiliario 0 Joule
                );
                
                // ===========================================
            }
        });

        // ================= DEBUG ==========================
        Scenario scenario = controler.getScenario();
        Set<Id<Vehicle>> electricVehicleIds = Collections.unmodifiableSet(this.evFleetManager.getFleet().keySet());
        QuickLinkDebugHandler debugHandler = new QuickLinkDebugHandler(electricVehicleIds, scenario);
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

}