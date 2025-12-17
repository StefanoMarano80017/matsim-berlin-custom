package org.matsim.CustomMonitor.EVfleet.strategy.fleet;

import org.matsim.CustomMonitor.model.EvCsvEntry;
import org.matsim.CustomMonitor.model.EvModel;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

public class CsvFleetGenerationStrategy implements EvFleetStrategy {

    private static final int SEED = 42;
    private static final Random rng = new Random();
    private static List<EvCsvEntry> evDataset = new ArrayList<>();

    @Override
    public List<EvModel> generateFleet(Resource csvResource, int count, double socMean, double socStdDev) {
        try {
            loadCsv(csvResource);
            setSeed(SEED);
            return generateEvModels(count, socMean, socStdDev);
        } catch (Exception e) {
            throw new RuntimeException("Errore generazione EV da CSV", e);
        }
    }

    /** Carica il CSV completo con parsing robusto dal Resource */
    public static void loadCsv(Resource csvResource) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvResource.getInputStream()))) {
            evDataset = reader.lines()
                    .skip(1) // salta intestazione
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(CsvFleetGenerationStrategy::parseCsvLine)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
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
                    entry.efficiencyWhPerKm() / 1000.0,
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

            evModel.updateDynamicState(
                    soc,
                    evModel.getNominalCapacityKwh() * 3.6e6 * soc
            );

            evModels.add(evModel);
        }

        return evModels;
    }

    // ================================================================
    // ================= PARSER CSV ================================
    // ================================================================
    private static EvCsvEntry parseCsvLine(String line) {
        if (line.trim().isEmpty())
            return null;

        String[] t = line.split(",", -1);

        if (t.length < 21) {
            System.err.println("[EvGenerator] Riga CSV malformata, ignorata:\n" + line);
            return null;
        }

        try {
            return new EvCsvEntry(
                    t[0].trim(), t[1].trim(), parseDouble(t[2]), parseDouble(t[3]),
                    t[4].trim(), parseInt(t[5]), parseDouble(t[6]), parseDouble(t[7]),
                    parseDouble(t[8]), parseDouble(t[9]), parseDouble(t[10]), t[11].trim(),
                    parseDouble(t[12]), parseDouble(t[13]), parseInt(t[14]), t[15].trim(),
                    t[16].trim(), parseInt(t[17]), parseInt(t[18]), parseInt(t[19]), t[20].trim()
            );
        } catch (Exception ex) {
            System.err.println("[EvGenerator] Errore parsing riga CSV: " + line);
            ex.printStackTrace();
            return null;
        }
    }

    // ================================================================
    // ================= HELPER SICURI PER PARSING ===================
    // ================================================================
    private static double parseDouble(String s) {
        if (s == null) return 0.0;
        String cleaned = s.trim().replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            System.err.println("Impossibile parsare in Double: " + s + " (tentativo pulito: " + cleaned + ")");
            return 0.0;
        }
    }

    private static int parseInt(String s) {
        if (s == null) return 0;
        String cleaned = s.trim().replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) return 0;
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            System.err.println("Impossibile parsare in Integer: " + s + " (tentativo pulito: " + cleaned + ")");
            return 0;
        }
    }
}
