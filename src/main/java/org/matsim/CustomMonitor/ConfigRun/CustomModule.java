package org.matsim.CustomMonitor.ConfigRun;

import org.matsim.CustomMonitor.ChargingHub.HubManager;
import org.matsim.CustomMonitor.EVfleet.EvFleetManager;
import org.matsim.CustomMonitor.EVfleet.EnergyConsumption.EvConsumptionModelFactory;
import org.matsim.CustomMonitor.EVfleet.events.EvChargingEventHandler;
import org.matsim.CustomMonitor.Monitoring.QuickLinkDebugHandler;
import org.matsim.CustomMonitor.Monitoring.TimeStepMonitor;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecificationDefaultImpl;
import org.matsim.core.controler.AbstractModule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Names;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CustomModule extends AbstractModule {

    private static final Logger log = LogManager.getLogger(EvFleetManager.class);

    private final HubManager hubManager;
    private final EvFleetManager evFleetManager;
    private final ChargingInfrastructureSpecification infraSpec;

    private final Path chargingHubPath;
    private final Path evDatasetPath;
    private final int sampleSize;
    private final double socMean;
    private final double socStdDev;
    private final boolean debug;

    public CustomModule(
        Scenario scenario, 
        Path chargingHubPath,
        Path evDatasetPath,
        int sampleSize, 
        double socMean, 
        double socStdDev, 
        boolean debug
    ) {
        this.chargingHubPath = chargingHubPath;
        this.evDatasetPath   = evDatasetPath;
        this.sampleSize      = sampleSize;
        this.socMean         = socMean;
        this.socStdDev       = socStdDev;
        this.infraSpec       = new ChargingInfrastructureSpecificationDefaultImpl();
        this.hubManager      = new HubManager(scenario.getNetwork(), infraSpec);
        this.evFleetManager  = new EvFleetManager();
        this.debug           = debug;
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
        this.hubManager.createHub(chargingHubPath);
    }

    private EvFleetManager initializeEvFleetManager(Scenario scenario) {
        evFleetManager.setFleetStrategy(new org.matsim.CustomMonitor.EVfleet.strategy.fleet.CsvFleetGenerationStrategy());
        evFleetManager.setPlanStrategy(new org.matsim.CustomMonitor.EVfleet.strategy.plan.StaticPlanGenerator());
        evFleetManager.setVehicleFactory(new org.matsim.CustomMonitor.EVfleet.factory.EvVehicleFactory());
        evFleetManager.generateFleetFromCsv(evDatasetPath, scenario, sampleSize, socMean, socStdDev);
        return evFleetManager;
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
        bind(EvFleetManager.class).toInstance(evFleetManager);
        bind(HubManager.class).toInstance(hubManager);
        
        bind(TimeStepMonitor.class).asEagerSingleton();
        bind(Double.class).annotatedWith(Names.named("timeStepMonitorStep")).toInstance(900.0);
        addMobsimListenerBinding().to(TimeStepMonitor.class);

        addEventHandlerBinding().toInstance(hubManager);
        addEventHandlerBinding().toInstance(new EvChargingEventHandler(evFleetManager.getFleet()));

        bind(DriveEnergyConsumption.Factory.class).toProvider(new Provider<>() {
            @Inject private EvFleetManager providerEvFleetManager;
            @Override
            public DriveEnergyConsumption.Factory get() {
                return new EvConsumptionModelFactory(providerEvFleetManager);
            }
        }).asEagerSingleton();

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
