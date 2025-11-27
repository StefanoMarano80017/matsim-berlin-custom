package org.matsim.CustomMonitor.EVfleet;

import org.matsim.CustomMonitor.model.EvModel;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vehicles.VehicleType;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvFleetManager {
    private final Map<Id<Vehicle>, EvModel> fleet = new HashMap<>();

    /**
     * Genera flotta casuale delegando a EvGenerator e la salva internamente.
     */
    public void generateFleetFromCsv(Path csv, Scenario scenario, int count, double socMean, double socStdDev) {
        System.out.println("[EvFleetManager] Avvio generazione flotta da CSV...");
        System.out.println("[EvFleetManager] Parametri: count=" + count + ", socMean=" + socMean + ", socStdDev=" + socStdDev);
        try {
            System.out.println("[EvFleetManager] Caricamento CSV degli EV...");
            EvGenerator.loadCsv(csv);
            EvGenerator.setSeed(42);
            System.out.println("[EvFleetManager] Generazione modelli EV casuali...");
            List<EvModel> evModels = EvGenerator.generateEvModels(count, socMean, socStdDev);
            for (EvModel ev : evModels) {
                System.out.println("  - Aggiunto veicolo: " + ev.getVehicleId());
                fleet.put(ev.getVehicleId(), ev);
            }
            System.out.println("[EvFleetManager] Flotta generata. Totale veicoli: " + fleet.size());
        } catch (IOException e) {
            System.out.println("[EvFleetManager] ERRORE caricando CSV: " + e.getMessage());
            e.printStackTrace();
        }
        populateScenarioVehicles(scenario);
    }

    /** Inserisce i veicoli nello scenario MATSim */
    private void populateScenarioVehicles(Scenario scenario) {
        System.out.println("[EvFleetManager] Inserimento veicoli nello Scenario...");
        VehiclesFactory vehicleFactory = scenario.getVehicles().getFactory();
        VehicleType vehicleType = vehicleFactory.createVehicleType(Id.create("EV_Type_From_CSV", VehicleType.class));
        vehicleType.setDescription("EV auto-generated from CSV");
        vehicleType.setMaximumVelocity(160.0 / 3.6);
        scenario.getVehicles().addVehicleType(vehicleType);
        for (EvModel ev : fleet.values()) {
            System.out.println("  - Registrazione veicolo nello scenario: " + ev.getVehicleId());
            Vehicle vehicle = vehicleFactory.createVehicle(ev.getVehicleId(), vehicleType);
            scenario.getVehicles().addVehicle(vehicle);
        }
        System.out.println("[EvFleetManager] Inserimento veicoli completato.");
    }

    public Map<Id<Vehicle>, EvModel> getFleet() {
        return Collections.unmodifiableMap(fleet);
    }

    public EvModel getVehicle(Id<Vehicle> vehicleId) {
        System.out.println("[EvFleetManager] Richiesta modello veicolo: " + vehicleId);
        return fleet.get(vehicleId);
    }

    /** Aggiorna SOC e stato dinamico dei veicoli dalla simulazione (QSim) */
    public void updateVehiclesFromQSim(org.matsim.core.mobsim.qsim.QSim qSim) {
        System.out.println("[EvFleetManager] Aggiornamento stato EV da QSim...");
        ElectricFleet electricFleet = qSim.getChildInjector().getInstance(ElectricFleet.class);
        for (EvModel evModel : fleet.values()) {
            Id<Vehicle> vehId = evModel.getVehicleId();
            ElectricVehicle ev = electricFleet.getElectricVehicles().get(vehId);
            if (ev != null && ev.getBattery() != null) {
                double soc = ev.getBattery().getSoc();
                double energyJ = ev.getBattery().getCharge();
                boolean isCharging = ev.getChargingPower() != null;
                System.out.println("  * [" + vehId + "] SOC=" + soc + ", energia(J)=" + energyJ + ", charging=" + isCharging);
                evModel.updateDynamicState(soc, energyJ, isCharging);
            } else {
                System.out.println("  ! Nessun EV trovato in ElectricFleet per: " + vehId);
            }
        }
    }

    /** Calcola SOC medio della flotta */
    public double calculateAverageSoc() {
        double avg = fleet.values()
                        .stream()
                        .mapToDouble(EvModel::getCurrentSoc)
                        .average()
                        .orElse(0.0);
        System.out.println("[EvFleetManager] SOC medio flotta: " + avg);
        return avg;
    }

    /** Calcola distanza media dei veicoli in ricarica/non ricarica */
    public double calculateAverageDistanceByChargingStatus(boolean isCharging) {
        double avg = fleet.values()
                        .stream()
                        .filter(ev -> ev.isCharging() == isCharging)
                        .mapToDouble(EvModel::getDistanceTraveledKm)
                        .average()
                        .orElse(0.0);
        System.out.println("[EvFleetManager] Distanza media (isCharging=" + isCharging + "): " + avg);
        return avg;
    }
}
