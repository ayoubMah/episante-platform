package com.upec.episantecommon.exception;

public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(message, "NOT_FOUND", 404);
    }
}
