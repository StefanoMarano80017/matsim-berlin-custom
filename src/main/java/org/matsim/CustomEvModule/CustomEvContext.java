package org.matsim.CustomEvModule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecificationDefaultImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.CustomEvModule.Hub.HubManager;
import org.matsim.CustomEvModule.EVfleet.EvFleetManager;
import org.matsim.CustomEvModule.EVfleet.factory.EvVehicleFactory;
import org.matsim.CustomEvModule.EVfleet.strategy.fleet.CsvFleetGenerationStrategy;
import org.matsim.CustomEvModule.EVfleet.strategy.fleet.EvFleetStrategy;
import org.matsim.CustomEvModule.EVfleet.strategy.plan.PlanGenerationStrategy;
import org.matsim.CustomEvModule.EVfleet.strategy.plan.StaticPlanGenerator;
import org.matsim.ServerEvSetup.ConfigRun.ConfigRun;
import org.matsim.ServerEvSetup.ConfigRun.ConfigRun.PlanGenerationStrategyEnum;
import org.matsim.ServerEvSetup.ConfigRun.ConfigRun.VehicleGenerationStrategyEnum;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class CustomEvContext {

    private static final Logger log = LogManager.getLogger(CustomEvContext.class);

    private final HubManager hubManager;
    private final EvFleetManager evFleetManager;
    private final ChargingInfrastructureSpecification infraSpec;

    public CustomEvContext(Scenario scenario, ConfigRun config) {
        this.infraSpec      = new ChargingInfrastructureSpecificationDefaultImpl();
        this.hubManager     = initializeHubManager(scenario, config, infraSpec);
        this.evFleetManager = initializeEvFleetManager(scenario, config);

        registerDefaultVehicles(
            scenario.getVehicles().getFactory(),
            scenario.getVehicles(),
            scenario.getPopulation().getPersons()
        );

        log.info("{CustomEvContext} Scenario dati (Hub, EV Fleet, Veicoli default) preparati.");
    }
    /*
    *  Inizializzazione Hub Manager 
    */
    private HubManager initializeHubManager(Scenario scenario, ConfigRun config, ChargingInfrastructureSpecification infraSpec) {
        HubManager Manager = new HubManager(scenario.getNetwork(), infraSpec);
        Manager.createHub(config.getCsvResourceHub());
        return Manager;
    }
    /*
    *   Inizializzazione EvFleetManager
    */
    private EvFleetManager initializeEvFleetManager(Scenario scenario, ConfigRun config) {

        EvFleetManager evFleetManager = new EvFleetManager();

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

    /*
    *  Default Vehicles assignment -> quando creo un agente devo dargli anche il walk e il pt di default
    */
    @SuppressWarnings("deprecation")
    private void registerDefaultVehicles(
        VehiclesFactory vehicleFactory,
        Vehicles vehicles, 
        Map<Id<Person>, ? extends Person> persons
    ) {
        Id<Vehicle> walkVehicleId = createDefaultVehicleType(
            vehicleFactory, 
            vehicles, 
            TransportMode.walk,
            Id.create("default_walk_type", VehicleType.class),
            Id.create("default_walk_vehicle", Vehicle.class)
        );

        Id<Vehicle> ptVehicleId = createDefaultVehicleType(
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

    private Id<Vehicle> createDefaultVehicleType(
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

    /*
    *  Getters 
    */
    public HubManager getHubManager() {
        return hubManager;
    }

    public EvFleetManager getEvFleetManager() {
        return evFleetManager;
    }

    public ChargingInfrastructureSpecification getInfraSpec(){
        return infraSpec;
    }
}
