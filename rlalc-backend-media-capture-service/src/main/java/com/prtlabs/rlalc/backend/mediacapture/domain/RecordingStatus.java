package com.prtlabs.rlalc.backend.mediacapture.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the status of a recording, including its current state,
 * any errors that occurred, and the list of generated chunks.
 */
@Getter
@AllArgsConstructor
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

}