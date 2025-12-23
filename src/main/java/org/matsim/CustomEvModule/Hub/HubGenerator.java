package org.matsim.CustomEvModule.Hub;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.infrastructure.*;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HubGenerator {

    private static final Logger log = LogManager.getLogger(HubGenerator.class);

    private final Network network;

    public HubGenerator(Network network) {
        this.network = network;
    }

    /**
     * CSV: hubId,linkId,nColonnine,type,power
     * Restituisce una mappa hubId -> ChargingHub con i charger registrati.
     */
    public Map<String, ChargingHub> generateHubsFromCSV(Resource csvResource, ChargingInfrastructureSpecification infraSpec) throws IOException {
        Map<String, ChargingHub> hubs = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvResource.getInputStream()))) {
            String header = reader.readLine(); // skip header
            if (header == null) return hubs;

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;

                String[] rawParts = line.split("[,\t]", -1);
                if (rawParts.length < 5) {
                    log.error("[HubGenerator] Riga ignorata (colonne insufficienti) line " + lineNumber + ": " + line);
                    continue;
                }

                String hubId    = rawParts[0].trim();
                String linkId   = rawParts[1].trim();
                int nColonnine  = parseIntSafe(rawParts[2], 1);
                String type     = rawParts[3].trim();
                double power    = parseDoubleSafe(rawParts[4], 11.0) * 1000.0;

                Link link = network.getLinks().get(Id.createLinkId(linkId));
                if (link == null) {
                    log.error("[HubGenerator] Link non trovato: " + linkId + " (riga " + lineNumber + ")");
                    continue;
                }

                ChargingHub hub = new ChargingHub(hubId, Id.createLinkId(linkId));

                for (int i = 0; i < nColonnine; i++) {
                    String chargerIdStr = hubId + "_col" + (i + 1);
                    Id<Charger> chargerId = Id.create(chargerIdStr, Charger.class);
                    Attributes attrs = new AttributesImpl();
                    attrs.putAttribute("hubId", hubId);
                    ImmutableChargerSpecification chargerSpec = ImmutableChargerSpecification.newBuilder()
                            .id(chargerId)
                            .linkId(link.getId())
                            .chargerType(type)
                            .plugPower(power)
                            .plugCount(1)
                            .attributes(attrs)
                            .build();

                    infraSpec.addChargerSpecification(chargerSpec);
                    hub.addCharger(chargerId);
                }

                hubs.put(hubId, hub);
            }
        }
        return hubs;
    }

    private int parseIntSafe(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception ex) { return fallback; }
    }

    private double parseDoubleSafe(String s, double fallback) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception ex) { return fallback; }
    }
}
