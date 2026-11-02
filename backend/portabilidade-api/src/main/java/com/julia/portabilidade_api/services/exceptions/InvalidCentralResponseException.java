package com.julia.portabilidade_api.services.exceptions;

public class InvalidCentralResponseException extends RuntimeException{
    public InvalidCentralResponseException(String msg){
        super(msg);
    }
}
