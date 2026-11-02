package com.julia.portabilidade_api.controllers;

import com.julia.portabilidade_api.dto.response.InstallmentResponse;
import com.julia.portabilidade_api.services.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final ScheduleService service;

    @GetMapping("/{beneficiaryId}/schedule")
    public ResponseEntity<List<InstallmentResponse>> getSchedule(
            @PathVariable("beneficiaryId") String beneficiaryId
    ) {
        return ResponseEntity.ok(service.getSchedule(beneficiaryId));
    }
}
