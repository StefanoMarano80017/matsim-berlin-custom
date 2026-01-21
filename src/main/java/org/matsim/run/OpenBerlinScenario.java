package org.matsim.run;

import java.util.List;

/*
*  Standard libs
*/
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
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.vsp.scoring.RideScoringParamsFromCarParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
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
import org.matsim.simwrapper.SimWrapperModule;
import org.springboot.service.GenerationService.DTO.HubSpecDto;
/*
*  Custom Libs
*/
import org.matsim.CustomEvModule.CustomEvContext;
import org.matsim.CustomEvModule.CustomEvModule;

import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.matsim.ServerEvSetup.ConfigRun.ConfigRun;
import org.matsim.ServerEvSetup.SimulationInterface.SimulationBridgeInterface;
/*
*  Guice 
*/
import com.google.inject.Key;
import com.google.inject.name.Names;
/*
*  CLI
*/
import picocli.CommandLine;

@CommandLine.Command(header = ":: Open Berlin Scenario ::", version = OpenBerlinScenario.VERSION, mixinStandardHelpOptions = true, showDefaultValues = true)
public class OpenBerlinScenario extends MATSimApplication {

    private Controler controler;

    /*
    * Parametri standard simulazione
    */
    public static final String VERSION = "6.4";
    public static final String CRS = "EPSG:25832";

    // Tutti i parametri della run
    private ConfigRun configRun; 

    @CommandLine.Mixin
    private final SampleOptions sample = new SampleOptions(10, 25, 3, 1);
    private Double sampleSizeStatic;

    @CommandLine.Option(names = "--plan-selector", description = "Plan selector to use.", defaultValue = DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
    private String planSelector;

    public OpenBerlinScenario() {
        super(String.format("input/v%s/berlin-v%s.config.xml", VERSION, VERSION));
        this.sampleSizeStatic = null;
    }

    // Builder fluido per configurare la run
    public OpenBerlinScenario withConfigRun(ConfigRun configRun) {
        this.configRun = configRun;
        this.sampleSizeStatic = configRun.getSampleSizeStatic();
        return this;
    }

    public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinScenario.class, args);
	}

    /**
     * Setup simulazione con modelli pre-generati dal server (specifiche pure).
     * 
     * Flusso server-driven:
     * 1. Server genera HubSpecDto (modelli di dominio puri)
     * 2. Vengono passati al bridge e di qui alla simulazione
     * 3. CustomEvContext li traduce in ChargingHub + specifiche MATSim
     * 
     * @param evModels Lista di modelli EV pre-generati dal server
     * @param hubSpecs Lista di specifiche hub pure (dal server)
     * @return SimulationBridgeInterface per accedere ai dati della simulazione
     */
    public SimulationBridgeInterface SetupSimulationWithServerModels(List<EvModel> evModels, List<HubSpecDto> hubSpecs) {
        if (configRun == null) {
            throw new IllegalStateException(
                "ConfigRun non inizializzato. Usa il builder ConfigRun prima di SetupSimulationWithServerModels."
            );
        }

        if (evModels == null || evModels.isEmpty()) {
            throw new IllegalArgumentException("evModels cannot be null or empty");
        }

        if (hubSpecs == null || hubSpecs.isEmpty()) {
            throw new IllegalArgumentException("hubSpecs cannot be null or empty");
        }

        // 1. Config
        Config config = ConfigUtils.loadConfig(
            String.format(configRun.getConfigPath(), VERSION, VERSION)
        );

        // 2. Custom config
        config = prepareConfig(config);

        // 3. Scenario
        Scenario scenario = ScenarioUtils.loadScenario(config);
        
        // 4. CustomEvContext  SPECIFICHE HUB PURE dal server -> manager nella simulazione
        CustomEvContext context = new CustomEvContext(
            scenario, 
            configRun, 
            evModels, 
            hubSpecs
        );

        // 6. Bridge 
        SimulationBridgeInterface bridge = new SimulationBridgeInterface(
            context.getEvFleetManager(),
            context.getHubManager()
        );
        
        CustomEvModule module = new CustomEvModule(
            bridge,
            context.getHubManager(),
            context.getEvFleetManager(),
            context.getInfraSpec(),
            configRun
        );

        // 5. Controller
        Controler controler = new Controler(scenario);
        prepareControler(controler);
        controler.addOverridingModule(module);
        controler.addControlerListener(bridge);
        this.controler = controler;
        return bridge;
    }

    public void run(){
        this.controler.run();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Config prepareConfig(Config config) {
        // --- Creazione EV config---
        SetEvModule(config);

        config.routing().setAccessEgressType(
            RoutingConfigGroup.AccessEgressType.none
        );

        SetSampleSize(config);

        config.controller().setRunId(
            sample.adjustName(
                config.controller().getRunId()
            )
        );
        config.controller().setOutputDirectory(
            sample.adjustName(
                config.controller().getOutputDirectory()
            )
        );
        config.plans().setInputFile(
            sample.adjustName(
                config.plans().getInputFile()
            )
        );

        config.qsim().setUsingTravelTimeCheckInTeleportation(true);
        RideScoringParamsFromCarParams.setRideScoringParamsBasedOnCarParams(config.scoring(), 1.0);

        Activities.addScoringParams(config, true);

        // Forziamo ReRoute Iterazione 0
        SetInitialReRoute(config);

        config.qsim().setUsePersonIdForMissingVehicleId(false);

        // Bicycle config
        ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);

        PlansConfigGroup plansConfig = config.plans();
        plansConfig.setHandlingOfPlansWithoutRoutingMode(PlansConfigGroup.HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);

        return config;
    }

    /*
    * Standard No EV
    */
    @Override
    protected void prepareControler(Controler controler) {
        controler.addOverridingModule(new EvModule());
        controler.addOverridingModule(new TravelTimeBinding());
        controler.addOverridingModule(new SimWrapperModule());
        controler.addOverridingModule(new QsimTimingModule());
        controler.addOverridingModule(new PersonMoneyEventsAnalysisModule());
    }

    /*
    *  Set Ev Module
    */
    private void SetEvModule(Config config){
        // --- Creazione EV config---
        EvConfigGroup evConfig = ConfigUtils.addOrGetModule(config, EvConfigGroup.class);
        evConfig.chargersFile = "fake_chargers.xml"; // Placeholder
        //UrbanEVConfigGroup urbanEVConfig = ConfigUtils.addOrGetModule(config, UrbanEVConfigGroup.class);
        //urbanEVConfig.setCriticalSOC(0.9);
        evConfig.chargeTimeStep = 5; //Timestep di avanzamneto SoC durante la ricarica
        // Register EV charging activities
        config.scoring().addActivityParams(
            new ActivityParams("car charging")
                    .setScoringThisActivityAtAll(false)
                    .setTypicalDuration(3600 * 2)
                    .setMinimalDuration(300)
        );
    }

    /*
    *   Set sample size
    */
    private void SetSampleSize(Config config){
        double sampleSize = (this.sampleSizeStatic != null) ? this.sampleSizeStatic : sample.getSample();
        config.qsim().setFlowCapFactor(sampleSize);
        config.qsim().setStorageCapFactor(sampleSize);
        config.counts().setCountsScaleFactor(sampleSize);
    }

    /*
    *  Set Initial ReRouting
    */
    private void SetInitialReRoute(Config config){
        // Forziamo ReRoute Iterazione 0
        StrategySettings initialReRoute = new StrategySettings();
        initialReRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
        initialReRoute.setWeight(1.0);
        initialReRoute.setDisableAfter(1);
        config.replanning().addStrategySettings(initialReRoute);
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