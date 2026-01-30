package org.springboot.DTO.out.WebSocketDTO.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

@Schema(description = "Stato di un singolo charger in un hub")
public class ChargerStatus {

    private String  chargerId;
    private boolean occupied;
    private boolean active;

    /*
    *  valore cumulativo di energia distribuita
    */
    private double energy;

    /*
    *  valore di energia istantanea che sta venendo erogata
    */
    private double charging_energy;

    @Schema(description = "ID del veicolo elettrico collegato (obbligatorio se occupied=true)")
    private String evId;

    public ChargerStatus() {
    }

    public ChargerStatus(
        String chargerId,
        boolean occupied, 
        boolean active,
        double energy, 
        double charging_energy,
        String evId
    ) {
        this.chargerId  = chargerId;
        this.occupied   = occupied;
        this.active     = active;
        this.energy     = energy;
        this.evId       = evId;
        this.charging_energy = charging_energy;
    }

    public String getChargerId() {
        return chargerId;
    }

    public void setChargerId(String chargerId) {
        this.chargerId = chargerId;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public boolean isActive(){
        return this.active;
    }

    public void setActive(boolean active){
        this.active = active;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }

    public void setCharging_energy(double energy){
        if(occupied) this.charging_energy = energy;
    }

    public double getChargin_energy(){
        return charging_energy;
    }

    public String getEvId() {
        return evId;
    }

    public void setEvId(String evId) {
        this.evId = evId;
    }

    /**
     * Se occupied è true, evId deve essere valorizzato
     */
    @AssertTrue(message = "evId è obbligatorio quando il charger è occupato")
    public boolean isEvIdValidWhenOccupied() {
        if (!occupied) {
            return true;
        }
        return evId != null && !evId.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "ChargerStatus{" +
                "chargerId='" + chargerId + '\'' +
                ", occupied=" + occupied +
                ", energy=" + energy +
                ", evId='" + evId + '\'' +
                '}';
    }
}
