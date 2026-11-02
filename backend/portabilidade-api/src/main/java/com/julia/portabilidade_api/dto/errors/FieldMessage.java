package com.julia.portabilidade_api.dto.errors;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FieldMessage {

    private String fieldName;
    private String message;

}
