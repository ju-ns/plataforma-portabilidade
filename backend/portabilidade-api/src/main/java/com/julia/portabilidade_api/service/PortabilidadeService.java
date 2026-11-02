package com.julia.portabilidade_api.service;

import com.julia.portabilidade_api.client.CentralConvenioClient;
import com.julia.portabilidade_api.dto.request.RegistrarAutorizacaoRequest;
import com.julia.portabilidade_api.dto.response.RegistrarAutorizacaoResponse;
import com.julia.portabilidade_api.entity.Portabilidade;
import com.julia.portabilidade_api.entity.StatusPortabilidade;
import com.julia.portabilidade_api.repository.PortabilidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PortabilidadeService {

    private final PortabilidadeRepository repository;
    private final CentralConvenioClient convenioClient;

    public RegistrarAutorizacaoResponse registrar(RegistrarAutorizacaoRequest request){
        RegistrarAutorizacaoResponse response = convenioClient.criarAutorizacao(request);
        Portabilidade portabilidade = Portabilidade.builder()
                .beneficiarioId(request.beneficiarioId())
                .autorizacaoId(response.autorizacaoId())
                .autorizadaEm(LocalDateTime.now())
                .status(StatusPortabilidade.AUTORIZADA)
                .build();

        repository.save(portabilidade);
        return response;
    }


}
