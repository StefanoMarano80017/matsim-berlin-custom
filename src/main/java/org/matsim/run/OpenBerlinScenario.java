package org.matsim.run;

import org.matsim.CustomMonitor.ChargingHub.TargetSocChargingHandler;
import org.matsim.CustomMonitor.ConfigRun.ConfigRun;
import org.matsim.CustomMonitor.ConfigRun.CustomModule;
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
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
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
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;

import com.google.inject.Key;
import com.google.inject.name.Names;

import picocli.CommandLine;
import playground.vsp.ev.UrbanEVConfigGroup;

import java.util.List;

@CommandLine.Command(header = ":: Open Berlin Scenario ::", version = OpenBerlinScenario.VERSION, mixinStandardHelpOptions = true, showDefaultValues = true)
public class OpenBerlinScenario extends MATSimApplication {

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
        this.sampleSizeStatic = configRun.GetsampleSizeStatic();
        return this;
    }

    public static void main(String[] args) {
		MATSimApplication.run(OpenBerlinScenario.class, args);
	}

    public void runScenario() {
        if (configRun == null) {
            throw new IllegalStateException("ConfigRun non inizializzato. Usa il builder ConfigRun prima di runScenario.");
        }

        // --- 1. Carica Config dal file impostato nel ConfigRun ---
        Config config = ConfigUtils.loadConfig(String.format(configRun.getConfigPath(), VERSION, VERSION));

        // --- 2. Applica tutte le personalizzazioni di prepareConfig ---
        config = prepareConfig(config);

        // --- 3. Crea lo Scenario dal Config ---
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // --- 4. Prepara il CustomModule tramite ConfigRun ---
        CustomModule customModule = new CustomModule(
                scenario,
                configRun.getCsvResourceHub(),
                configRun.getCsvResourceEv(),
                1, 0.70, 0.15,
                configRun.isDebug(),
                configRun.isPublishOnSpring()
        );
        customModule.PrepareScenarioEV(scenario);

        // --- 5. Prepara il Controler e avvia ---
        Controler controler = new Controler(scenario);
        prepareControler(controler, customModule);

        controler.run();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Config prepareConfig(Config config) {
        // --- Creazione EV config---
        EvConfigGroup evConfig = ConfigUtils.addOrGetModule(config, EvConfigGroup.class);
        evConfig.chargersFile = "fake_chargers.xml"; // Placeholder
        UrbanEVConfigGroup urbanEVConfig = ConfigUtils.addOrGetModule(config, UrbanEVConfigGroup.class);
        urbanEVConfig.setCriticalSOC(0.9);
        config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.none);

        // Register EV charging activities
        config.scoring().addActivityParams(
            new ActivityParams("car charging")
                    .setScoringThisActivityAtAll(false)
                    .setTypicalDuration(3600 * 2)
                    .setMinimalDuration(300)
        );

        SimWrapperConfigGroup sw = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
        double sampleSize = (this.sampleSizeStatic != null) ? this.sampleSizeStatic : sample.getSample();
        config.qsim().setFlowCapFactor(sampleSize);
        config.qsim().setStorageCapFactor(sampleSize);
        config.counts().setCountsScaleFactor(sampleSize);
        sw.sampleSize = sampleSize;
        config.controller().setRunId(sample.adjustName(config.controller().getRunId()));
        config.controller().setOutputDirectory(sample.adjustName(config.controller().getOutputDirectory()));
        config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));

        config.qsim().setUsingTravelTimeCheckInTeleportation(true);
        RideScoringParamsFromCarParams.setRideScoringParamsBasedOnCarParams(config.scoring(), 1.0);

        Activities.addScoringParams(config, true);

        // Strategia di replanning
        for (String subpopulation : List.of("person", "freight", "goodsTraffic", "commercialPersonTraffic", "commercialPersonTraffic_service")) {
            config.replanning().addStrategySettings(
                    new StrategySettings()
                            .setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
                            .setWeight(1.0)
                            .setSubpopulation(subpopulation)
            );
            config.replanning().addStrategySettings(
                    new StrategySettings()
                            .setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute)
                            .setWeight(0.15)
                            .setSubpopulation(subpopulation)
            );
        }

        config.replanning().addStrategySettings(
                new StrategySettings()
                    .setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator)
                    .setWeight(0.15).setSubpopulation("person")
        );
        
        config.replanning().addStrategySettings(
                new StrategySettings()
                    .setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice)
                    .setWeight(0.15).setSubpopulation("person")
        );

        // Forziamo ReRoute Iterazione 0
        StrategySettings initialReRoute = new StrategySettings();
        initialReRoute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
        initialReRoute.setWeight(1.0);
        initialReRoute.setDisableAfter(1);
        config.replanning().addStrategySettings(initialReRoute);

        config.qsim().setUsePersonIdForMissingVehicleId(false);

        // Bicycle config
        ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);

        // Single iteration
        config.controller().setLastIteration(1);
        config.controller().setWriteEventsInterval(1);

        PlansConfigGroup plansConfig = config.plans();
        plansConfig.setHandlingOfPlansWithoutRoutingMode(PlansConfigGroup.HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);

        return config;
    }

    protected void prepareControler(Controler controler, CustomModule customModule) {
        controler.addOverridingModule(new EvModule());
        controler.addOverridingModule(customModule);
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