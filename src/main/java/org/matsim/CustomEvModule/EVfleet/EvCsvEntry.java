package org.matsim.CustomEvModule.EVfleet;

public record EvCsvEntry(
        String brand,
        String model,
        double topSpeedKmh,
        double batteryCapacityKWh,
        String batteryType,
        int numberOfCells,
        double torqueNm,
        double efficiencyWhPerKm,
        double rangeKm,
        double acceleration0To100,
        double fastChargingPowerKwDc,
        String fastChargePort,
        double towingCapacityKg,
        double cargoVolumeL,
        int seats,
        String drivetrain,
        String segment,
        int lengthMm,
        int widthMm,
        int heightMm,
        String carBodyType
) {}
