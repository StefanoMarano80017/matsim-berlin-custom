package org.matsim.CustomEvModule.Utils;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;

public class CoordinateConverter {

    private static MathTransform transform;

    static {
        try {
            // Sistema della rete MATSim (Berlino)
            CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:25832"); // metri
            CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");  // lat/lon WGS84
            transform = CRS.findMathTransform(sourceCRS, targetCRS, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Converte coordinate x/y della rete MATSim in lat/lon WGS84
     */
    public static double[] toLatLon(double x, double y) {
        try {
            // Coord di partenza
            Coordinate source = new Coordinate(x, y);

            // Coord di destinazione (trasformata)
            Coordinate target = new Coordinate();
            JTS.transform(source, target, transform);

            // Restituisci lat/lon
            return new double[]{target.x, target.y};
        } catch (Exception e) {
            e.printStackTrace();
            return new double[]{0, 0};
        }
    }
}
