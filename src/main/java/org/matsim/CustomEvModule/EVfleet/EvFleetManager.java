package org.matsim.CustomEvModule.EVfleet;

import org.matsim.ServerEvSetup.ConfigRun.ConfigRun;
import org.matsim.CustomEvModule.EVfleet.factory.EvVehicleFactory;
import org.matsim.CustomEvModule.EVfleet.strategy.fleet.EvFleetStrategy;
import org.matsim.CustomEvModule.EVfleet.strategy.plan.PlanGenerationStrategy;
import org.matsim.CustomEvModule.model.EvModel;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EvFleetManager{

    private static final Logger log = LogManager.getLogger(EvFleetManager.class);

    private final Map<Id<Vehicle>, EvModel> fleet = new HashMap<>();
    /*
    *   Strategie per la generazione del fleet e dei piani
    */
    private EvFleetStrategy fleetStrategy;
    private PlanGenerationStrategy planStrategy;
    private EvVehicleFactory vehicleFactory;

    @Inject
    public EvFleetManager() {}

    // ----------------------------------------------------
    // SETUP Strategies
    // ----------------------------------------------------
    public void setFleetStrategy(EvFleetStrategy strategy) {
        this.fleetStrategy = strategy;
    }

    public void setPlanStrategy(PlanGenerationStrategy strategy) {
        this.planStrategy = strategy;
    }

    public void setVehicleFactory(EvVehicleFactory factory) {
        this.vehicleFactory = factory;
    }

    // ----------------------------------------------------
    // PUBLIC API
    // ----------------------------------------------------
    public void generateFleet(Scenario scenario, ConfigRun config){
        if (fleetStrategy == null)
            throw new IllegalStateException("FleetGenerationStrategy not set");

        if (planStrategy == null)
            throw new IllegalStateException("PlanGenerationStrategy not set");

        if (vehicleFactory == null)
            throw new IllegalStateException("EvVehicleFactory not set");

        log.info("[EvFleetManager] Generating EV fleet");
        // 1. Usa la strategia per generare gli EvModel
        List<EvModel> EVmodels = fleetStrategy.generateFleet(
            config.getCsvResourceEv(), 
            config.getNumeroVeicoli(),
            config.getSocMedio(), 
            config.getSocStdDev()
        );
        // 2. Registra modelli nel manager
        EVmodels.forEach(model -> 
            fleet.put(
                Id.create(model.getVehicleId().toString() + "_car", Vehicle.class), 
                model
            )
        );
        // 3. Genera veicoli MATSim nello Scenario
        EVmodels.forEach(model ->
            vehicleFactory.createMatsimVehicle(
                model,
                scenario.getVehicles(), 
                Set.of("default", "a", "b") //tipi di caricatore 
            )
        );
        // 4. Genera piani tramite strategia
        EVmodels.forEach(
            model -> planStrategy.generatePlanForVehicle(model.getVehicleId(), scenario)
        );
    }

    // ----------------------------------------------------
    // SIMULATION UPDATE 
    // ----------------------------------------------------
    public void updateSoc(ElectricFleet electricFleet) {
        if (electricFleet == null) throw new IllegalArgumentException("ElectricFleet is null");
        for (EvModel evModel : fleet.values()) {
            Id<Vehicle> qsimId = Id.create(evModel.getVehicleId().toString() + "_car", Vehicle.class);
            var ev = electricFleet.getElectricVehicles().get(qsimId);
            if (ev != null) {
                evModel.updateDynamicState(
                    ev.getBattery().getSoc(),
                    ev.getBattery().getCharge()
                );
            }
        }
    }

    // ----------------------------------------------------
    // GETTERS
    // ----------------------------------------------------
    public Map<Id<Vehicle>, EvModel> getFleet() {
        return Collections.unmodifiableMap(fleet);
    }

    public EvModel getVehicle(Id<Vehicle> vehicleId) {
        return fleet.get(vehicleId);
    }

}
