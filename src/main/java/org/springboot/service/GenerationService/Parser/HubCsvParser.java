package org.springboot.service.generationService.Parser;

import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springboot.service.generationService.DTO.ChargerSpecDto;
import org.springboot.service.generationService.DTO.HubSpecDto;

/**
 * Parser CSV per hub di ricarica lato server con supporto colonnine miste.
 * 
 * Responsabilità:
 * - leggere il file CSV
 * - validare i dati
 * - generare modelli di dominio puri (HubSpecDto)
 * 
 * NON dipende da MATSim, solo da modelli di dominio server.
 * 
 * CSV format: hubId,linkId,type,power
 * Una riga per colonnina. Supporta colonnine di tipo misto (AC, CCS, etc.) con potenze variabili.
 * Example:
 *   hub1,link123,AC,11.0
 *   hub1,link123,CCS,50.0
 *   hub2,link456,AC,22.0
 */
public class HubCsvParser {

    private static final Logger log = LogManager.getLogger(HubCsvParser.class);

    /**
     * Parsa un file CSV di hub e genera una lista di HubSpecDto.
     * Supporta colonnine di tipo misto sullo stesso hub.
     * 
     * @param csvResource Risorsa CSV con i dati degli hub
     * @return Lista di HubSpecDto parsed
     * @throws IOException Se il file non può essere letto
     */
    public List<HubSpecDto> parseHubs(Resource csvResource) throws IOException {
        Map<String, HubSpecDto> hubs = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvResource.getInputStream()))) {
            String header = reader.readLine();
            if (header == null) {
                log.warn("[HubCsvParser] CSV file is empty");
                return new ArrayList<>(hubs.values());
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    parseLine(line, lineNumber, hubs);
                } catch (Exception e) {
                    log.error("[HubCsvParser] Error parsing line {}: {}", lineNumber, e.getMessage());
                }
            }
        }

        return new ArrayList<>(hubs.values());
    }

    /**
     * Parsa una singola riga CSV (una colonnina) e aggiunge il charger all'hub.
     * Supporta colonnine di tipo misto.
     * 
     * @param line Riga CSV: hubId,linkId,type,power
     * @param lineNumber Numero di riga (per log)
     * @param hubs Mappa di hub (viene aggiornata)
     */
    private void parseLine(String line, int lineNumber, Map<String, HubSpecDto> hubs) {
        String[] parts = line.split("[,\t]", -1);
        
        if (parts.length < 4) {
            throw new IllegalArgumentException("Insufficient columns (expected 4, got " + parts.length + ")");
        }

        String hubId = parts[0].trim();
        String linkId = parts[1].trim();
        String chargerType = parts[2].trim();
        double powerKw = parseDoubleSafe(parts[3], 11.0);

        if (hubId.isEmpty() || linkId.isEmpty()) {
            throw new IllegalArgumentException("hubId or linkId is empty");
        }

        if (chargerType.isEmpty()) {
            throw new IllegalArgumentException("chargerType is empty");
        }

        if (powerKw <= 0) {
            throw new IllegalArgumentException("power must be > 0");
        }

        // Crea o recupera l'hub
        HubSpecDto hub = hubs.computeIfAbsent(hubId, k -> new HubSpecDto(hubId, linkId));

        // Valida che il linkId sia coerente per lo stesso hub
        if (!hub.getLinkId().equals(linkId)) {
            throw new IllegalArgumentException("Hub " + hubId + " has inconsistent linkIds: " + 
                hub.getLinkId() + " vs " + linkId);
        }

        // Genera ID univoco per la colonnina (incrementale)
        int chargerIndex = hub.getChargers().size() + 1;
        String chargerId = hubId + "_col" + chargerIndex;

        ChargerSpecDto charger = new ChargerSpecDto(
            chargerId,
            linkId,
            chargerType,
            powerKw,
            1 // plugCount
        );
        hub.addCharger(charger);

        log.debug("[HubCsvParser] Parsed charger {} (type: {}, power: {} kW) for hub {}", 
            chargerId, chargerType, powerKw, hubId);
    }

    private double parseDoubleSafe(String s, double fallback) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            log.warn("[HubCsvParser] Failed to parse double '{}', using fallback {}", s, fallback);
            return fallback;
        }
    }
}
