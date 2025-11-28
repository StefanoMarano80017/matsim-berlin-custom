package org.matsim.CustomMonitor.EVfleet;

import org.matsim.CustomMonitor.model.EvModel;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.contrib.ev.fleet.*;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.vehicles.*;
import org.matsim.api.core.v01.network.Network;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.Random;

/**
 * EvFleetManager: gestisce la flotta EV, la registrazione nello scenario
 * e l'aggiornamento dello stato dinamico (SOC, energia) dal QSim.
 */
public class EvFleetManager implements ChargingStartEventHandler, ChargingEndEventHandler {

    // --- Constants ---
    private static final int GENERATOR_SEED = 42;
    private static final String DEFAULT_CHARGER_TYPE = "default";

    // --- Fields ---
    private final Map<Id<Vehicle>, EvModel> fleet = new HashMap<>();

    public EvFleetManager() {
        // default constructor (per binding/injection)
    }

    // =========================================================================================
    // SECTION: Setup & Generation
    // =========================================================================================

    /**
     * Carica dataset EV, genera EvModel casuali e registra i veicoli nello Scenario.
     */
    public void generateFleetFromCsv(Path csv, Scenario scenario, int count, double socMean, double socStdDev) {
        System.out.println("[EvFleetManager] Avvio generazione flotta da CSV...");

        // 1. Caricamento e Generazione Modelli
        loadAndGenerateModels(csv, count, socMean, socStdDev);

        // 2. Registrazione Veicoli nello Scenario
        populateScenarioVehicles(scenario);

        assignDummyPlansToFleet(scenario);
    }


    /**
     * Debug della popolazione: stampa tutte le persone e i loro veicoli/piani.
     */
    public static void debugPopulation(Scenario scenario) {
        Population population = scenario.getPopulation();
        System.out.println("=== Debug Popolazione MATSim ===");
        System.out.println("Totale persone: " + population.getPersons().size());

        for (Person person : population.getPersons().values()) {
            // Se l'ID della persona inizia con "personEV_"
            if (person.getId().toString().startsWith("personEV_")) {
                System.out.println("Person ID: " + person.getId());

                for (Plan plan : person.getPlans()) {
                    for (PlanElement pe : plan.getPlanElements()) {
                        if (pe instanceof Activity) {
                            Activity act = (Activity) pe;
                            System.out.println("  Activity type: " + act.getType());
                            System.out.println("  Link: " + act.getLinkId());
                            System.out.println("  Start time: " + act.getStartTime());
                            System.out.println("  End time: " + act.getEndTime());
                        } else if (pe instanceof Leg) {
                            Leg leg = (Leg) pe;
                            System.out.println("  Leg mode: " + leg.getMode());

                            if (leg.getRoute() != null) {
                                // Stampa i link del percorso, se presente
                                if (leg.getRoute() instanceof NetworkRoute) {
                                    NetworkRoute nr = (NetworkRoute) leg.getRoute();
                                    System.out.println("  Route start: " + nr.getStartLinkId());
                                    System.out.println("  Route end: " + nr.getEndLinkId());
                                    System.out.println("  Route via links: " + nr.getLinkIds());
                                }
                            }
                        }
                    }
                    System.out.println("------------------------------------------------");
                }
            }
        }
        System.out.println("=== Fine Debug Popolazione ===");
        throw new RuntimeException("Debug Popolazione completato - interruzione esecuzione");
    }

