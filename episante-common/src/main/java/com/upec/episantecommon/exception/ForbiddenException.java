package com.upec.episantecommon.exception;

public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(message, "FORBIDDEN", 403);
    }
}
