package com.authshield360.common;

import java.util.Map;

/** Business rule violation carrying an {@link ErrorCode} and optional field errors. */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Map<String, String> fields;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.message(), null);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BusinessException(ErrorCode errorCode, String message, Map<String, String> fields) {
        super(message);
        this.errorCode = errorCode;
        this.fields = fields;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public Map<String, String> getFields() { return fields; }
}
