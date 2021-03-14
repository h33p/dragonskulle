/* (C) 2021 DragonSkulle */
package org.dragonskulle.exceptions;

/** @author Oscar L This exception is thrown when any error occurs during decoding from a payload */
public class DecodingException extends Exception {
    public DecodingException(String errorMessage) {
        super(errorMessage);
    }
}
