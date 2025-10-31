package com.prtlabs.utils.exceptions;

public class PrtBaseException extends Exception
{

    public final String exceptionCode;    // Mandatory for all PRTLabs project

    public PrtBaseException(String exceptionCode, String message, Throwable cause) {
        super(message, cause);
        this.exceptionCode = exceptionCode;
    }

    public PrtBaseException(String exceptionCode, String message) {
        this(exceptionCode, message, null);
    }

    public PrtBaseException(String exceptionCode) {
        this(exceptionCode, exceptionCode, null);
    }

    @Override
    public String getMessage() {
        return String.format("[%s] -%s", this.exceptionCode, super.getMessage());
    }

}