    private void loadAndGenerateModels(Path csv, int count, double socMean, double socStdDev) {
        try {
            EvGenerator.loadCsv(csv);
            EvGenerator.setSeed(GENERATOR_SEED);
            List<EvModel> evModels = EvGenerator.generateEvModels(count, socMean, socStdDev);

            for (EvModel ev : evModels) {
                fleet.put(ev.getVehicleId(), ev);
            }
            System.out.println("[EvFleetManager] Flotta generata. Totale modelli: " + fleet.size());
        } catch (IOException e) {
            System.err.println("[EvFleetManager] Errore caricando CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void populateScenarioVehicles(Scenario scenario) {
        System.out.println("[EvFleetManager] Inserimento veicoli nello Scenario...");
        int registered = 0;
        Vehicles vehicles = scenario.getVehicles();
        for (EvModel ev : fleet.values()) {
            createMatsimEvVehicle(ev, vehicles);
            registered++;
        }
        System.out.println("[EvFleetManager] Inseriti " + registered + " veicoli nello scenario.");
    }

    /**
     * Crea e registra un veicolo MATSim (e il relativo VehicleType se mancante).
     */
    private Vehicle createMatsimEvVehicle(EvModel model, Vehicles vehicles) {
        Id<VehicleType> typeId = Id.create(model.getModel(), VehicleType.class);
        
        // Assicura che il tipo di veicolo esista
        VehicleType vehicleType = getOrCreateVehicleType(vehicles, typeId, model);

        // Crea il veicolo
        Vehicle vehicle = VehicleUtils.createVehicle(model.getVehicleId(), vehicleType);
        vehicle.getAttributes().putAttribute(ElectricFleetUtils.INITIAL_SOC, model.getCurrentSoc());

        vehicles.addVehicle(vehicle);
        return vehicle;
    }

    private VehicleType getOrCreateVehicleType(Vehicles vehicles, Id<VehicleType> typeId, EvModel model) {
        VehicleType existingType = vehicles.getVehicleTypes().get(typeId);
        if (existingType != null) {
            return existingType;
        }

        VehicleType newType = VehicleUtils.createVehicleType(typeId);
        newType.setMaximumVelocity(model.getTopSpeedKmh() * 3.6);

        EngineInformation engineInfo = newType.getEngineInformation();
        VehicleUtils.setHbefaTechnology(engineInfo, ElectricFleetUtils.EV_ENGINE_HBEFA_TECHNOLOGY);
        VehicleUtils.setEnergyCapacity(engineInfo, model.getNominalCapacityKwh());
        engineInfo.getAttributes().putAttribute(ElectricFleetUtils.CHARGER_TYPES, List.of(DEFAULT_CHARGER_TYPE));

        vehicles.addVehicleType(newType);
        return newType;
    }

    /**
     * Assegna un piano "dummy" a tutti i veicoli della flotta.
     */
    public void assignDummyPlansToFleet(Scenario scenario) {
        PopulationFactory factory = scenario.getPopulation().getFactory();
        for (EvModel evModel : fleet.values()) {
            createAndRegisterPerson(scenario, factory, evModel.getVehicleId());
        }
        System.out.println("[EvFleetManager] Piani dummy assegnati a " + fleet.size() + " veicoli.");
    }

    private void createAndRegisterPerson(
            Scenario scenario,
            PopulationFactory factory,
            Id<Vehicle> vehicleId
    ) {
        Person person = factory.createPerson(Id.createPersonId("person" + vehicleId));
        person.getAttributes().putAttribute("vehicle", vehicleId);
        Plan plan = factory.createPlan();

        // Trova i link più vicini guidabili da auto
        Link[] links = pickTwoRandomCarLinks(scenario);
        Link homeLink = links[0];
        Link workLink = links[1];

        System.out.println("Home link: " + homeLink.getId());
        System.out.println("Work link: " + workLink.getId());

        if (homeLink == null || workLink == null) {
            System.err.println("[WARN] Non trovato link car per home o work per veicolo " + vehicleId);
            throw new IllegalStateException("Link car non trovato per home o work");
        }

        // Home mattina
        Activity home = factory.createActivityFromLinkId("home", homeLink.getId());
        home.setEndTime(7 * 3600);
        plan.addActivity(home);

        // Leg to work
        Leg legToWork = factory.createLeg("car");
        plan.addLeg(legToWork);

        // Work
        Activity work = factory.createActivityFromLinkId("work", workLink.getId());
        work.setEndTime(16 * 3600);
        plan.addActivity(work);

        // Leg back home
        Leg legHome = factory.createLeg("car");
        plan.addLeg(legHome);

        // Home sera
        Activity homeEvening = factory.createActivityFromLinkId("home", homeLink.getId());
        plan.addActivity(homeEvening);

        person.addPlan(plan);
        scenario.getPopulation().addPerson(person);
    }

    /**
     * Restituisce due link distinti scelti casualmente dalla rete,
     * entrambi guidabili in auto.
     */
    public static Link[] pickTwoRandomCarLinks(Scenario scenario) {
        Network network = scenario.getNetwork();

        // Filtra solo link guidabili con 'car'
        List<Link> carLinks = network.getLinks().values().stream()
                .filter(link -> link.getAllowedModes().contains("car"))
                .collect(Collectors.toList());

        if (carLinks.size() < 2) {
            throw new RuntimeException("Non ci sono abbastanza link guidabili in auto nella rete!");
        }

        Random random = new Random();

        // Scegli due link distinti
        int idx1 = random.nextInt(carLinks.size());
        int idx2;
        do {
            idx2 = random.nextInt(carLinks.size());
        } while (idx2 == idx1);

        return new Link[]{carLinks.get(idx1), carLinks.get(idx2)};
    }


    // =========================================================================================
    // SECTION: Runtime & Simulation Update
    // =========================================================================================

    /**
     * Aggiorna lo stato (SOC, energia) leggendo l'ElectricFleet dal QSim.
     */
    public void updateSocFromQSim(org.matsim.core.mobsim.qsim.QSim qSim) {
        if (qSim == null) {
            System.err.println("[EvFleetManager] updateSocFromQSim: qSim == null, salto aggiornamento");
            return;
        }

        ElectricFleet electricFleet = getElectricFleetFromQSim(qSim);
        if (electricFleet == null) return;

        int updated = 0;
        int missing = 0;

        for (EvModel evModel : fleet.values()) {
            boolean success = updateSingleVehicleState(evModel, electricFleet);
            if (success) {
                updated++;
            } else {
                missing++;
            }
        }
        /*
        for (EvModel ev : fleet.values()) {
            System.out.println(ev.getVehicleId() + " ha percorso " + ev.getDistanceTraveledKm() + " km");
        }
         */
        
        System.out.printf("[EvFleetManager] updateSocFromQSim: aggiornati=%d, mancanti=%d%n", updated, missing);
    }

    private ElectricFleet getElectricFleetFromQSim(org.matsim.core.mobsim.qsim.QSim qSim) {
        try {
            return qSim.getChildInjector().getInstance(ElectricFleet.class);
        } catch (Exception e) {
            System.err.println("[EvFleetManager] Impossibile ottenere ElectricFleet dall'injector del QSim: " + e.getMessage());
            return null;
        }
    }

    private boolean updateSingleVehicleState(EvModel evModel, ElectricFleet electricFleet) {
        ElectricVehicle ev = electricFleet.getElectricVehicles().get(evModel.getVehicleId());
        if (ev != null && ev.getBattery() != null) {
            double soc = ev.getBattery().getSoc();
            double energyJ = ev.getBattery().getCharge();
            evModel.updateDynamicState(soc, energyJ);
            return true;
        }
        return false;
    }

    // =========================================================================================
    // SECTION: Event Handlers
    // =========================================================================================

    @Override
    public void handleEvent(ChargingStartEvent event) {
        handleChargingEvent(event.getVehicleId(), event.getTime(), true);
    }

    @Override
    public void handleEvent(ChargingEndEvent event) {
        handleChargingEvent(event.getVehicleId(), event.getTime(), false);
    }

    private void handleChargingEvent(Id<Vehicle> vid, double time, boolean isStarting) {
        EvModel ev = fleet.get(vid);
        if (ev != null) {
            ev.setCharging(isStarting);
            if (isStarting) {
                System.out.printf("[%.0f] Veicolo %s inizia ricarica%n", time, vid);
            } else {
                System.out.printf("[%.0f] Veicolo %s termina ricarica, SOC=%.3f%n", time, vid, ev.getCurrentSoc());
            }
        } else {
            String eventType = isStarting ? "ChargingStart" : "ChargingEnd";
            System.out.printf("[%.0f] %s per veicolo NON-registrato %s%n", time, eventType, vid);
        }
    }

    // =========================================================================================
    // SECTION: Getters & Statistics
    // =========================================================================================

    public Map<Id<Vehicle>, EvModel> getFleet() {
        return Collections.unmodifiableMap(fleet);
    }

    public EvModel getVehicle(Id<Vehicle> vehicleId) {
        return fleet.get(vehicleId);
    }

    public double calculateAverageSoc() {
        return fleet.values().stream()
                .mapToDouble(EvModel::getCurrentSoc)
                .average()
                .orElse(0.0);
    }

    public double calculateAverageDistanceByChargingStatus(boolean isCharging) {
        return fleet.values().stream()
                .filter(ev -> ev.isCharging() == isCharging)
                .mapToDouble(EvModel::getDistanceTraveledKm)
                .average()
                .orElse(0.0);
    }
}