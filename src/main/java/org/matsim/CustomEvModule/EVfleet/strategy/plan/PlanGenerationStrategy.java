package org.matsim.CustomEvModule.EVfleet.strategy.plan;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.vehicles.Vehicle;

public interface PlanGenerationStrategy {
    void generatePlanForVehicle(Id<Vehicle> vehicleId, Scenario scenario);
}
