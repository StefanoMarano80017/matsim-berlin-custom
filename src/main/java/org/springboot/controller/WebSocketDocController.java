package org.springboot.controller;

import org.springboot.DTO.out.WebSocketDTO.payload.HubStatusPayload;
import org.springboot.DTO.out.WebSocketDTO.payload.TimeStepPayload;
import org.springboot.DTO.out.WebSocketDTO.payload.VehicleStatus;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/ws-doc")
public class WebSocketDocController {
    
    @Operation(summary = "Descrizione messaggio WebSocket vehicle status")
    @GetMapping("vehicle-doc")
    public VehicleStatus getVehicleStatusDoc() {
        return null; // non viene usato realmente
    }

    @Operation(summary = "Descrizione messaggio WebSocket hub status")
    @GetMapping("hub-doc")
    public HubStatusPayload gethubtatusDoc() {
        return null; // non viene usato realmente
    }

    @Operation(summary = "Descrizione messaggio WebSocket status ad ogni timestep")
    @GetMapping("status-doc")
    public TimeStepPayload getStatusDoc() {
        return null; // non viene usato realmente
    }

}
