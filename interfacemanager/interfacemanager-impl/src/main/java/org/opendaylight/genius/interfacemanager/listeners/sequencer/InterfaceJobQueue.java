/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners.sequencer;

import java.util.Queue;
import java.util.Timer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class InterfaceJobQueue {
    private Queue<TaskEntry> taskQueue;
    private AtomicBoolean interfaceTimerFlag;
    private Timer interfaceTimer;

    public Queue<TaskEntry> getTaskQueue() {
        return taskQueue;
    }

    public void setTaskQueue(Queue<TaskEntry> taskQueue) {
        this.taskQueue = taskQueue;
    }

    public AtomicBoolean getInterfaceTimerFlag() {
        return interfaceTimerFlag;
    }

    public void setInterfaceTimerFlag(AtomicBoolean interfaceTimerFlag) {
        this.interfaceTimerFlag = interfaceTimerFlag;
    }

    public Timer getInterfaceTimer() {
        return interfaceTimer;
    }

    public void setInterfaceTimer(Timer interfaceTimer) {
        this.interfaceTimer = interfaceTimer;
    }

    public InterfaceJobQueue() {
        this.taskQueue = new ConcurrentLinkedQueue<>();
        this.interfaceTimerFlag = new AtomicBoolean(false);
        this.interfaceTimer = new Timer();
    }
}