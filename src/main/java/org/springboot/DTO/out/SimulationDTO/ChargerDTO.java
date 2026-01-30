package org.springboot.DTO.out.SimulationDTO;

/**
 * DTO per un charger con supporto a tipo misto e potenza variabile.
 * 
 * Campi:
 * - chargerId: ID univoco della colonnina
 * - chargerType: Tipo di charger (AC, CCS, etc.)
 * - plugPowerKw: Potenza in kW
 * - availablePlugs: Set di tipi di plug disponibili (legacy, mantenuto per compatibilità)
 * - isActive: se è attivo o disattivato il charger
 */
public class ChargerDTO {

    private String chargerId;
    private String chargerType;
    private double plugPowerKw;

    /**
     * Costruttore con supporto a charger di tipo misto.
     */
    public ChargerDTO(String chargerId, String chargerType, double plugPowerKw) {
        this.chargerId = chargerId;
        this.chargerType = chargerType;
        this.plugPowerKw = plugPowerKw;
    }

    /**
     * Costruttore legacy per compatibilità.
     */
    public ChargerDTO(String chargerId) {
        this.chargerId = chargerId;
    }

    public String getChargerId() {
        return chargerId;
    }

    public void setChargerId(String chargerId) {
        this.chargerId = chargerId;
    }

    public String getChargerType() {
        return chargerType;
    }

    public void setChargerType(String chargerType) {
        this.chargerType = chargerType;
    }

    public double getPlugPowerKw() {
        return plugPowerKw;
    }

    public void setPlugPowerKw(double plugPowerKw) {
        this.plugPowerKw = plugPowerKw;
    }

}
