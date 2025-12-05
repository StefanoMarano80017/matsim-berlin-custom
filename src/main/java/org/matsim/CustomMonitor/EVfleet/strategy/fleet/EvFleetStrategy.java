package org.matsim.CustomMonitor.EVfleet.strategy.fleet;

import org.matsim.CustomMonitor.model.EvModel;

import java.nio.file.Path;
import java.util.List;

public interface EvFleetStrategy {

    List<EvModel> generateFleet(
                            Path csvPath, 
                            int n_veic, 
                            double socMean, 
                            double socStdDev
    );
}
