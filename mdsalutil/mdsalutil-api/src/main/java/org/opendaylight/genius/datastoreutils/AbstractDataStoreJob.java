/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.Callable;

public abstract class AbstractDataStoreJob extends Observable implements Callable<List<ListenableFuture<Void>>> {

    public enum State {
        CREATED,
        RUNNING,
        SUCCESSFUL,
        FAILED
    }

    State state;

    public boolean isFinished() {
        return state == State.SUCCESSFUL || state == State.FAILED;
    }

    public boolean isSuccessful() {
        return state == State.SUCCESSFUL;
    }

    public boolean hasFailed() {
        return state == State.FAILED;
    }

    /**
     * Returns the key that DataStoreJobCoordinator uses for classifying jobs
     * that must be executed in sequence
     *
     * @return the key for this job
     */
    public abstract String getJobQueueKey();


    /**
     * Checks if the Job is apt for execution
     *
     * @throws InvalidJobException if the Job does not fulfill all conditions
     *         that must be met for execution
     */
    public abstract void validate() throws InvalidJobException;
}
