package com.prtlabs.rlalc.backend.mediacapture.domain;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the status of a recording, including its current state,
 * any errors that occurred, and the list of generated chunks.
 */
public class RecordingStatus {

    /**
     * Possible states of a recording.
     */
    public enum Status {
        PENDING,
        ONGOING,
        COMPLETED,
        PARTIAL_FAILURE
    }

    private Status status;
    private List<String> errors;
    private List<File> chunkList;

    /**
     * Default constructor.
     */
    public RecordingStatus() {
        this.status = Status.PENDING;
        this.errors = new ArrayList<>();
        this.chunkList = new ArrayList<>();
    }

    /**
     * Constructor with initial status.
     *
     * @param status the initial status
     */
    public RecordingStatus(Status status) {
        this.status = status;
        this.errors = new ArrayList<>();
        this.chunkList = new ArrayList<>();
    }

    /**
     * Constructor with all fields.
     *
     * @param status    the status
     * @param errors    the list of errors
     * @param chunkList the list of chunks
     */
    public RecordingStatus(Status status, List<String> errors, List<File> chunkList) {
        this.status = status;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.chunkList = chunkList != null ? chunkList : new ArrayList<>();
    }

    /**
     * Gets the status.
     *
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status to set
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Gets the errors.
     *
     * @return the errors
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Sets the errors.
     *
     * @param errors the errors to set
     */
    public void setErrors(List<String> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    /**
     * Adds an error.
     *
     * @param error the error to add
     */
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }

    /**
     * Gets the chunk list.
     *
     * @return the chunk list
     */
    public List<File> getChunkList() {
        return chunkList;
    }

    /**
     * Sets the chunk list.
     *
     * @param chunkList the chunk list to set
     */
    public void setChunkList(List<File> chunkList) {
        this.chunkList = chunkList != null ? chunkList : new ArrayList<>();
    }
}