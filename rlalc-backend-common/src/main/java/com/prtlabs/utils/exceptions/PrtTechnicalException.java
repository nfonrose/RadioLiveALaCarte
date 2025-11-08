package com.prtlabs.utils.exceptions;

public class PrtTechnicalException extends PrtBaseException
{

    public PrtTechnicalException(String exceptionCode, String message, Throwable cause) {
        super(exceptionCode, message, cause);
    }

    public PrtTechnicalException(String exceptionCode, String message) {
        this(exceptionCode, message, null);
    }

    public PrtTechnicalException(String exceptionCode) {
        this(exceptionCode, exceptionCode, null);
    }

    @Override
    public String getMessage() {
        return String.format("[%s] -%s", this.exceptionCode, super.getMessage());
    }

}
