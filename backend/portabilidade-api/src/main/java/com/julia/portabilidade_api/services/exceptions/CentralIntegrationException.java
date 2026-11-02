package com.julia.portabilidade_api.services.exceptions;

public class CentralIntegrationException extends RuntimeException{
    public CentralIntegrationException(String msg, Throwable cause){
        super(msg, cause);
    }
}
