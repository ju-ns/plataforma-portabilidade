package com.julia.portabilidade_api.services.exceptions;

public class InvalidInstallmentSelectionException extends RuntimeException{

    public InvalidInstallmentSelectionException(String msg){
        super(msg);
    }
}
