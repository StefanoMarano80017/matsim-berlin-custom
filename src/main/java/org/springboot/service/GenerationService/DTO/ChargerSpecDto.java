package org.springboot.service.generationService.DTO;

/**
 * Modello di dominio lato server per un charger.
 * Completamente indipendente da MATSim.
 * 
 * Contiene le informazioni essenziali di un charger necessarie per:
 * - trasportare tramite bridge
 * - tradurre in ImmutableChargerSpecification quando registrato in MATSim
 */
public class ChargerSpecDto {

    private final String chargerId;
    private final String linkId;
    private final String chargerType;
    private final double plugPowerKw;
    private final int plugCount;

    public ChargerSpecDto(
        String chargerId,
        String linkId,
        String chargerType,
        double plugPowerKw,
        int plugCount
    ) {
        this.chargerId = chargerId;
        this.linkId = linkId;
        this.chargerType = chargerType;
        this.plugPowerKw = plugPowerKw;
        this.plugCount = plugCount;
    }

    public String getChargerId() {
        return chargerId;
    }

    public String getLinkId() {
        return linkId;
    }

    public String getChargerType() {
        return chargerType;
    }

    public double getPlugPowerKw() {
        return plugPowerKw;
    }

    public int getPlugCount() {
        return plugCount;
    }

    @Override
    public String toString() {
        return "ChargerSpecDto{" +
                "chargerId='" + chargerId + '\'' +
                ", linkId='" + linkId + '\'' +
                ", chargerType='" + chargerType + '\'' +
                ", plugPowerKw=" + plugPowerKw +
                ", plugCount=" + plugCount +
                '}';
    }
}
