package com.julia.portabilidade_api.controllers.handlers;

import com.julia.portabilidade_api.dto.errors.CustomError;
import com.julia.portabilidade_api.dto.errors.ValidationError;
import com.julia.portabilidade_api.services.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<CustomError> duplicateException(DuplicateException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT;
        CustomError err = new CustomError(
                Instant.now(),
                status.value(),
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(InvalidCentralResponseException.class)
    public ResponseEntity<CustomError> invalidCentralResponse(InvalidCentralResponseException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_GATEWAY;
        CustomError err = new CustomError(
                Instant.now(),
                status.value(),
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(ResourceNotFound.class)
    public ResponseEntity<CustomError> resourceNotFound(ResourceNotFound e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        CustomError err = new CustomError(
                Instant.now(),
                status.value(),
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomError> methodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request){
        HttpStatus status = HttpStatus.UNPROCESSABLE_CONTENT;
        ValidationError err = new ValidationError(
                Instant.now(),
                status.value(),
                "Dados inválidos",
                request.getRequestURI()
        );
        for(FieldError f: e.getBindingResult().getFieldErrors()){
            err.addError(f.getField(), f.getDefaultMessage());
        }
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(AuthorizationUncertainException.class)
    public ResponseEntity<CustomError> authorizationUncertain(AuthorizationUncertainException exception,HttpServletRequest request){
        var error = new CustomError(
                Instant.now(),
                HttpStatus.BAD_GATEWAY.value(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    @ExceptionHandler(BeneficiaryNotAuthorizedException.class)
    public ResponseEntity<CustomError> beneficiaryNotAuthorized(
            BeneficiaryNotAuthorizedException exception,
            HttpServletRequest request
    ) {
        var error = new CustomError(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(CentralIntegrationException.class)
    public ResponseEntity<CustomError> centralIntegration(
            CentralIntegrationException exception,
            HttpServletRequest request
    ) {
        var error = new CustomError(
                Instant.now(),
                HttpStatus.BAD_GATEWAY.value(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    @ExceptionHandler(InvalidInstallmentSelectionException.class)
    public ResponseEntity<CustomError> invalidInstallmentSelection(
            InvalidInstallmentSelectionException exception,
            HttpServletRequest request
    ) {
        var error = new CustomError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.unprocessableContent().body(error);
    }

    @ExceptionHandler(PortabilityUncertainException.class)
    public ResponseEntity<CustomError> portabilityUncertain(
            PortabilityUncertainException exception,
            HttpServletRequest request
    ) {
        var error = new CustomError(
                Instant.now(),
                HttpStatus.BAD_GATEWAY.value(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    @ExceptionHandler(CentralUnavailableException.class)
    public ResponseEntity<CustomError> centralUnavailable(
            CentralUnavailableException exception,
            HttpServletRequest request
    ) {
        var error = new CustomError(
                Instant.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error);
    }




}
