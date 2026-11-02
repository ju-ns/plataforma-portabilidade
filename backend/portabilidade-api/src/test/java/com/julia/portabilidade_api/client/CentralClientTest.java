package com.julia.portabilidade_api.client;

import com.julia.portabilidade_api.dto.request.CreatePortabilityRequest;
import com.julia.portabilidade_api.dto.request.RegisterAuthorizationRequest;
import com.julia.portabilidade_api.entities.PortabilityStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

public class CentralClientTest {

    private MockRestServiceServer server;
    private CentralClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder()
                .baseUrl("http://central.test");

        server = MockRestServiceServer.bindTo(builder).build();
        client = new CentralClient(builder.build());
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void shouldRegisterAuthorizationAndMapAuthorizationId() {
        server.expect(requestTo("http://central.test/autorizacoes"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "beneficiaryId": "BEN001",
                          "idempotencyKey": "auth-001"
                        }
                        """))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "autorizacaoId": "AUTH001"
                                }
                                """));

        var response = client.registerAuthorization(
                new RegisterAuthorizationRequest("BEN001", "auth-001")
        );
    }

    @Test
    void shouldMapScheduleFieldsReturnedByCentral() {
        server.expect(requestTo(
                        "http://central.test/beneficiarios/BEN001/agenda"
                ))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {
                            "parcelaId": "P001",
                            "convenioId": "CONV01",
                            "valor": 150.00,
                            "dataDesconto": "2026-01-10",
                            "status": "ABERTA"
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        var result = client.getSchedule("BEN001");

        assertEquals(1, result.size());

        var installment = result.getFirst();

        assertEquals("P001", installment.installmentId());
        assertEquals("CONV01", installment.agreementId());
        assertEquals(
                0,
                new BigDecimal("150.00").compareTo(installment.amount())
        );
        assertEquals(
                LocalDate.of(2026, 1, 10),
                installment.deductionDate()
        );
        assertEquals("ABERTA", installment.status());
    }

    @Test
    void shouldAcceptEmptyScheduleReturnedByCentral() {
        server.expect(requestTo(
                        "http://central.test/beneficiarios/BEN_ERRO/agenda"
                ))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        var result = client.getSchedule("BEN_ERRO");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldSendPortabilityAndMapProtocol() {
        server.expect(requestTo(
                        "http://central.test/operacoes-portabilidade"
                ))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "beneficiarioId": "BEN001",
                          "autorizacaoId": "AUTH001",
                          "parcelaIds": ["P001"],
                          "contaDestino": {
                            "banco": "001",
                            "agencia": "1234",
                            "conta": "123456-7"
                          }
                        }
                        """))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "protocolo": "PROTOCOL001"
                                }
                                """));

        var response = client.createPortability(
                portabilityRequest("BEN001"),
                "AUTH001"
        );

        assertEquals("PROTOCOL001", response.protocol());
    }

    @Test
    void shouldPropagate503ReturnedByCentral() {
        server.expect(requestTo(
                        "http://central.test/operacoes-portabilidade"
                ))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.beneficiarioId").value("BEN_ERRO"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "erro": "Central indisponível no momento (simulado). Tente novamente."
                                }
                                """));

        var exception = assertThrows(
                HttpServerErrorException.ServiceUnavailable.class,
                () -> client.createPortability(
                        portabilityRequest("BEN_ERRO"),
                        "AUTH001"
                )
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatusCode()
        );
    }

    @Test
    void shouldInterpret204AsPending() {
        server.expect(requestTo(
                        "http://central.test/operacoes-portabilidade/PROTOCOL001"
                ))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NO_CONTENT)
                        .header("Retry-After", "2"));

        var response = client.getPortabilityStatus("PROTOCOL001");

        assertEquals(PortabilityStatus.PENDENTE, response.status());
        assertNull(response.rejectionReason());
    }

    @Test
    void shouldMapConfirmedStatus() {
        server.expect(requestTo(
                        "http://central.test/operacoes-portabilidade/PROTOCOL001"
                ))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "status": "CONFIRMADA",
                          "motivoRejeicao": null
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.getPortabilityStatus("PROTOCOL001");

        assertEquals(PortabilityStatus.CONFIRMADA, response.status());
        assertNull(response.rejectionReason());
    }

    @Test
    void shouldMapRejectedStatusAndReason() {
        server.expect(requestTo(
                        "http://central.test/operacoes-portabilidade/PROTOCOL001"
                ))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "status": "REJEITADA",
                          "motivoRejeicao": "Beneficiário atingiu o limite de margem consignável"
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.getPortabilityStatus("PROTOCOL001");

        assertEquals(PortabilityStatus.REJEITADA, response.status());
        assertEquals(
                "Beneficiário atingiu o limite de margem consignável",
                response.rejectionReason()
        );
    }

    private CreatePortabilityRequest portabilityRequest(
            String beneficiaryId
    ) {
        return new CreatePortabilityRequest(
                beneficiaryId,
                List.of("P001"),
                "001",
                "1234",
                "123456-7",
                "port-001"
        );
    }



}
