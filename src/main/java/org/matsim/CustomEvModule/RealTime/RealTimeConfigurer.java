package org.matsim.CustomEvModule.RealTime;

import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;

public class RealTimeConfigurer implements MobsimInitializedListener, MobsimBeforeSimStepListener{

    private long realStartMillis = -1;

    @SuppressWarnings("rawtypes")
    @Override
    public void notifyMobsimInitialized(MobsimInitializedEvent event) {
        realStartMillis = System.currentTimeMillis();
        System.out.println("QSim started – real-time pacing enabled");
    }

    /*
    * Soft real time, possiamo dire è time-paced
    */
    @SuppressWarnings("rawtypes")
    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {

        if (realStartMillis < 0) return; // QSim not started yet

        double simTime = e.getSimulationTime(); // seconds
        long targetRealTime = realStartMillis + (long) (simTime * 1000);
        long now = System.currentTimeMillis();
        long sleep = targetRealTime - now;

        if (sleep > 0) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException event) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
}
