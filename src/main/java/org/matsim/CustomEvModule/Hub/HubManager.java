package org.matsim.CustomEvModule.Hub;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import org.springboot.service.GenerationService.DTO.ChargerSpecDto;
import org.springboot.service.GenerationService.DTO.HubSpecDto;
import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HubManager {

    private static final Logger log = LogManager.getLogger(HubManager.class);

    private final HubGenerator generator;
    private final ChargingInfrastructureSpecification infraSpec;
    private final Network network;

    private Map<String, ChargingHub> hubs = new HashMap<>();
    private final Map<Id<Charger>, String> charger2hub = new HashMap<>();

    public HubManager(Network network, ChargingInfrastructureSpecification infraSpec) {
        this.infraSpec = infraSpec;
        this.network = network;
        this.generator = new HubGenerator(network);
    }

    /**
     * Restituisce una lista di charger libero e compatibile con i tipi richiesti su uno specifico link.
     * @param linkId link su cui cercare charger
     * @param compatibleTypes tipi di charger compatibili con il veicolo
     * @return Optional<Id<Charger>> disponibile
     */
    public List<Charger> getAvailableChargerForLink(Id<Link> linkId, ImmutableList<String> compatibleTypes, ChargingInfrastructure chargingInfrastructure) {
        return chargingInfrastructure.getChargers().values().stream()
                .filter(c -> c.getLink().getId().equals(linkId))
                .filter(c -> compatibleTypes.contains(
                    c.getSpecification().getChargerType()
                ))
                .toList();
    }

    /**
     * Registra i ChargingHub pre-generati dal server a partire da HubSpecDto.
     * 
     * Questo metodo:
     * 1. Riceve i modelli di dominio puri (HubSpecDto) dal server
     * 2. Traduce ogni ChargerSpecDto in ImmutableChargerSpecification (MATSim)
     * 3. Registra le specifiche nell'infrastruttura MATSim
     * 4. Crea i ChargingHub e li registra in questo manager
     * 
     * @param hubSpecs Lista di specifiche hub (modelli di dominio server)
     */
    public void registerChargingHubsFromSpecs(List<HubSpecDto> hubSpecs) {
        if (hubSpecs == null || hubSpecs.isEmpty()) {
            throw new IllegalArgumentException("hubSpecs cannot be null or empty");
        }

        log.info("[HubManager] Registering {} hub specifications from server", hubSpecs.size());

        // Per ogni hub spec, crea un ChargingHub e registra i charger in MATSim
        for (HubSpecDto hubSpec : hubSpecs) {
            String hubId = hubSpec.getHubId();
            String linkIdStr = hubSpec.getLinkId();
            
            // Valida il link
            Id<Link> linkId = Id.createLinkId(linkIdStr);
            Link link = network.getLinks().get(linkId);
            if (link == null) {
                log.error("[HubManager] Link not found: {} (hub {})", linkIdStr, hubId);
                continue;
            }

            // Crea il ChargingHub
            ChargingHub hub = new ChargingHub(hubId, linkId);

            // Per ogni charger spec, crea la specifica MATSim e registrala
            for (ChargerSpecDto chargerSpec : hubSpec.getChargers()) {
                String chargerId = chargerSpec.getChargerId();
                String chargerType = chargerSpec.getChargerType();
                double powerKw = chargerSpec.getPlugPowerKw();
                int plugCount = chargerSpec.getPlugCount();

                Id<Charger> chargerIdMatSim = Id.create(chargerId, Charger.class);

                // Crea gli attributi per MATSim
                Attributes attrs = new AttributesImpl();
                attrs.putAttribute("hubId", hubId);

                // Crea la specifica del charger per MATSim (in Watt)
                ImmutableChargerSpecification chargerSpecMatSim = ImmutableChargerSpecification.newBuilder()
                        .id(chargerIdMatSim)
                        .linkId(linkId)
                        .chargerType(chargerType)
                        .plugPower(powerKw * 1000.0) // Converti kW in W
                        .plugCount(plugCount)
                        .attributes(attrs)
                        .build();

                // Registra la specifica nell'infrastruttura
                infraSpec.addChargerSpecification(chargerSpecMatSim);

                // Registra il charger nel hub
                hub.addCharger(chargerIdMatSim, Set.of(chargerType));

                // Registra il mapping charger -> hub
                charger2hub.put(chargerIdMatSim, hubId);
            }

            // Registra l'hub nel manager
            hubs.put(hubId, hub);
            log.debug("[HubManager] Registered hub {} with {} chargers", hubId, hubSpec.getChargers().size());
        }

        log.info("[HubManager] Successfully registered all hub specifications");
    }

    /**
     * Registra i ChargingHub pre-generati dal server.
     * Costruisce anche la mappa charger2hub dalla specifica dell'infrastruttura.
     * 
     * @param chargingHubs Collection di hub pre-generati dal server
     */
    public void registerChargingHubs(Collection<ChargingHub> chargingHubs) {
        if (chargingHubs == null || chargingHubs.isEmpty())
            throw new IllegalArgumentException("chargingHubs cannot be null or empty");

        log.info("[HubManager] Registering {} pre-generated charging hubs", chargingHubs.size());
        
        this.hubs = chargingHubs.stream()
                .collect(Collectors.toMap(ChargingHub::getId, hub -> hub));

        infraSpec.getChargerSpecifications().values().forEach(chSpec -> {
            Id<Charger> chId = chSpec.getId();
            String hubId = (String) chSpec.getAttributes().getAttribute("hubId");
            if (hubId == null) throw new IllegalArgumentException("Charger " + chId + " senza hubId");
            charger2hub.put(chId, hubId);
        });

        log.debug("[HubManager] Charging hubs registered successfully.");
    }

    public void createHub(Resource csvFile) {
        try {
            this.hubs = generator.generateHubsFromCSV(csvFile, infraSpec);
            log.debug("[HubManager] Hub e colonnine generate con successo.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        infraSpec.getChargerSpecifications().values().forEach(chSpec -> {
            Id<Charger> chId = chSpec.getId();
            String hubId = (String) chSpec.getAttributes().getAttribute("hubId");
            if (hubId == null) throw new IllegalArgumentException("Charger " + chId + " senza hubId");
            charger2hub.put(chId, hubId);
        });

        log.debug("[HubManager] Hub e colonnine create e registrate.");
    }

    public ChargingHub getHub(String hubId) {
        return hubs.get(hubId);
    }

    public Collection<ChargingHub> getChargingHubs() {
        return hubs.values();
    }

    public String getHubIdForCharger(Id<Charger> chargerId) {
        return charger2hub.get(chargerId);
    }

    public void incrementOccupancy(Id<Charger> chargerId, String evId) {
        ChargingHub hub = getHub(getHubIdForCharger(chargerId));
        if (hub != null) {
            hub.incrementOccupancy(chargerId, evId);
        }
    }

    public void decrementOccupancy(Id<Charger> chargerId, double energy) {
        ChargingHub hub = getHub(getHubIdForCharger(chargerId));
        if(hub != null){
            hub.decrementOccupancy(chargerId, energy);
        }
    }

    public Collection<ChargingHub> getAllHubs() {
        return Collections.unmodifiableCollection(hubs.values());
    }

    // ---------------- Dirty flags ----------------
    public Set<ChargingHub> getDirtyHubs() {
        return hubs.values().stream()
                   .filter(ChargingHub::isDirty)
                   .collect(Collectors.toSet());
    }

    public void resetDirtyFlags() {
        hubs.values().forEach(ChargingHub::resetDirty);
    }
}
