package org.matsim.CustomMonitor;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecificationDefaultImpl; 

/**
 * Classe responsabile della definizione e generazione dell'infrastruttura
 * di ricarica (Charging Hubs) nello scenario. 
 * Crea l'oggetto ChargingInfrastructureSpecification, assumendo che lo scenario
 * non lo contenga.
 */
public class ChargingHubGenerator {

    // Costanti per il Link dove verrà posizionata la stazione di ricarica
    public static final String LINK_HUB = "2000"; 
    public static final Id<Charger> CHARGER_ID = Id.create("Hub_Berlin_Fast", Charger.class);

    public static void generateChargingHubs(Scenario scenario) {
        
        // Creiamo e istanziamo l'oggetto per la specifica dell'infrastruttura di ricarica
        // utilizzando ChargingInfrastructureSpecificationDefaultImpl
        ChargingInfrastructureSpecification chargingSpec = new ChargingInfrastructureSpecificationDefaultImpl(); 

        Id<Link> hubLinkId = Id.createLinkId(LINK_HUB);
        
        // Creazione del Charger e aggiunta alla specifica.
        // Crea un charger da 150kW con 4 prese (plugs) e lo aggiunge alla specifica
        chargingSpec.addChargerSpecification(ImmutableChargerSpecification.newBuilder()
                .id(CHARGER_ID)
                .linkId(hubLinkId)
                .plugPower(150000) // 150 kW (Fast Charger)
                .plugCount(4)
                .build());
        
        System.out.println("Stazione di ricarica '" + CHARGER_ID + "' creata sul Link " + LINK_HUB);
    }
}