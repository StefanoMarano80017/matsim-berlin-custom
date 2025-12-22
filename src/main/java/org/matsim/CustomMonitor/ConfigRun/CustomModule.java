package org.matsim.CustomMonitor.ConfigRun;

import org.matsim.CustomMonitor.ChargingHub.HubManager;
import org.matsim.CustomMonitor.ChargingHub.TargetSocChargingHandler;
import org.matsim.CustomMonitor.ConfigRun.ConfigRun.PlanGenerationStrategyEnum;
import org.matsim.CustomMonitor.ConfigRun.ConfigRun.VehicleGenerationStrategyEnum;
import org.matsim.CustomMonitor.EVfleet.EvFleetManager;
import org.matsim.CustomMonitor.EVfleet.EnergyConsumption.EvConsumptionModelFactory;
import org.matsim.CustomMonitor.EVfleet.factory.EvVehicleFactory;
import org.matsim.CustomMonitor.EVfleet.strategy.fleet.CsvFleetGenerationStrategy;
import org.matsim.CustomMonitor.EVfleet.strategy.fleet.EvFleetStrategy;
import org.matsim.CustomMonitor.EVfleet.strategy.plan.PlanGenerationStrategy;
import org.matsim.CustomMonitor.EVfleet.strategy.plan.StaticPlanGenerator;
import org.matsim.CustomMonitor.Monitoring.HubChargingMonitor;
import org.matsim.CustomMonitor.Monitoring.QuickLinkDebugHandler;
import org.matsim.CustomMonitor.Monitoring.TimeStepSocMonitor;
import org.matsim.CustomMonitor.Monitoring.VehicleStatusMonitor;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecificationDefaultImpl;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.prepare.choices.RandomPlanGenerator;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
import org.springframework.core.io.Resource;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CustomModule extends AbstractModule {

    private static final Logger log = LogManager.getLogger(CustomModule.class);

    private final HubManager     hubManager;
    private final EvFleetManager evFleetManager;
    private final ChargingInfrastructureSpecification infraSpec;

    private final ConfigRun config;
    private final boolean debug;

    public CustomModule(Scenario scenario, ConfigRun config) {
        this.config = config;
        this.infraSpec      = new ChargingInfrastructureSpecificationDefaultImpl();
        this.hubManager     = new HubManager(scenario.getNetwork(), infraSpec);
        this.evFleetManager = new EvFleetManager();
        this.debug             = config.isDebug();
    }

    public void PrepareScenarioEV(Scenario scenario) {
        // --- Preparazione Scenario ---
        initializeEvFleetManager(scenario);
        loadChargingHubs();
        registerDefaultVehicles(
            scenario.getVehicles().getFactory(),
            scenario.getVehicles(),
            scenario.getPopulation().getPersons()
        );

        log.info("Modulo Custom: Scenario dati (Hub, EV Fleet, Veicoli default) preparati.");
    }

    private void loadChargingHubs() {
        this.hubManager.createHub(config.getCsvResourceHub());
    }

    private EvFleetManager initializeEvFleetManager(Scenario scenario) {

        evFleetManager.setFleetStrategy(
            createFleetStrategy(config.getVehicleStrategy())
        );

        evFleetManager.setPlanStrategy(
            createPlanStrategy(config.getPlanStrategy())
        );

        evFleetManager.setVehicleFactory(
            new EvVehicleFactory()
        );

        evFleetManager.generateFleet(
            scenario,
            config
        );

        return evFleetManager;
    }

    private EvFleetStrategy createFleetStrategy(VehicleGenerationStrategyEnum strategy) {
        return switch (strategy) {
            case FROM_CSV -> new CsvFleetGenerationStrategy();
            case UNIFORM  -> throw new RuntimeException();
            case NORMAL   -> throw new RuntimeException();
            default       -> throw new RuntimeException();
        };
    }

    private PlanGenerationStrategy createPlanStrategy(PlanGenerationStrategyEnum strategy) {
        return switch (strategy) {
            case STATIC       -> new StaticPlanGenerator();
            case RANDOM       -> throw new RuntimeException();
            case FROM_CSV     -> throw new RuntimeException();
            default           -> throw new RuntimeException();
        };
    }

    @SuppressWarnings("deprecation")
    private void registerDefaultVehicles(
        VehiclesFactory vehicleFactory,
        Vehicles vehicles, 
        Map<Id<Person>, ? extends Person> persons
    ) {
        Id<Vehicle> walkVehicleId = createVehicleType(
            vehicleFactory, 
            vehicles, 
            TransportMode.walk,
            Id.create("default_walk_type", VehicleType.class),
            Id.create("default_walk_vehicle", Vehicle.class)
        );

        Id<Vehicle> ptVehicleId = createVehicleType(
            vehicleFactory, 
            vehicles, 
            TransportMode.pt,
            Id.create("default_pt_type", VehicleType.class),
            Id.create("default_pt_vehicle", Vehicle.class)
        );

        persons.values().forEach(person -> {
            Map<String, Id<Vehicle>> mode2Vehicle;
            try {
                mode2Vehicle = VehicleUtils.getVehicleIds(person);
            } catch (RuntimeException e) {
                mode2Vehicle = new HashMap<>();
            }
            mode2Vehicle.put(TransportMode.walk, walkVehicleId);
            mode2Vehicle.put(TransportMode.pt, ptVehicleId);
            VehicleUtils.insertVehicleIdsIntoAttributes(person, mode2Vehicle);
        });
    }

    private Id<Vehicle> createVehicleType(
        VehiclesFactory vehicleFactory, 
        Vehicles vehicles, 
        String networkMode,
        Id<VehicleType> typeId, 
        Id<Vehicle> vehicleId
    ) {
        VehicleType vehicleType = vehicleFactory.createVehicleType(typeId);
        vehicleType.setNetworkMode(networkMode);
        vehicles.addVehicleType(vehicleType);

        Vehicle defaultVehicle = vehicleFactory.createVehicle(vehicleId, vehicleType);
        vehicles.addVehicle(defaultVehicle);

        return defaultVehicle.getId();
    }

    @Override
    public void install() {
        bind(ChargingInfrastructureSpecification.class).toInstance(infraSpec);
        /*
        *  Binding dei miei manager per gli altri moduli
        */
        bind(EvFleetManager.class).toInstance(evFleetManager);
        bind(HubManager.class).toInstance(hubManager);
        /*
        *   monitor inizio e fine ricarica a un hub
        */
        HubChargingMonitor hubChargingMonitor = new HubChargingMonitor(hubManager, evFleetManager);
        addEventHandlerBinding().toInstance(hubChargingMonitor);
        /*
        *   Andamento Soc nel tempo, settare il timestep per aggiornamento in discreto 
        */
        TimeStepSocMonitor timeStepSocMonitor = new TimeStepSocMonitor(evFleetManager, hubManager, config.getStepSize());        
        addMobsimListenerBinding().toInstance(timeStepSocMonitor);
        /*
        *  monitor dello stato del veicolo
        */
        VehicleStatusMonitor vehicleStatusMonitor = new VehicleStatusMonitor(evFleetManager);
        addEventHandlerBinding().toInstance(vehicleStatusMonitor);
        /*
        *  Modello di consumo del SoC
        */
        bind(DriveEnergyConsumption.Factory.class).toProvider(new Provider<>() {
            @Inject private EvFleetManager providerEvFleetManager;
            @Override
            public DriveEnergyConsumption.Factory get() {
                return new EvConsumptionModelFactory(providerEvFleetManager);
            }
        }).asEagerSingleton();
        /*
        *  Handler della ricarica nel piano + strategia soc target
        */
        installQSimModule(new AbstractQSimModule() {
			@Override protected void configureQSim() {
				bind(TargetSocChargingHandler.class).in(Singleton.class);
				addMobsimScopeEventHandlerBinding().to( TargetSocChargingHandler.class);
			}
		});

        if(debug == true) {
            Set<Id<Vehicle>> electricVehicleIds = Collections.unmodifiableSet(evFleetManager.getFleet().keySet());
            QuickLinkDebugHandler debugHandler = new QuickLinkDebugHandler(electricVehicleIds);
            addEventHandlerBinding().toInstance(debugHandler);
        }

        log.info("Modulo Custom installato con successo");
    }

    public EvFleetManager getEvFleetManager() {
        return evFleetManager;
    }

    public HubManager getHubManager() {
        return hubManager;
    }

}
