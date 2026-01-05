package org.matsim.CustomEvModule.Hub;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.matsim.core.gbl.MatsimRandom;
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
        if (vId == null) return;
        ElectricVehicle ev  = electricFleet.getElectricVehicles().get(vId);
        if (ev == null) return;

        double socTarget = readSocTarget(e.getPersonId());

        List<Charger> compatibleChargers =
            chargingInfrastructure.getChargers().values().stream()
                .filter(c -> c.getLink().getId().equals(e.getLinkId()))
                .filter(c -> ev.getChargerTypes().contains(
                    c.getSpecification().getChargerType()
                ))
                .toList();

        if (compatibleChargers.isEmpty()) {
            log.info("[TargetSocChargingHandler] Veicolo {} Nessun charger compatibile sul link {}", vId, e.getLinkId());
            return;
        }

        // Strategia: scegli charger (random)
        Charger selected = compatibleChargers.get(
            MatsimRandom.getRandom().nextInt(compatibleChargers.size())
        );

        ChargingStrategy strategy = new ChargeUpToMaxSocStrategy(
            selected.getSpecification(),
            ev,
            socTarget
        );

        selected.getLogic().addVehicle(ev, strategy, e.getTime());
        log.info("[TargetSocChargingHandler] Veicolo {} assegnato a charger {} (hub {})", vId, selected.getId(), hubManager.getHubIdForCharger(selected.getId()));
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
