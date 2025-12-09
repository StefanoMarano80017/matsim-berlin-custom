package org.springboot.controller;

import org.springboot.DTO.payload.HubStatusPayload;
import org.springboot.DTO.payload.VehicleStatus;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/ws-doc")
public class WebSocketDocController {
    
    @Operation(summary = "Descrizione messaggio WebSocket vehicle status")
    @GetMapping("vehicle-status-doc")
    public VehicleStatus getVehicleStatusDoc() {
        return null; // non viene usato realmente
    }

    @Operation(summary = "Descrizione messaggio WebSocket hub status")
    @GetMapping("vehicle-hub-doc")
    public HubStatusPayload gethubtatusDoc() {
        return null; // non viene usato realmente
    }


}
