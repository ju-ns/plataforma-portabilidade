package com.julia.portabilidade_api.services.exceptions;

public class AuthorizationUncertainException extends RuntimeException{
    public AuthorizationUncertainException(String msg){
        super(msg);
    }

    public AuthorizationUncertainException(
            String msg,
            Throwable cause
    ) {
        super(msg, cause);
    }
}
