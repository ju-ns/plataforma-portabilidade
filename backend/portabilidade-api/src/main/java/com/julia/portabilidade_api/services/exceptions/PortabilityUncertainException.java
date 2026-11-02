package com.julia.portabilidade_api.services.exceptions;

public class PortabilityUncertainException extends RuntimeException {

    public PortabilityUncertainException(String message) {
        super(message);
    }

    public PortabilityUncertainException(String message, Throwable cause) {
        super(message, cause);
    }
}
