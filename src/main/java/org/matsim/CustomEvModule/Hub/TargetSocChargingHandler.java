package org.matsim.CustomEvModule.Hub;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.charging.ChargeUpToMaxSocStrategy;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

public class TargetSocChargingHandler
        implements ActivityStartEventHandler, ActivityEndEventHandler,
                   PersonLeavesVehicleEventHandler, ChargingEndEventHandler, MobsimScopeEventHandler {


    private static final Logger log = LogManager.getLogger(TargetSocChargingHandler.class);

    ElectricFleet electricFleet;
    ChargingInfrastructure chargingInfrastructure;
    ChargingStrategy.Factory strategyFactory;

    private final Map<Id<Person>, Id<Vehicle>> lastVehicleUsed = new HashMap<>();
    private final Map<Id<Vehicle>, Id<Charger>> vehiclesAtChargers = new HashMap<>();
    private final HubManager hubManager;

    @Inject
	TargetSocChargingHandler(
        ChargingInfrastructure chargingInfrastructure, 
        ElectricFleet electricFleet, 
        ChargingStrategy.Factory strategyFactory,
        HubManager hubManager
    ) {
		this.chargingInfrastructure = chargingInfrastructure;
		this.electricFleet   = electricFleet;
		this.strategyFactory = strategyFactory;
		this.hubManager      = hubManager;
	}

    @Override
    public void handleEvent(PersonLeavesVehicleEvent e) {
        lastVehicleUsed.put(e.getPersonId(), e.getVehicleId());
    }

    @Override
    public void handleEvent(ActivityStartEvent e) {
        if (!e.getActType().endsWith("car charging")) return;

        Id<Link> eventLink = e.getLinkId();
        if(eventLink == null) return;
        
        Id<Vehicle> vId     = lastVehicleUsed.get(e.getPersonId());
        ElectricVehicle ev  = electricFleet.getElectricVehicles().get(vId);
        
        Optional<Id<Charger>> optionalCharger = hubManager.getAvailableChargerForLink(
                eventLink, ev.getChargerTypes()
        );

        if (optionalCharger.isEmpty()) {
            log.info("[TargetSocChargingHandler] Nessun charger disponibile compatibile per veicolo " + vId);
            return;
        }

        Id<Charger> chargerId   = optionalCharger.get();
        Charger charger         = chargingInfrastructure.getChargers().get(chargerId);
        double socTarget        = readSocTarget(e.getPersonId());

        ChargingStrategy strategy = new ChargeUpToMaxSocStrategy(charger.getSpecification(), ev, socTarget);
        charger.getLogic().addVehicle(ev, strategy, e.getTime());
        vehiclesAtChargers.put(ev.getId(), charger.getId());

        log.info("[TargetSocChargingHandler] Veicolo {} assegnato a charger {} (hub {})", vId, chargerId, hubManager.getHubIdForCharger(chargerId));
    }

    @Override
    public void handleEvent(ActivityEndEvent e) {
        if (!e.getActType().endsWith("car charging")) return;

        Id<Vehicle> vId = lastVehicleUsed.get(e.getPersonId());
        Id<Charger> cId = vehiclesAtChargers.remove(vId);

        if (cId != null) {
            Charger c = chargingInfrastructure.getChargers().get(cId);
            c.getLogic().removeVehicle(electricFleet.getElectricVehicles().get(vId), e.getTime());
        }
    }

    @Override
    public void handleEvent(ChargingEndEvent e) {
        vehiclesAtChargers.remove(e.getVehicleId());
    }

    private double readSocTarget(Id<Person> personId) {
        // esempio: attributo della persona
        return 0.99; // 80%
    }


}
