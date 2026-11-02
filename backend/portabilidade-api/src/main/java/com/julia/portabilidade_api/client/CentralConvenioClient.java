package com.julia.portabilidade_api.client;


import com.julia.portabilidade_api.dto.request.RegistrarAutorizacaoRequest;
import com.julia.portabilidade_api.dto.response.RegistrarAutorizacaoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class CentralConvenioClient {

    private final RestClient restClient;



    public RegistrarAutorizacaoResponse criarAutorizacao(RegistrarAutorizacaoRequest request){
        return restClient.post()
                .uri("/autorizacoes")
                .body(request)
                .retrieve()
                .body(RegistrarAutorizacaoResponse.class);
    }

}
