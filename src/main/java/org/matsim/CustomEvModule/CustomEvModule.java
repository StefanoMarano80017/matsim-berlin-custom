package org.matsim.CustomEvModule;
/*
*  Standard libs
*/
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.charging.FastThenSlowCharging;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.vehicles.Vehicle;
/*
*  Custom Libs
*/
import org.matsim.ServerEvSetup.ConfigRun.ConfigRun;
import org.matsim.ServerEvSetup.SimulationInterface.SimulationBridgeInterface;
import org.matsim.CustomEvModule.Hub.HubManager;
import org.matsim.CustomEvModule.Hub.TargetSocChargingHandler;
import org.matsim.CustomEvModule.EVfleet.EvFleetManager;
import org.matsim.CustomEvModule.EVfleet.EnergyConsumption.EvConsumptionModelFactory;
import org.matsim.CustomEvModule.Monitoring.HubChargingMonitor;
import org.matsim.CustomEvModule.Monitoring.QuickLinkDebugHandler;
import org.matsim.CustomEvModule.Monitoring.TimeStepSocMonitor;
import org.matsim.CustomEvModule.Monitoring.VehicleStatusMonitor;
import org.matsim.CustomEvModule.RealTime.RealTimeConfigurer;

/*
*  GUICE
*/
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.util.Collections;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CustomEvModule extends AbstractModule {

    private static final Logger log = LogManager.getLogger(CustomEvModule.class);

    private final HubManager                          hubManager;
    private final EvFleetManager                      evFleetManager;
    private final SimulationBridgeInterface           bridge;
    private final ChargingInfrastructureSpecification infraSpec;

    private final ConfigRun config;
    private final boolean   debug_link;
    private final boolean   realtime;

    public CustomEvModule(
        SimulationBridgeInterface bridge,
        HubManager hubManager,
        EvFleetManager evFleetManager,
        ChargingInfrastructureSpecification infraSpec,
        ConfigRun config
    ) {
        this.bridge = bridge;
        this.hubManager = hubManager;
        this.evFleetManager = evFleetManager;
        this.infraSpec = infraSpec;
        this.config = config;
        this.debug_link = config.isDebugLink();
        this.realtime = config.isRealTime();
    }

    @Override
    public void install() {
        bind(ChargingInfrastructureSpecification.class).toInstance(infraSpec);
        /*
        *   Binding dei miei manager per gli altri moduli
        */
        bind(EvFleetManager.class).toInstance(evFleetManager);
        bind(HubManager.class).toInstance(hubManager);
        /*
        *   Monitor inizio e fine ricarica a un hub
        */
        HubChargingMonitor hubChargingMonitor = new HubChargingMonitor(bridge);
        addEventHandlerBinding().toInstance(hubChargingMonitor);
        /*
        *   Andamento Soc nel tempo, settare il timestep per aggiornamento in discreto 
        */
        TimeStepSocMonitor timeStepSocMonitor = new TimeStepSocMonitor(bridge, config.getStepSize());        
        addMobsimListenerBinding().toInstance(timeStepSocMonitor);
        /*
        *   Monitor dello stato del veicolo
        */
        VehicleStatusMonitor vehicleStatusMonitor = new VehicleStatusMonitor(bridge);
        addEventHandlerBinding().toInstance(vehicleStatusMonitor);
        /*
        *   Modello di consumo del SoC
        */
        bind(DriveEnergyConsumption.Factory.class).toProvider(new Provider<>() {
            @Inject private EvFleetManager providerEvFleetManager;
            @Override
            public DriveEnergyConsumption.Factory get() {
                return new EvConsumptionModelFactory(providerEvFleetManager);
            }
        }).asEagerSingleton();

        //a dummy factory, as aux consumption is part of the drive consumption in the model
        bind( AuxEnergyConsumption.Factory.class )
            .toInstance(electricVehicle -> ( beginTime, duration, linkId ) -> 0 ); 

        /*
        *   Handler della ricarica nel piano + strategia soc target
        */
        installQSimModule(new AbstractQSimModule() {
			@Override protected void configureQSim() {
				bind(TargetSocChargingHandler.class).in(Singleton.class);
				addMobsimScopeEventHandlerBinding().to( TargetSocChargingHandler.class);
			}
		});

        /*
        *  Modello di ricarica della charging station
        */
        bind(ChargingPower.Factory.class).toInstance(ev -> new FastThenSlowCharging(ev));

        /*
        *   Logger dei percorsi dei veicoli
        */
        if(debug_link) {
            Set<Id<Vehicle>> electricVehicleIds = Collections.unmodifiableSet(evFleetManager.getFleet().keySet());
            QuickLinkDebugHandler debugHandler = new QuickLinkDebugHandler(electricVehicleIds);
            addEventHandlerBinding().toInstance(debugHandler);
        }

        /*
        *   dilatazione dei tempi per real time
        */
        if(realtime){
            RealTimeConfigurer realTimeConfigurer = new RealTimeConfigurer();
            addMobsimListenerBinding().toInstance(realTimeConfigurer);
        }

        log.info("Modulo CustomEvModule installato con successo");
    }

}
