package org.matsim.CustomMonitor.EVfleet;

import org.matsim.CustomMonitor.model.EvCsvEntry;
import org.matsim.CustomMonitor.model.EvModel;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class EvGenerator {

    private static final Random rng = new Random();
    private static List<EvCsvEntry> evDataset = new ArrayList<>();

    /** Carica il CSV completo con parsing robusto */
    public static void loadCsv(Path csvPath) throws IOException {
        evDataset = Files.lines(csvPath)
                .skip(1) // salta intestazione
                .map(String::trim)
                .filter(line -> !line.isEmpty()) // ignora righe vuote
                .map(EvGenerator::parseCsvLine)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        System.out.println("[EvGenerator] Caricati " + evDataset.size() + " modelli EV dal CSV.");
    }

    /** Imposta seed per generazione riproducibile */
    public static void setSeed(long seed) {
        rng.setSeed(seed);
    }

    /** Genera una lista di EvModel casuali */
    public static List<EvModel> generateEvModels(int count, double socMean, double socStdDev) {
        if (evDataset.isEmpty())
            throw new IllegalStateException("CSV EV non caricato!");

        List<EvModel> evModels = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            EvCsvEntry entry = evDataset.get(rng.nextInt(evDataset.size()));
            Id<Vehicle> vehicleId = Id.createVehicleId("EV_" + i);

            // SOC iniziale casuale (0–1)
            double soc = socMean + rng.nextGaussian() * socStdDev;
            soc = Math.max(0, Math.min(1, soc));

            EvModel evModel = new EvModel(
                    vehicleId,
                    entry.brand(),
                    entry.model(),
                    entry.batteryCapacityKWh(),
                    entry.efficiencyWhPerKm() / 1000.0, // Wh/km → kWh/km
                    entry.batteryType(),
                    entry.numberOfCells(),
                    entry.torqueNm(),
                    entry.topSpeedKmh(),
                    entry.rangeKm(),
                    entry.acceleration0To100(),
                    entry.fastChargingPowerKwDc(),
                    entry.fastChargePort(),
                    entry.towingCapacityKg(),
                    entry.cargoVolumeL(),
                    entry.seats(),
                    entry.drivetrain(),
                    entry.segment(),
                    entry.lengthMm(),
                    entry.widthMm(),
                    entry.heightMm(),
                    entry.carBodyType()
            );

            // Stato iniziale
            evModel.updateDynamicState(
                    soc,
                    evModel.getNominalCapacityKwh() * 3.6e6 * soc
            );

            evModel.setCharging(false);
            evModels.add(evModel);
        }

        return evModels;
    }


    // ================================================================
    // ============== PARSER CSV ROBUSTO ==============================
    // ================================================================

    private static EvCsvEntry parseCsvLine(String line) {
        if (line.trim().isEmpty())
            return null;

        String[] t = line.split(",", -1); // -1 conserva campi vuoti

        if (t.length < 21) {
            System.err.println("[EvGenerator] Riga CSV malformata, ignorata:\n" + line);
            return null;
        }

        try {
            return new EvCsvEntry(
                    t[0].trim(),      // brand
                    t[1].trim(),      // model
                    parseDouble(t[2]),
                    parseDouble(t[3]),
                    t[4].trim(),
                    parseInt(t[5]),
                    parseDouble(t[6]),
                    parseDouble(t[7]),
                    parseDouble(t[8]),
                    parseDouble(t[9]),
                    parseDouble(t[10]),
                    t[11].trim(),
                    parseDouble(t[12]),
                    parseDouble(t[13]),
                    parseInt(t[14]),
                    t[15].trim(),
                    t[16].trim(),
                    parseInt(t[17]),
                    parseInt(t[18]),
                    parseInt(t[19]),
                    t[20].trim()
            );
        } catch (Exception ex) {
            System.err.println("[EvGenerator] Errore parsing riga CSV: " + line);
            ex.printStackTrace();
            return null; // scarta riga malformata
        }
    }
    
    // ================================================================
    // ============== HELPER SICURI PER PARSING =======================
    // ================================================================

    private static double parseDouble(String s) {
        s = s.trim();
        if (s.isEmpty()) return 0.0;
        return Double.parseDouble(s);
    }

    private static int parseInt(String s) {
        s = s.trim();
        if (s.isEmpty()) return 0;
        return Integer.parseInt(s);
    }
}
