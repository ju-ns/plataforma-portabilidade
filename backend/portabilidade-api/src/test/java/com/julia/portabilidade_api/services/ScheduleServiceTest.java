package com.julia.portabilidade_api.services;


import com.julia.portabilidade_api.client.CentralClient;
import com.julia.portabilidade_api.dto.response.InstallmentResponse;
import com.julia.portabilidade_api.factory.InstallmentFactory;
import com.julia.portabilidade_api.repositories.PortabilityRepository;
import com.julia.portabilidade_api.services.exceptions.BeneficiaryNotAuthorizedException;
import com.julia.portabilidade_api.services.exceptions.CentralIntegrationException;
import com.julia.portabilidade_api.services.exceptions.ResourceNotFound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ScheduleServiceTest {

    @Mock
    private PortabilityRepository portabilityRepository;

    @Mock
    private CentralClient centralClient;

    @InjectMocks
    private ScheduleService scheduleService;

    private String beneficiaryId;
    private String beneficiaryNotAuthorizedId;
    private String beneficiaryWithNoScheduleId;
    private InstallmentResponse january;
    private InstallmentResponse february;



    @BeforeEach
    void setUp() throws Exception {
        beneficiaryId = "BEN001";
        beneficiaryNotAuthorizedId = "BEN002";
        beneficiaryWithNoScheduleId = "BEN_ERRO";
        january = InstallmentFactory.installment("P001", LocalDate.of(2026, 1, 10));
        february= InstallmentFactory.installment("P002", LocalDate.of(2026, 2, 10));
    }

    @Test
    public void shouldReturnScheduleOrderedByDateWithoutFilteringDates(){
        when(portabilityRepository.existsByBeneficiaryIdAndAuthorizationIdIsNotNull(beneficiaryId))
                .thenReturn(true);
        when(centralClient.getSchedule(beneficiaryId))
                .thenReturn(List.of(february, january));

        var result = scheduleService.getSchedule(beneficiaryId);
        assertEquals(List.of(january, february), result);
        verify(centralClient).getSchedule(beneficiaryId);
    }

    @Test
    public void shouldNotCallCentralWhenBeneficiaryIsNotAuthorized(){
        when(portabilityRepository.existsByBeneficiaryIdAndAuthorizationIdIsNotNull(beneficiaryNotAuthorizedId))
                .thenReturn(false);
        assertThrows(BeneficiaryNotAuthorizedException.class,
                () -> scheduleService.getSchedule(beneficiaryNotAuthorizedId));
    }

    @Test
    public void shouldThrowResourceNotFoundWhenScheduleIsEmpty(){
        when(centralClient.getSchedule(beneficiaryWithNoScheduleId))
                .thenReturn(List.of());
        when(portabilityRepository.existsByBeneficiaryIdAndAuthorizationIdIsNotNull(beneficiaryWithNoScheduleId))
                .thenReturn(true);

        var exception = assertThrows(ResourceNotFound.class,
                () -> scheduleService.getSchedule(beneficiaryWithNoScheduleId));
        assertEquals("Nenhum agendamento de desconto encontrado para o beneficiário "
                        + beneficiaryWithNoScheduleId + ".",
                exception.getMessage()
        );

        verify(centralClient).getSchedule(beneficiaryWithNoScheduleId);
    }

    @Test
    public void shouldPropagateCentralCommunicationFailure(){
        var failure = new CentralIntegrationException("Não foi possível consultar a agenda na Central.",
                new ResourceAccessException("Connection refused")
        );
        when(portabilityRepository.existsByBeneficiaryIdAndAuthorizationIdIsNotNull(beneficiaryId))
                .thenReturn(true);
        when(centralClient.getSchedule(beneficiaryId)).thenThrow(failure);

        var exception = assertThrows(CentralIntegrationException.class,
                () -> scheduleService.getSchedule(beneficiaryId));
        assertSame(failure, exception);

    }
}
