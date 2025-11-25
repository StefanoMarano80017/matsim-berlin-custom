package org.matsim.CustomMonitor;

import org.matsim.CustomMonitor.model.EvModel;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vehicles.VehicleType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvFleetManager {

    private final Map<Id<Vehicle>, EvModel> fleet = new HashMap<>();

    /**
     * Genera flotta casuale delegando a EvGenerator e la salva internamente.
     */
    public void generateFleetFromCsv(Scenario scenario, int count, double socMean, double socStdDev) {
        List<EvModel> evModels = EvGenerator.generateEvModels(count, socMean, socStdDev);
        for (EvModel ev : evModels) {
            fleet.put(ev.getVehicleId(), ev);
        }
        // Inserisce i veicoli nello scenario
        populateScenarioVehicles(scenario);
    }

    /** Inserisce i veicoli nello scenario MATSim */
    private void populateScenarioVehicles(Scenario scenario) {
        VehiclesFactory vehicleFactory = scenario.getVehicles().getFactory();
        VehicleType vehicleType = vehicleFactory.createVehicleType(Id.create("EV_Type_From_CSV", VehicleType.class));
        vehicleType.setDescription("EV auto-generated from CSV");
        vehicleType.setMaximumVelocity(160.0 / 3.6);
        scenario.getVehicles().addVehicleType(vehicleType);

        for (EvModel ev : fleet.values()) {
            Vehicle vehicle = vehicleFactory.createVehicle(ev.getVehicleId(), vehicleType);
            scenario.getVehicles().addVehicle(vehicle);
        }
    }

    public Map<Id<Vehicle>, EvModel> getFleet() {
        return Collections.unmodifiableMap(fleet);
    }

    public EvModel getVehicle(Id<Vehicle> vehicleId) {
        return fleet.get(vehicleId);
    }

    /** Aggiorna SOC e stato dinamico dei veicoli dalla simulazione (QSim) */
    public void updateVehiclesFromQSim(org.matsim.core.mobsim.qsim.QSim qSim) {
        org.matsim.contrib.ev.fleet.ElectricFleet electricFleet =
                qSim.getChildInjector().getInstance(org.matsim.contrib.ev.fleet.ElectricFleet.class);

        for (EvModel evModel : fleet.values()) {
            Id<Vehicle> vehId = evModel.getVehicleId();
            org.matsim.contrib.ev.fleet.ElectricVehicle ev = electricFleet.getElectricVehicles().get(vehId);
            if (ev != null && ev.getBattery() != null) {
                double soc = ev.getBattery().getSoc();
                double energyJ = ev.getBattery().getCharge();
                boolean isCharging = ev.getChargingPower() != null;
                evModel.updateDynamicState(soc, energyJ, isCharging);
            }
        }
    }

    /** Calcola SOC medio della flotta */
    public double calculateAverageSoc() {
        return fleet.values().stream()
                .mapToDouble(EvModel::getCurrentSoc)
                .average()
                .orElse(0.0);
    }

    /** Calcola distanza media dei veicoli in ricarica/non ricarica */
    public double calculateAverageDistanceByChargingStatus(boolean isCharging) {
        return fleet.values().stream()
                .filter(ev -> ev.isCharging() == isCharging)
                .mapToDouble(EvModel::getDistanceTraveledKm)
                .average()
                .orElse(0.0);
    }
}
