package com.prtlabs.exceptions;

public class PrtBaseRuntimeException extends RuntimeException
{

    public final String exceptionCode;    // Mandatory for all PRTLabs project

    public PrtBaseRuntimeException(String exceptionCode, String message, Throwable cause) {
        super(message, cause);
        this.exceptionCode = exceptionCode;
    }

    @Override
    public String getMessage() {
        return String.format("[%s] -%s", this.exceptionCode, super.getMessage());
    }

}
