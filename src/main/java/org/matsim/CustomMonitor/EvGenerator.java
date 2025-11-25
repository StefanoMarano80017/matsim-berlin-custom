package org.matsim.CustomMonitor;

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

    /** Carica il CSV completo */
    public static void loadCsv(String csvPath) throws IOException {
        evDataset = Files.lines(Path.of(csvPath))
                            .skip(1) // salta intestazione
                            .map(EvGenerator::parseCsvLine)
                            .collect(Collectors.toList());
        System.out.println("[EvGenerator] Caricati " + evDataset.size() + " modelli EV dal CSV.");
    }

    /** Imposta seed per generazione riproducibile */
    public static void setSeed(long seed) {
        rng.setSeed(seed);
    }

    /** Genera una lista di EvModel casuali */
    public static List<EvModel> generateEvModels(int count, double socMean, double socStdDev) {
        if (evDataset.isEmpty()) throw new IllegalStateException("CSV EV non caricato!");

        List<EvModel> evModels = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            EvCsvEntry entry = evDataset.get(rng.nextInt(evDataset.size()));
            Id<Vehicle> vehicleId = Id.createVehicleId("EV_" + i);

            // SOC iniziale casuale (0-1)
            double soc = socMean + rng.nextGaussian() * socStdDev;
            soc = Math.max(0, Math.min(1, soc));

            EvModel evModel = new EvModel(
                    vehicleId,
                    entry.brand(),
                    entry.model(),
                    entry.batteryCapacityKWh(),
                    entry.efficiencyWhPerKm() / 1000.0, // Wh/km -> kWh/km
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

            // Aggiorna SOC iniziale
            evModel.updateDynamicState(soc, evModel.getNominalCapacityKwh() * 3.6e6 * soc, false);

            evModels.add(evModel);
        }
        return evModels;
    }

    private static EvCsvEntry parseCsvLine(String line) {
        String[] t = line.split(",", -1);
        return new EvCsvEntry(
                t[0], 
                t[1], 
                Double.parseDouble(t[2]),
                Double.parseDouble(t[3]),
                t[4], 
                Integer.parseInt(t[5]),
                Double.parseDouble(t[6]), 
                Double.parseDouble(t[7]), 
                Double.parseDouble(t[8]),
                Double.parseDouble(t[9]), 
                Double.parseDouble(t[10]), 
                t[11],
                Double.parseDouble(t[12]), 
                Double.parseDouble(t[13]), 
                Integer.parseInt(t[14]),
                t[15], 
                t[16], 
                Integer.parseInt(t[17]), 
                Integer.parseInt(t[18]), 
                Integer.parseInt(t[19]),
                t[20]
        );
    }
}
