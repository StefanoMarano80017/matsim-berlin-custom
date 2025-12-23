package org.matsim.CustomEvModule.EVfleet.strategy.fleet;

import org.matsim.CustomEvModule.model.EvModel;
import org.springframework.core.io.Resource;

import java.util.List;

public interface EvFleetStrategy {

    List<EvModel> generateFleet(
                            Resource csvPath, 
                            int n_veic, 
                            double socMean, 
                            double socStdDev
    );
}
