package com.prtlabs.exceptions;

public class PrtBaseException extends Exception
{

    public final String exceptionCode;    // Mandatory for all PRTLabs project

    public PrtBaseException(String exceptionCode, String message, Throwable cause) {
        super(message, cause);
        this.exceptionCode = exceptionCode;
    }

    @Override
    public String getMessage() {
        return String.format("[%s] -%s", this.exceptionCode, super.getMessage());
    }

}
