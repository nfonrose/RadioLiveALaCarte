package com.prtlabs.utils.exceptions;

public class PrtTechnicalRuntimeException extends PrtBaseRuntimeException
{

    public PrtTechnicalRuntimeException(String exceptionCode, String message, Throwable cause) {
        super(exceptionCode, message, cause);
    }

    public PrtTechnicalRuntimeException(String exceptionCode, String message) {
        this(exceptionCode, message, null);
    }

    public PrtTechnicalRuntimeException(String exceptionCode) {
        this(exceptionCode, exceptionCode, null);
    }

    @Override
    public String getMessage() {
        return String.format("[%s] -%s", this.exceptionCode, super.getMessage());
    }

}
