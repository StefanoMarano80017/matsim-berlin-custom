package org.springboot.DTO.out.SimulationDTO.mapper;

import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.springboot.DTO.out.SimulationDTO.EvDto;

public class EvMapper {

    public static EvDto toDto(EvModel model) {
        EvDto dto = new EvDto();

        dto.setVehicleId(model.getVehicleId().toString());
        dto.setManufacturer(model.getManufacturer());
        dto.setModel(model.getModel());
        dto.setNominalCapacityKwh(model.getNominalCapacityKwh());
        dto.setConsumptionKwhPerKm(model.getConsumptionKwhPerKm());
        dto.setBatteryType(model.getBatteryType());
        dto.setNumberOfCells(model.getNumberOfCells());
        dto.setTorqueNm(model.getTorqueNm());
        dto.setTopSpeedKmh(model.getTopSpeedKmh());
        dto.setRangeKm(model.getRangeKm());
        dto.setAcceleration0To100(model.getAcceleration0To100());
        dto.setFastChargingPowerKwDc(model.getFastChargingPowerKwDc());
        dto.setFastChargePort(model.getFastChargePort());
        dto.setTowingCapacityKg(model.getTowingCapacityKg());
        dto.setCargoVolumeL(model.getCargoVolumeL());
        dto.setSeats(model.getSeats());
        dto.setDrivetrain(model.getDrivetrain());
        dto.setSegment(model.getSegment());
        dto.setLengthMm(model.getLengthMm());
        dto.setWidthMm(model.getWidthMm());
        dto.setHeightMm(model.getHeightMm());
        dto.setCarBodyType(model.getCarBodyType());

        dto.setCurrentSoc(model.getCurrentSoc());
        dto.setCurrentEnergyJoules(model.getCurrentEnergyJoules());
        dto.setDistanceTraveledKm(model.getDistanceTraveledKm());
        dto.setState(model.getState().name());
        dto.setLinkId(model.getLinkId());

        return dto;
    }
}
