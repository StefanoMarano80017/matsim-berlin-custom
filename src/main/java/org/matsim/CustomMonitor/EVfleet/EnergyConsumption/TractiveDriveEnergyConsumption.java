package org.matsim.CustomMonitor.EVfleet.EnergyConsumption;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.CustomMonitor.EVfleet.EvFleetManager;
import org.matsim.CustomMonitor.model.EvModel;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

/**
 * Modello di consumo basato sulla trazione (Tractive Energy Model), che simula
 * l'energia necessaria per superare le forze di resistenza (rotolamento, aerodinamica, pendenza).
 *
 * Utilizza la velocita' media sul link e parametri fisici stimati dal EvModel.
 * NOTA: Richiede che l'attributo "slope" (pendenza in radianti o gradi) sia disponibile sul Link.
 */
public class TractiveDriveEnergyConsumption implements DriveEnergyConsumption {

    private static final Logger log = LogManager.getLogger(TractiveDriveEnergyConsumption.class);

    // Costanti Fisiche Globali
    private static final double GRAVITY = 9.81; // Accelerazione di gravità (m/s^2)
    private static final double AIR_DENSITY = 1.225; // Densità dell'aria (kg/m^3)

    private final ElectricVehicle electricVehicle;
    private final EvFleetManager fleetManager;
    private final double regenerativeEfficiency = 0.75; // Efficienza di ricarica (rigenerazione)

    public TractiveDriveEnergyConsumption(ElectricVehicle electricVehicle, EvFleetManager fleetManager) {
        this.electricVehicle = electricVehicle;
        this.fleetManager = fleetManager;
    }

    /**
     * Recupera la pendenza del link e la converte in radianti.
     */
    private double getSlopeAngleRadians(Link link) {
        // Si assume che l'attributo "slope" contenga la pendenza in gradi.
        // Se l'attributo è mancante o non gestibile, si assume pendenza zero.
        try {
            Double slopeDegrees = (Double) link.getAttributes().getAttribute("slope");
            if (slopeDegrees != null) {
                // Converti gradi in radianti
                return Math.toRadians(slopeDegrees);
            }
        } catch (Exception e) {
            // Ignora se l'attributo "slope" non è disponibile o non è un Double
        }
        return 0.0; // Nessuna pendenza
    }


    // --- Metodi di Stima ---
    private double calculateMassEstimate(EvModel evData) {
        // Stima: Massa base (es. 1500 kg) + Peso batteria (es. 7 kg per kWh)
        final double BASE_MASS_KG = 1500.0;
        final double BATTERY_WEIGHT_FACTOR_KG_PER_KWH = 7.0; // Stima approssimativa

        double estimatedMass = BASE_MASS_KG + (evData.getNominalCapacityKwh() * BATTERY_WEIGHT_FACTOR_KG_PER_KWH);

        // Aggiusta la stima in base al segmento (molto approssimativo)
        if (evData.getSegment().equalsIgnoreCase("SUV")) {
            estimatedMass += 300.0;
        } else if (evData.getSegment().equalsIgnoreCase("Hatchback")) {
            estimatedMass -= 100.0;
        }
        return estimatedMass;
    }

    private double calculateCrEstimate() {
        // Coefficiente di resistenza al rotolamento standard per pneumatici moderni
        return 0.01; 
    }

