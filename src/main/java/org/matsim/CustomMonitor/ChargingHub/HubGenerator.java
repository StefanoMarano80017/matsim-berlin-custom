package org.matsim.CustomMonitor.ChargingHub;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.infrastructure.*;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HubGenerator {

    private final Network network;
    private final ChargingInfrastructureSpecification infrastructureSpec;

    public HubGenerator(Network network, ChargingInfrastructureSpecification infraSpec) {
        this.network = network;
        this.infrastructureSpec = infraSpec;
    }

    /**
     * CSV: hubId,linkId,nColonnine,type,power
     */
    public void generateHubsFromCSV(Path csvFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(csvFile)) {

            String header = reader.readLine(); // skip header
            if (header == null) return;

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.trim().isEmpty()) continue; // skip blank lines

                // Support both "," and "\t"
                String[] rawParts = line.split("[,\t]", -1);

                if (rawParts.length < 5) {
                    System.err.println("[HubGenerator] Riga ignorata (colonne insufficienti) line "
                            + lineNumber + ": " + line);
                    continue;
                }

                // Trim all values
                String[] parts = new String[rawParts.length];
                for (int i = 0; i < rawParts.length; i++) {
                    parts[i] = rawParts[i].trim();
                }

                try {
                    String hubId = parts[0];
                    String linkId = parts[1];
                    int nColonnine = parseIntSafe(parts[2], 1);
                    String type = parts[3];
                    double power = parseDoubleSafe(parts[4], 11.0);
                    double powerW = power * 1000.0; 
                    // Validate link existence
                    Link link = network.getLinks().get(Id.createLinkId(linkId));
                    if (link == null) {
                        System.err.println("[HubGenerator] Link non trovato: " + linkId
                                + " (riga " + lineNumber + ")");
                        continue;
                    }

                    // Generate chargers
                    for (int i = 0; i < nColonnine; i++) {

                        String chargerId = hubId + "_col" + (i + 1);

                        Attributes attrs = new AttributesImpl();
                        attrs.putAttribute("hubId", hubId);

                        ImmutableChargerSpecification chargerSpec =
                                ImmutableChargerSpecification.newBuilder()
                                        .id(Id.create(chargerId, Charger.class))
                                        .linkId(link.getId())
                                        .chargerType(type)
                                        .plugPower(powerW)
                                        .plugCount(1) // 1 plug per colonnina
                                        .attributes(attrs)
                                        .build();

                        infrastructureSpec.addChargerSpecification(chargerSpec);
                    }

                } catch (Exception ex) {
                    System.err.println("[HubGenerator] Errore parsing riga "
                            + lineNumber + ": " + line);
                    ex.printStackTrace();
                }
            }
        }
    }

    // -----------------------------
    // Parsing helpers robusti
    // -----------------------------

    private int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ex) {
            System.err.println("[HubGenerator] Warning: valore intero malformato \"" + s
                    + "\" -> uso fallback=" + fallback);
            return fallback;
        }
    }

    private double parseDoubleSafe(String s, double fallback) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception ex) {
            System.err.println("[HubGenerator] Warning: valore double malformato \"" + s
                    + "\" -> uso fallback=" + fallback);
            return fallback;
        }
    }
}
