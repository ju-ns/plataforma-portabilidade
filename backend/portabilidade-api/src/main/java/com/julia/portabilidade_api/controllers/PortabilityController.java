package com.julia.portabilidade_api.controllers;

import com.julia.portabilidade_api.dto.request.RegisterAuthorizationRequest;
import com.julia.portabilidade_api.dto.response.AuthorizationResponse;
import com.julia.portabilidade_api.entities.PortabilityStatus;
import com.julia.portabilidade_api.services.PortabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/authorizations")
@RequiredArgsConstructor
public class PortabilityController {

    private final PortabilityService service;

    @PostMapping
    public ResponseEntity<AuthorizationResponse> registerAuthorization(
            @Valid @RequestBody RegisterAuthorizationRequest request)
    {
        var result = service.registerAuthorization(request);
        var portability = result.portability();
        var response = new AuthorizationResponse(portability);

        HttpStatus status;

        if (result.created()) {
            status = HttpStatus.CREATED;
        } else if (portability.getStatus() == PortabilityStatus.CRIADA) {
            status = HttpStatus.ACCEPTED;
        } else {
            status = HttpStatus.OK;
        }
        return ResponseEntity.status(status).body(response);
    }
}
