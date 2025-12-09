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
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.springboot.websocket.SimulationEventPublisher;
import org.springframework.context.ApplicationContext;

import picocli.CommandLine;
import playground.vsp.ev.UrbanEVConfigGroup;
import playground.vsp.ev.UrbanEVModule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.matsim.CustomMonitor.SimulationWebSocketModule;
import org.matsim.CustomMonitor.ConfigRun.CustomModule;


@CommandLine.Command(header = ":: Open Berlin Scenario ::", version = OpenBerlinScenario.VERSION, mixinStandardHelpOptions = true, showDefaultValues = true)
public class OpenBerlinScenario extends MATSimApplication {

    private static ApplicationContext applicationContext; // Il contesto Spring

    public static final String VERSION = "6.4";
    public static final String CRS = "EPSG:25832";

    @CommandLine.Mixin
    private final SampleOptions sample = new SampleOptions(10, 25, 3, 1);
    protected Double sampleSizeStatic;
    private CustomModule customModule;

    @CommandLine.Option(names = "--plan-selector", description = "Plan selector to use.", defaultValue = DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
    private String planSelector;

   
    public OpenBerlinScenario() {
        super(String.format("input/v%s/berlin-v%s.config.xml", VERSION, VERSION));
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
        Path hubPath = Path.of("input/CustomInput/charging_hub.csv");
        Path evDatasetPath = Path.of("input/CustomInput/ev-dataset.csv");
        this.customModule = new CustomModule(
            scenario, 
            hubPath, 
            evDatasetPath, 
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
            } else {
                throw new IllegalStateException(
                    "SPRING_CONTEXT_STATIC non è stato impostato. La simulazione non può avviare il bridge WebSocket."
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
}