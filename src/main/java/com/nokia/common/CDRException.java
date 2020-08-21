package com.nokia.common;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CDRException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private ErrorCode errorCode;

    public CDRException(ErrorCode errorCode, String errorMessage) {
        super(errorCode.toString() + " - " + errorMessage);
        this.errorCode = errorCode;
    }

    private CDRException(ErrorCode errorCode, String errorMessage, Throwable cause) {
        super(errorCode.toString() + " - " + getMessage(errorMessage) + " - " + getMessage(cause), cause);

        this.errorCode = errorCode;
    }

    public static CDRException asCDRXException(ErrorCode errorCode, String message) {
        return new CDRException(errorCode, message);
    }

    public static CDRException asCDRXException(ErrorCode errorCode, String message, Throwable cause) {
        if (cause instanceof CDRException) {
            return (CDRException) cause;
        }
        return new CDRException(errorCode, message, cause);
    }

    public static CDRException asCDRXException(ErrorCode errorCode, Throwable cause) {
        if (cause instanceof CDRException) {
            return (CDRException) cause;
        }
        return new CDRException(errorCode, getMessage(cause), cause);
    }

    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    private static String getMessage(Object obj) {
        if (obj == null) {
            return "";
        }

        if (obj instanceof Throwable) {
            StringWriter str = new StringWriter();
            PrintWriter pw = new PrintWriter(str);
            ((Throwable) obj).printStackTrace(pw);
            return str.toString();
            // return ((Throwable) obj).getMessage();
        } else {
            return obj.toString();
        }
    }
}
