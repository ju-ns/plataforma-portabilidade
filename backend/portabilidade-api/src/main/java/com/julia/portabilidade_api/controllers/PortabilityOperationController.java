package com.julia.portabilidade_api.controllers;


import com.julia.portabilidade_api.dto.request.CreatePortabilityRequest;
import com.julia.portabilidade_api.dto.response.PortabilityResponse;
import com.julia.portabilidade_api.entities.PortabilityStatus;
import com.julia.portabilidade_api.services.PortabilityOperationService;
import com.julia.portabilidade_api.services.PortabilityStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/portabilities")
@RequiredArgsConstructor
public class PortabilityOperationController {

    private final PortabilityOperationService service;
    private final PortabilityStatusService statusService;

    @PostMapping
    public ResponseEntity<PortabilityResponse> create(
            @Valid @RequestBody CreatePortabilityRequest request
    ) {
        var portability = service.create(request);
        var response = new PortabilityResponse(portability);

        if (portability.getStatus() == PortabilityStatus.CRIADA
                || portability.getStatus() == PortabilityStatus.PENDENTE) {
            return ResponseEntity.accepted().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PortabilityResponse> getStatus(
            @PathVariable("id") UUID id
    ) {
        var portability = statusService.getStatus(id);
        var response = new PortabilityResponse(portability);

        if (portability.getStatus() == PortabilityStatus.PENDENTE
                || portability.getStatus() == PortabilityStatus.CRIADA) {
            return ResponseEntity.ok()
                    .header("Retry-After", "2")
                    .body(response);
        }

        return ResponseEntity.ok(response);
    }

}
