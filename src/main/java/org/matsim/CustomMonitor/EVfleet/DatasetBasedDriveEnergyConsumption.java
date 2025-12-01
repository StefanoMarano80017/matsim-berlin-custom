package org.matsim.CustomMonitor.EVfleet;

import org.matsim.CustomMonitor.model.EvModel;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
/**
 * Modello di consumo che si basa direttamente sul valore medio (kWh/km) 
 * fornito dal dataset del veicolo (EvModel), convertendolo in un consumo istantaneo 
 * proporzionale alla velocita' del link.
 * * Questo approccio semplifica la modellazione, ignorando la fisica complessa 
 * (accelerazione, pendenza) ma utilizzando il dato di consumo medio reale.
 */
public class DatasetBasedDriveEnergyConsumption implements DriveEnergyConsumption {

    // Fattore di conversione: da kWh/km a Joule/metro (J/m)
    // 1 kWh = 3.6e6 Joule
    // 1 km = 1000 metri
    // 3.6e6 / 1000 = 3600.0
    private static final double KWH_PER_KM_TO_JOULE_PER_METER_FACTOR = 3600.0;

    private final ElectricVehicle electricVehicle;
    private final EvFleetManager fleetManager;

    public DatasetBasedDriveEnergyConsumption(ElectricVehicle electricVehicle, EvFleetManager fleetManager) {
        this.electricVehicle = electricVehicle;
        this.fleetManager = fleetManager;
    }

    /**
     * Calcola il consumo di energia per percorrere il link.
     * @return Consumo totale in Joule (J) per il tempo di viaggio sul link.
     */
    @Override
    public double calcEnergyConsumption(Link link, double travelTime, double linkEnterTime) {
        // 1. Ottieni i dati specifici del veicolo dal EvFleetManager
        EvModel evData = fleetManager.getVehicle(electricVehicle.getId());

        if (evData == null) {
            // Questo veicolo EV non fa parte della nostra flotta monitorata.
            // Restituisce 0, affidandosi al modello di consumo di fallback di MATSim 
            // se ce n'è uno, o ignorando il consumo.
            System.err.println("WARN: Veicolo " + electricVehicle.getId() + " non trovato in EvFleetManager. Consumo impostato a 0.");
            return 0.0;
        }

        double consumptionKwhPerKm = evData.getConsumptionKwhPerKm();
        
        System.out.println("DEBUG: Veicolo " + electricVehicle.getId() + 
                           " consumo medio: " + consumptionKwhPerKm + " kWh/km su link " + link.getId());

        // Calcola il consumo in Joule per metro (J/m)
        double consumptionJoulesPerMeter = consumptionKwhPerKm * KWH_PER_KM_TO_JOULE_PER_METER_FACTOR;

        // Calcola la distanza percorsa sul link (in metri)
        double distanceMeters = link.getLength();

        // Consumo totale in Joule per percorrere il link
        double totalConsumptionJoules = consumptionJoulesPerMeter * distanceMeters;
        
        // Aggiorna la distanza percorsa nel nostro EvModel per scopi di monitoraggio
        evData.addDistanceTraveled(distanceMeters);

        throw new RuntimeException("DEBUG: Consumo calcolato per veicolo " + electricVehicle.getId() + 
                                   " su link " + link.getId() + " e': " + totalConsumptionJoules + " J");

        // MATSim si aspetta che il consumo sia espresso in un valore POSITIVO
        //return totalConsumptionJoules;
    }
}