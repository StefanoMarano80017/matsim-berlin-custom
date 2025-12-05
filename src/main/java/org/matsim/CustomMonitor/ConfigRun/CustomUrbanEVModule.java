package org.matsim.CustomMonitor.ConfigRun;

import org.matsim.CustomMonitor.EVfleet.EvFleetManager;
import org.matsim.CustomMonitor.EVfleet.EnergyConsumption.EvConsumptionModelFactory;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;

import playground.vsp.ev.UrbanEVModule;

public class CustomUrbanEVModule extends UrbanEVModule {
    private final ChargingInfrastructureSpecification infraSpec;
    private final EvFleetManager evFleetManager;

    public CustomUrbanEVModule(ChargingInfrastructureSpecification infraSpec, EvFleetManager evFleetManager) {
        this.infraSpec = infraSpec;
        this.evFleetManager = evFleetManager;
    }

    @Override
    public void install() {
        // Bind MATSim style
        bind(EvConsumptionModelFactory.class).toInstance(new EvConsumptionModelFactory(evFleetManager));
        super.install();
    }
}