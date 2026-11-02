package com.julia.portabilidade_api.services.exceptions;

public class DuplicateException extends RuntimeException{
    public DuplicateException(String msg){
        super(msg);
    }
}
