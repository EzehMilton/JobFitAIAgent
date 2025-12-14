package com.milton.agent.models;

/**
 * Interface for request objects that contain job description text.
 * Allows generic extraction actions to work with any request type.
 */
public interface JobDescriptionProvider {
    /**
     * Returns the job description text from this request.
     * @return the job description string
     */
    String getJobDescriptionText();
}
