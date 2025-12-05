package com.upec.episantecommon.exception;

public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(message, "BAD_REQUEST", 400);
    }
}
