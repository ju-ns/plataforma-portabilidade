package com.julia.portabilidade_api.services.exceptions;

public class BeneficiaryNotAuthorizedException extends RuntimeException {

    public BeneficiaryNotAuthorizedException(String msg){
        super(msg);
    }
}
