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
import org.matsim.contrib.ev.EvConfigGroup; // Import del gruppo di configurazione EV
import org.matsim.contrib.ev.EvModule; // Import del Modulo EV
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecificationDefaultImpl;
import org.matsim.contrib.vsp.scoring.RideScoringParamsFromCarParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.run.scoring.AdvancedScoringConfigGroup;
import org.matsim.run.scoring.AdvancedScoringModule;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import picocli.CommandLine;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import java.nio.file.Files;
import java.nio.file.Path;

import org.matsim.CustomMonitor.ChargingHub.HubManager;
import org.matsim.CustomMonitor.EVfleet.EvFleetManager;
import org.matsim.CustomMonitor.EVfleet.EvSocMonitor;


@CommandLine.Command(header = ":: Open Berlin Scenario ::", version = OpenBerlinScenario.VERSION, mixinStandardHelpOptions = true, showDefaultValues = true)
public class OpenBerlinScenario extends MATSimApplication {

	private HubManager hubManager;
	private ChargingInfrastructureSpecification infraSpec;

	public static final String VERSION = "6.4";
	public static final String CRS = "EPSG:25832";

	@CommandLine.Mixin
	private final SampleOptions sample = new SampleOptions(10, 25, 3, 1);

	@CommandLine.Option(names = "--plan-selector",
		description = "Plan selector to use.",
		defaultValue = DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
	private String planSelector;


	private final EvFleetManager evFleetManager = new EvFleetManager();


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

		SimWrapperConfigGroup sw = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

		// --- Creazione EV config---
		EvConfigGroup evConfig = ConfigUtils.addOrGetModule(config, EvConfigGroup.class);
		evConfig.chargersFile = "fake_chargers.xml"; // Placeholder, i charger saranno gestiti dal HubManager

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

		// --- Aggiunge strategia fittizia ChangeExpBeta per VSP ---
		ReplanningConfigGroup.StrategySettings dummyStrategy = new ReplanningConfigGroup.StrategySettings();
		dummyStrategy.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
		dummyStrategy.setWeight(0.0); // mai eseguita
		dummyStrategy.setSubpopulation("person"); // subpopulation fittizia
		config.replanning().addStrategySettings(dummyStrategy);

		config.qsim().setUsingTravelTimeCheckInTeleportation(true);

		// overwrite ride scoring params with values derived from car
		RideScoringParamsFromCarParams.setRideScoringParamsBasedOnCarParams(config.scoring(), 1.0);
		Activities.addScoringParams(config, true);

		// Forza l'esecuzione singola (Solo 1 mobsim, niente loop)
		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		// --- Aggiungi Configurazione EV (file esterni) ---
        ConfigUtils.addOrGetModule(config, EvConfigGroup.class);
		
		// Bicycle config must be present
		ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);
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
		EvFleetManager fleetManager = new EvFleetManager();
		fleetManager.generateFleetFromCsv(Path.of("input/CustomInput/ev-dataset.csv"), scenario, 50, 0.7, 0.15);
		System.out.println(">>> Scenario preparato con Flotta EV e Hub.");
	}

	@Override
	protected void prepareControler(Controler controler) {

		controler.addOverridingModule(new SimWrapperModule());
		controler.addOverridingModule(new TravelTimeBinding());
		controler.addOverridingModule(new QsimTimingModule());

		// --- MODIFICA EV: Modulo e Listener EV ---
		// 1. Installa il Modulo EV principale (gestisce la fisica della batteria e ricarica)
		controler.addOverridingModule(new EvModule());

		// 2. Registra il nostro Monitor (Gestisce log periodico del SOC e conteggio Hub)
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				//In questo modo i listener ricevono il EvFleetManager già popolato.
				bind(EvFleetManager.class).toInstance(evFleetManager);
				addMobsimListenerBinding().to(EvSocMonitor.class);
				addEventHandlerBinding().toInstance(hubManager);
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