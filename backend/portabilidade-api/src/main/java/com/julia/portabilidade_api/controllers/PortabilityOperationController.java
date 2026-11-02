package com.julia.portabilidade_api.controllers;


import com.julia.portabilidade_api.dto.request.CreatePortabilityRequest;
import com.julia.portabilidade_api.dto.response.PortabilityResponse;
import com.julia.portabilidade_api.entities.PortabilityStatus;
import com.julia.portabilidade_api.services.PortabilityOperationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/portabilities")
@RequiredArgsConstructor
public class PortabilityOperationController {

    private final PortabilityOperationService service;

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

}