    private double calculateCdaEstimate(EvModel evData) {
        // 1. Stima del Coefficiente Aerodinamico (Cd) basato sul tipo di carrozzeria
        double cd;
        String carBodyType = evData.getCarBodyType();
        double widthMm = evData.getWidthMm();
        double heightMm = evData.getHeightMm();
        String bodyTypeLower = carBodyType.toLowerCase();
        // Priorità 1: I veicoli più grandi o con forma meno aerodinamica (SUV, Van) hanno la priorità
        if (bodyTypeLower.contains("suv") || bodyTypeLower.contains("van") || bodyTypeLower.contains("wagon") || bodyTypeLower.contains("estate")) {
            // Copre SUV, Minivan, Small Passenger Van, Station/Estate
            cd = 0.35;
        } 
        // Priorità 2: Veicoli compatti, ma non ottimizzati come le berline pure
        else if (bodyTypeLower.contains("hatchback") || bodyTypeLower.contains("crossover")) {
            cd = 0.32;
        } 
        // Priorità 3: Forme più aerodinamiche (Berline e Coupé)
        else if (bodyTypeLower.contains("sedan") || bodyTypeLower.contains("liftback") || bodyTypeLower.contains("coupe") || bodyTypeLower.contains("cabriolet")) {
            // Copre Sedan, Liftback Sedan, Coupe, Cabriolet
            cd = 0.28;
        } 
        // Priorità 4: Valore predefinito se non rientra in nessuna categoria specifica
        else {
            cd = 0.30; // Default o valore medio
        }
        // 2. Stima dell'Area Frontale (A) in metri quadrati
        // Approssimazione: 80% del rettangolo (W x H)
        double widthM = widthMm / 1000.0;
        double heightM = heightMm / 1000.0;
        double areaFrontalM2 = 0.8 * widthM * heightM; 
        // 3. Ritorna il prodotto Cd * A
        return cd * areaFrontalM2;
    }


    @Override
    public double calcEnergyConsumption(Link link, double travelTime, double linkEnterTime) {
        EvModel evData = fleetManager.getVehicle(electricVehicle.getId());
        if (evData == null) {
            log.warn("WARN: Veicolo " + electricVehicle.getId() + " non trovato. Consumo impostato a 0.");
            return 0.0;
        }

        // 1. Recupero parametri fisici stimati
        double massKg     = calculateMassEstimate(evData);
        double cr         = calculateCrEstimate();
        double cda        = calculateCdaEstimate(evData);
        double efficiency = 0.90;

        // 2. Variabili di simulazione
        double linkLengthMeters  = link.getLength();
        double averageSpeedMps   = linkLengthMeters / travelTime;
        double slopeAngleRadians = getSlopeAngleRadians(link);
        double sinAlpha          = Math.sin(slopeAngleRadians);
        double cosAlpha          = Math.cos(slopeAngleRadians);

        // --- CALCOLO DELLE FORZE (Newton) ---

        // 2a. Forza di Rotolamento (Fr = m * g * Cr * cos(alpha))
        double fRolling = massKg * GRAVITY * cr * cosAlpha;

        // 2b. Forza Aerodinamica (Fa = 0.5 * rho * CdA * v^2)
        double fAero = 0.5 * AIR_DENSITY * cda * averageSpeedMps * averageSpeedMps;

        // 2c. Forza di Pendenza (Fg = m * g * sin(alpha))
        // Positiva in salita, Negativa in discesa
        double fSlope = massKg * GRAVITY * sinAlpha;

        // Forza Totale di Trazione richiesta (Ignorando F_accelerazione)
        double fTractive = fRolling + fAero + fSlope;

        // 3. CALCOLO DEL LAVORO (Energia) e Consumo (Joule)
        // Lavoro (Work) = Forza * Distanza
        // WorkJoules è l'energia meccanica necessaria o restituita
        double workJoules = fTractive * linkLengthMeters;
        double totalConsumptionJoules;

        if (workJoules >= 0) {
            // Trazione o resistenza: Energia consumata dalla batteria. Applica efficienza motrice.
            totalConsumptionJoules = workJoules / efficiency;
        } else {
            // Rigenerazione: Energia restituita alla batteria. Applica efficienza rigenerativa e motrice.
            // L'efficienza rigenerativa è solitamente il prodotto di (efficienza motrice) * (efficienza di ricarica)
            totalConsumptionJoules = workJoules * (1.0 - (1.0 - efficiency)) * regenerativeEfficiency;
        }

        // 4. Aggiornamento Distanza
        evData.addDistanceTraveled(linkLengthMeters);

        log.debug(String.format("Veicolo %s, Link %s - Consumo Tractive: %.2f J. Velocità media: %.2f m/s",
                electricVehicle.getId(), link.getId(), totalConsumptionJoules, averageSpeedMps));

        // MATSim gestirà l'aggiornamento della batteria nel DriveDischargingHandler
        return totalConsumptionJoules;
    }
}