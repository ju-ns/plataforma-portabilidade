package com.julia.portabilidade_api.client;


import com.julia.portabilidade_api.dto.request.RegisterAuthorizationRequest;
import com.julia.portabilidade_api.dto.response.InstallmentResponse;
import com.julia.portabilidade_api.dto.response.RegisterAuthorizationResponse;
import com.julia.portabilidade_api.services.exceptions.CentralIntegrationException;
import com.julia.portabilidade_api.services.exceptions.InvalidCentralResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CentralClient {

    private final RestClient restClient;

    public RegisterAuthorizationResponse registerAuthorization(RegisterAuthorizationRequest request){
        return restClient.post()
                .uri("/autorizacoes")
                .body(request)
                .retrieve()
                .body(RegisterAuthorizationResponse.class);
    }

    public List<InstallmentResponse> getSchedule(String beneficiaryId) {
        try {
            var installments = restClient.get()
                    .uri("/beneficiarios/{beneficiaryId}/agenda", beneficiaryId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<InstallmentResponse>>() {});

            if (installments == null) {
                throw new InvalidCentralResponseException(
                        "A Central retornou uma agenda inválida."
                );
            }

            return installments;

        } catch (RestClientException exception) {
            throw new CentralIntegrationException(
                    "Não foi possível consultar a agenda na Central.",
                    exception
            );
        }
    }

}
