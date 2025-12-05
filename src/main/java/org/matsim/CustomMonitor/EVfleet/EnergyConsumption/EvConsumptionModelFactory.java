package org.matsim.CustomMonitor.EVfleet.EnergyConsumption;

import org.matsim.CustomMonitor.EVfleet.EvFleetManager;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import javax.inject.Inject;

/**
 * Factory che crea il modello di consumo DatasetBasedDriveEnergyConsumption.
 * Questa factory riceve EvFleetManager tramite il suo costruttore,
 * che viene iniettato al momento del binding nel Controler (tramite toInstance).
 */
public class EvConsumptionModelFactory implements DriveEnergyConsumption.Factory {

    private final EvFleetManager evFleetManager;

    /**
     * Costruttore iniettato da Guice. 
     * Il MATSim Controler inietterà EvFleetManager che è stato precedentemente bindato.
     * @param evFleetManager Il manager della flotta EV.
     */
    @Inject // Manteniamo @Inject per permettere a Guice di iniettare l'oggetto nella Factory
    public EvConsumptionModelFactory(EvFleetManager evFleetManager) {
        this.evFleetManager = evFleetManager;
    }

    @Override
    public DriveEnergyConsumption create(ElectricVehicle electricVehicle) {
        System.out.println("FACTORY CHIAMATA: Creazione modello di consumo per veicolo " + electricVehicle.getId());

        // EvFleetManager contiene il veicolo con questo ID?
        if (this.evFleetManager.getFleet().containsKey(electricVehicle.getId())) {
            System.out.println("Veicolo Trovato nel custom Manager.");
        } else {
            System.err.println("!!! ATTENZIONE: Veicolo non trovato in EvFleetManager !!!");
        }

        // Restituisce l'istanza del tuo modello di consumo personalizzato.
        return new DatasetBasedDriveEnergyConsumption(electricVehicle, this.evFleetManager);
    }
}