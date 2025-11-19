package com.milton.agent.exceptions;


public class PromptLoaderException extends RuntimeException {

    public PromptLoaderException(String message) {
        super(message);
    }

    public PromptLoaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
