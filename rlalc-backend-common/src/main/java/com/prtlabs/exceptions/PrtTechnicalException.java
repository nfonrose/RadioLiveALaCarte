package com.prtlabs.exceptions;

public class PrtTechnicalException extends PrtBaseException
{

    public PrtTechnicalException(String exceptionCode, String message, Throwable cause) {
        super(exceptionCode, message, cause);
    }

}
