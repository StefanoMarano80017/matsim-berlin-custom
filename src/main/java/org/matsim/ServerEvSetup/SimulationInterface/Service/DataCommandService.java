package org.matsim.ServerEvSetup.SimulationInterface.Service;

import org.matsim.CustomEvModule.EVfleet.EvFleetManager;
import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.matsim.CustomEvModule.Hub.ChargingHub;
import org.matsim.CustomEvModule.Hub.HubManager;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.vehicles.Vehicle;

public class DataCommandService {

    private final EvFleetManager evFleetManager;
    private final HubManager hubManager;

    public DataCommandService(
        EvFleetManager evFleetManager,
        HubManager hubManager
    ) {
        this.evFleetManager = evFleetManager;
        this.hubManager = hubManager;
    }

    public void updateFleetSoc(ElectricFleet fleet) {
        if (fleet != null) {
            evFleetManager.updateSoc(fleet);
        }
    }

    public void updateEvState(Id<Vehicle> vehicleId, EvModel.State state) {
        EvModel model = evFleetManager.getVehicle(vehicleId);
        if (model != null) {
            model.setState(state);
        }
    }

    public EvModel getVehicle(String vehicleId) {
        return evFleetManager.getVehicle(Id.createVehicleId(vehicleId));
    }

    public void resetDirty() {
        evFleetManager.getFleet().values().forEach(EvModel::resetDirty);
    }

    public void updateEvPosition(Id<Vehicle> vehicleId, double x, double y) {
        EvModel ev = evFleetManager.getVehicle(vehicleId);
        if(ev != null) {
            ev.setCoord(x, y);
        }
    }

    /*
    *  Hub interface
    */
    public void handleChargingStart(Id<Charger> chargerId, String vehicleId) {
        try {
            hubManager.incrementOccupancy(chargerId, vehicleId);
        } catch (Exception ignored) {}
    }

    public void handleChargingEnd(Id<Charger> chargerId, double energy) {
        try {
            String hubId = hubManager.getHubIdForCharger(chargerId);
            ChargingHub hub = hubManager.getHub(hubId);
            if (hub != null) {
                hub.decrementOccupancy(chargerId, energy);
            }
        } catch (Exception ignored) {}
    }

    public void resetCurrentEnergy() {
        hubManager.getAllHubs()
                  .forEach(ChargingHub::resetCurrentEnergyDelivering);
    }

    public void updateEnergyDelivering() {
        hubManager.getAllHubs().forEach(hub ->
            hub.getChargerUnits().forEach(charger -> {
                String evId = charger.getOccupyingEvId();
                if (evId != null) {
                    EvModel ev = getVehicle(evId);
                    if (ev != null) {
                        //double deltaSoc = ev.getCurrentSoc() - ev.getLastSoc();
                        double deltaEnergy = ev.getCurrentEnergyJoules() - ev.getLastEnergyJoules();
                        charger.setCurrentEnergyDelivering(
                                (deltaEnergy/3.6e6) * 1.11
                        );
                    }
                }
            })
        );
    }

    public void updateChargerState(Id<Charger> chargerId, boolean active){
        hubManager.setChargerActive(chargerId, active);
    }

}
