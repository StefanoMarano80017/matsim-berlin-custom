package org.springboot.service.GenerationService.Parser;

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
import org.springboot.service.GenerationService.DTO.ChargerSpecDto;
import org.springboot.service.GenerationService.DTO.HubSpecDto;

/**
 * Parser CSV per hub di ricarica lato server.
 * 
 * Responsabilità:
 * - leggere il file CSV
 * - validare i dati
 * - generare modelli di dominio puri (HubSpecDto)
 * 
 * NON dipende da MATSim, solo da modelli di dominio server.
 * 
 * CSV format: hubId,linkId,nColonnine,type,power
 * Example: hub1,link123,2,default,11
 */
public class HubCsvParser {

    private static final Logger log = LogManager.getLogger(HubCsvParser.class);

    /**
     * Parsa un file CSV di hub e genera una lista di HubSpecDto.
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
     * Parsa una singola riga CSV e aggiunge il charger all'hub.
     * 
     * @param line Riga CSV: hubId,linkId,nColonnine,type,power
     * @param lineNumber Numero di riga (per log)
     * @param hubs Mappa di hub (viene aggiornata)
     */
    private void parseLine(String line, int lineNumber, Map<String, HubSpecDto> hubs) {
        String[] parts = line.split("[,\t]", -1);
        
        if (parts.length < 5) {
            throw new IllegalArgumentException("Insufficient columns (expected 5, got " + parts.length + ")");
        }

        String hubId = parts[0].trim();
        String linkId = parts[1].trim();
        int nChargers = parseIntSafe(parts[2], 1);
        String chargerType = parts[3].trim();
        double powerKw = parseDoubleSafe(parts[4], 11.0);

        if (hubId.isEmpty() || linkId.isEmpty()) {
            throw new IllegalArgumentException("hubId or linkId is empty");
        }

        if (nChargers <= 0) {
            throw new IllegalArgumentException("nChargers must be > 0");
        }

        if (powerKw <= 0) {
            throw new IllegalArgumentException("power must be > 0");
        }

        // Crea o recupera l'hub
        HubSpecDto hub = hubs.computeIfAbsent(hubId, k -> new HubSpecDto(hubId, linkId));

        // Aggiunge i charger all'hub
        for (int i = 0; i < nChargers; i++) {
            String chargerId = hubId + "_col" + (i + 1);
            ChargerSpecDto charger = new ChargerSpecDto(
                chargerId,
                linkId,
                chargerType,
                powerKw,
                1 // plugCount
            );
            hub.addCharger(charger);
        }

        log.debug("[HubCsvParser] Parsed hub {} with {} chargers", hubId, nChargers);
    }

    private int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            log.warn("[HubCsvParser] Failed to parse int '{}', using fallback {}", s, fallback);
            return fallback;
        }
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
