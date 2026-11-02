package com.julia.portabilidade_api.services.exceptions;

public class CentralUnavailableException extends RuntimeException{
    public CentralUnavailableException(String msg,  Throwable cause){
        super(msg, cause);
    }
}
