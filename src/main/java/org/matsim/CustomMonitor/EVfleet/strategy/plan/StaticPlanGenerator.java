package org.matsim.CustomMonitor.EVfleet.strategy.plan;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;
import java.util.stream.Collectors;

public class StaticPlanGenerator implements PlanGenerationStrategy {

    @Override
    public void generatePlanForVehicle(Id<Vehicle> vehicleId, Scenario scenario) {

        PopulationFactory factory = scenario.getPopulation().getFactory();

        Person p = factory.createPerson(Id.createPersonId(vehicleId));
        p.getAttributes().putAttribute("subpopulation", "person");

        Map<String, Id<Vehicle>> modeMap = new HashMap<>();
        modeMap.put("car", vehicleId);
        VehicleUtils.insertVehicleIdsIntoPersonAttributes(p, modeMap);

        Plan plan = factory.createPlan();

        Link[] links = pickTwoRandomCarLinks(scenario);
        Link homeLink = links[0];
        Link workLink = links[1];

        Id<Link> chargingLink = Id.createLinkId("4372494");

        Activity home = factory.createActivityFromLinkId("home", homeLink.getId());
        home.setEndTime(7 * 3600);
        plan.addActivity(home);

        plan.addLeg(factory.createLeg("car"));

        Activity work = factory.createActivityFromLinkId("work", workLink.getId());
        work.setEndTime(10 * 3600);
        plan.addActivity(work);

        plan.addLeg(factory.createLeg("car"));

        Activity charge = factory.createActivityFromLinkId("charging", chargingLink);
        charge.setEndTime(13 * 3600);
        plan.addActivity(charge);

        plan.addLeg(factory.createLeg("car"));

        plan.addActivity(factory.createActivityFromLinkId("home", homeLink.getId()));

        p.addPlan(plan);
        scenario.getPopulation().addPerson(p);
    }

    private Link[] pickTwoRandomCarLinks(Scenario scenario) {
        Network network = scenario.getNetwork();
        List<Link> carLinks = network.getLinks()
                                    .values()
                                    .stream()
                                    .filter(link -> link.getAllowedModes().contains("car"))
                                    .collect(Collectors.toList());

        Link home = carLinks.get(0);
        Link work = carLinks.get(Math.min(50, carLinks.size() - 1));
        return new Link[]{home, work};
    }
}
