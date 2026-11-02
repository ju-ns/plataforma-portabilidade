package com.julia.portabilidade_api.client;


import com.julia.portabilidade_api.dto.request.RegisterAuthorizationRequest;
import com.julia.portabilidade_api.dto.response.RegisterAuthorizationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

}
