package com.prtlabs.exceptions;

public class PrtTechnicalRuntimeException extends PrtBaseRuntimeException
{

    public PrtTechnicalRuntimeException(String exceptionCode, String message, Throwable cause) {
        super(exceptionCode, message, cause);
    }

}
