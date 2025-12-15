package org.springboot.SimulationBridge;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;


/*
*   Semplice event bus 
*   Lato Spring, sottoscrivi l’EventBus per ricevere gli eventi.
*   Lato MATSIM, pubblichi gli eventi sul bus.
*/
public class SimulationEventBus  {

    private static final SimulationEventBus INSTANCE = new SimulationEventBus();

    private final List<Consumer<Object>> listeners = new CopyOnWriteArrayList<>();


    public SimulationEventBus(){}

    public static SimulationEventBus getInstance(){
        return INSTANCE;
    }

    public void publish(Object event) {
        for (Consumer<Object> listener : listeners) {
            listener.accept(event);
        }
    }

    public void subscribe(Consumer<Object> listener) {
        listeners.add(listener);
    }
}
