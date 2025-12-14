package com.milton.agent.models;

/**
 * Interface for request objects that contain CV text.
 * Allows generic extraction actions to work with any request type.
 */
public interface CvTextProvider {
    /**
     * Returns the CV text from this request.
     * @return the CV text string
     */
    String getCvText();
}
