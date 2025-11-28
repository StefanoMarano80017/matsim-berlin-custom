package org.matsim.CustomMonitor.EVfleet;

import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;

import javax.inject.Inject;

/**
 * Factory personalizzata per creare istanze di DatasetBasedDriveEnergyConsumption.
 * Questa factory implementa l'interfaccia richiesta da MATSim per fornire il modello
 * di consumo durante la simulazione.
 * * Grazie a Guice, questa factory può iniettare l'EvFleetManager (bindato in OpenBerlinScenario)
 * e lo passa al modello di consumo.
 */
public class EvConsumptionModelFactory implements DriveEnergyConsumption.Factory {

    private final EvFleetManager evFleetManager;

    public EvConsumptionModelFactory() {
        this.evFleetManager = null; // Placeholder, Guice dovrebbe iniettare l'istanza corretta.
    }

    /**
     * Guice inietta l'EvFleetManager che è stato bindato come istanza in OpenBerlinScenario.
     */
    @Inject
    public EvConsumptionModelFactory(EvFleetManager evFleetManager) {
        this.evFleetManager = evFleetManager;
    }

    /**
     * Crea un nuovo modello di consumo (DatasetBasedDriveEnergyConsumption) per ogni veicolo EV.
     * La factory utilizza il veicolo EV corrente e il manager per inizializzare il modello.
     *
     * @param electricVehicle Il veicolo EV per cui creare il modello.
     * @return Una nuova istanza di DatasetBasedDriveEnergyConsumption.
     */
    @Override
    public DriveEnergyConsumption create(ElectricVehicle electricVehicle) {
        // La EvFleetManager passata al costruttore viene usata per creare il modello specifico.
        return new DatasetBasedDriveEnergyConsumption(electricVehicle, this.evFleetManager);
    }
}