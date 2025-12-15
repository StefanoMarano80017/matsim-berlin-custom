package org.matsim.run;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.matsim.analysis.QsimTimingModule;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.SampleOptions;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleLinkSpeedCalculator;
import org.matsim.contrib.bicycle.BicycleLinkSpeedCalculatorDefaultImpl;
import org.matsim.contrib.bicycle.BicycleTravelTime;
import org.matsim.contrib.ev.EvConfigGroup; 
import org.matsim.contrib.vsp.scoring.RideScoringParamsFromCarParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import picocli.CommandLine;
import playground.vsp.ev.UrbanEVConfigGroup;
import playground.vsp.ev.UrbanEVModule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.matsim.CustomMonitor.ConfigRun.CustomModule;
import org.matsim.CustomMonitor.ConfigRun.SimulationWebSocketModule;


@CommandLine.Command(header = ":: Open Berlin Scenario ::", version = OpenBerlinScenario.VERSION, mixinStandardHelpOptions = true, showDefaultValues = true)
public class OpenBerlinScenario extends MATSimApplication {

    public static final String VERSION = "6.4";
    public static final String CRS = "EPSG:25832";

    /*
    *
    */
    private static ApplicationContext applicationContext; // Il contesto Spring
    private CustomModule customModule;
    private static Resource csvResourceHub;
    private static Resource csvResourceEv;
    private static String configPath;

    @CommandLine.Mixin
    private final SampleOptions sample = new SampleOptions(10, 25, 3, 1);
    protected Double sampleSizeStatic;

    @CommandLine.Option(names = "--plan-selector", description = "Plan selector to use.", defaultValue = DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
    private String planSelector;

    public OpenBerlinScenario() {
        super(String.format("matsim-berlin-custom/input/v%s/berlin-v%s.config.xml", VERSION, VERSION));
    }

    public void runScenario(Double sampleSize) {
        this.sampleSizeStatic = sampleSize;
        // --- 1. Carica Config dal file di default ---
        Config config = OpenBerlinScenario.loadConfigFromPath();
        
        // --- 2. Applica tutte le personalizzazioni di prepareConfig ---
        config = prepareConfig(config);

        // --- 3. Crea lo Scenario dal Config ---
        Scenario scenario = ScenarioUtils.loadScenario(config);
        prepareScenario(scenario);

        // --- 4. Crea il Controler e prepara moduli ---
        Controler controler = new Controler(scenario);
        prepareControler(controler);

        // --- 5. Avvia simulazione ---
        controler.run();
    }

    /**
     * Metodo statico pubblico per impostare il contesto Spring dall'esterno
     */
    public static void setSpringContext(ApplicationContext context) {
        applicationContext = context;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Config prepareConfig(Config config) {
        // --- Creazione EV config---
        EvConfigGroup evConfig = ConfigUtils.addOrGetModule(config, EvConfigGroup.class);
        evConfig.chargersFile = "fake_chargers.xml"; // Placeholder, i charger saranno gestiti dal HubManager
        UrbanEVConfigGroup urbanEVConfig = ConfigUtils.addOrGetModule( config, UrbanEVConfigGroup.class );
		urbanEVConfig.setCriticalSOC(0.2);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.none);
		//register charging interaction activities for car
		config.scoring().addActivityParams(
				new ActivityParams(TransportMode.car + UrbanEVModule.PLUGOUT_INTERACTION).setScoringThisActivityAtAll(false ) 
        );
		config.scoring().addActivityParams(
				new ActivityParams( TransportMode.car + UrbanEVModule.PLUGIN_INTERACTION).setScoringThisActivityAtAll( false ) 
        );
        config.scoring().addActivityParams(
            new ActivityParams("charging")
                .setScoringThisActivityAtAll(true) // La si valuta
                .setTypicalDuration(3600 * 2) // 8 ore di durata tipica (per il calcolo del punteggio)
                .setMinimalDuration(300) // Durata minima di 5 minuti
                // Se hai dei parametri di scoring specifici per "work", usali qui:
        );

        SimWrapperConfigGroup sw = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

        double sampleSize;
        if(this.sampleSizeStatic != null) {
            sampleSize = this.sampleSizeStatic;
        } else {
            sampleSize = sample.getSample();
        }

        config.qsim().setFlowCapFactor(sampleSize);
        config.qsim().setStorageCapFactor(sampleSize);

        // Counts can be scaled with sample size
        config.counts().setCountsScaleFactor(sampleSize);
        sw.sampleSize = sampleSize;

        config.controller().setRunId(sample.adjustName(config.controller().getRunId()));
        config.controller().setOutputDirectory(sample.adjustName(config.controller().getOutputDirectory()));
        config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));
    

        config.qsim().setUsingTravelTimeCheckInTeleportation(true);

        // overwrite ride scoring params with values derived from car
        RideScoringParamsFromCarParams.setRideScoringParamsBasedOnCarParams(config.scoring(), 1.0);
        Activities.addScoringParams(config, true);

        // Required for all calibration strategies        s
        for (String subpopulation : List.of("person", "freight", "goodsTraffic", "commercialPersonTraffic", "commercialPersonTraffic_service")) {
			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings()
					.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
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

        if (csvResourceHub == null || csvResourceEv == null) {
            throw new RuntimeException("CSV Resources non inizializzate. Chiama setHubCSV() e setEvCSV() prima di prepareScenario.");
        }

        this.customModule = new CustomModule(
            scenario, 
            csvResourceHub, 
            csvResourceEv, 
            1, 0.6, 0.15, 
            true
        );
        customModule.PrepareScenarioEV(scenario);
    }

    @Override
    protected void prepareControler(Controler controler) {
        controler.addOverridingModule(new UrbanEVModule());

        if (customModule != null) {
            if (applicationContext != null) {
                controler.addOverridingModule(
                    new SimulationWebSocketModule(applicationContext)
                );
            } 
            controler.addOverridingModule(customModule);
        } else{
            throw new RuntimeException("CustomModule non inizializzato correttamente.");
        }

        //Moduli standard 
        controler.addOverridingModule(new TravelTimeBinding());
        controler.addOverridingModule(new SimWrapperModule());
        controler.addOverridingModule(new QsimTimingModule());
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


    /*
    *   Utility di config
    */
    public static void setEvCSV(Resource csvResource){
        if (csvResource == null || !csvResource.exists()) {
            throw new RuntimeException("Resource EV non trovata: " + csvResource);
        }
        csvResourceEv = csvResource;
    }

    public static void setHubCSV(Resource csvResource){
        if (csvResource == null || !csvResource.exists()) {
            throw new RuntimeException("Resource HUB non trovata: " + csvResource);
        }
        csvResourceHub = csvResource;
    }

    public static void setConfigPath(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Config path non valido");
        }
        configPath = String.format(path, VERSION, VERSION);
    }

    public static Config loadConfigFromPath() {
        if (configPath == null) {
            throw new IllegalStateException("Config path non impostato. Chiama prima setConfigPath()");
        }
        return ConfigUtils.loadConfig(configPath);
    }

}