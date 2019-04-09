/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners.sequencer;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.infrautils.utils.concurrent.KeyedLocks;
import org.opendaylight.infrautils.utils.concurrent.NamedLocks;
import org.opendaylight.infrautils.utils.concurrent.NamedSimpleReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterfaceInventoryStateTaskSequencer implements SequencerTask {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceInventoryStateTaskSequencer.class);
    private final Map<String, InterfaceJobQueue> jobQueueMap = new ConcurrentHashMap<>();
    private ExecutorService executorService;
    private final JobCoordinator coordinator;
    // 10 seconds delay
    private static final long INTERFACE_MIGRATION_TIME_IN_MILLISECS = 10000;
    private static final long LOCK_WAIT_TIME_IN_MILLISECS = 50;
    private final SleepTask sleepTask = new SleepTask();
    private final NamedLocks<String> interfaceLock = new NamedLocks<>();

    public SleepTask getSleepTask() {
        return sleepTask;
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public InterfaceInventoryStateTaskSequencer(final JobCoordinator coordinator) {
        executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setNameFormat("InterfaceInventorySequencer-%d").setDaemon(true)
                .setUncaughtExceptionHandler((thread, ex) -> LOG.error("Uncaught exception {}", thread, ex))
                .build());
        executorService.submit(new SequencerHandler());
        this.coordinator = coordinator;
    }

    public Map<String, InterfaceJobQueue> getJobQueueMap() {
        return jobQueueMap;
    }


    private class InterfaceTimerTask extends TimerTask {
        String interfaceName;

        InterfaceTimerTask(String interfaceName) {
            this.interfaceName = interfaceName;
        }

        @Override
        public void run() {
            boolean timerCleared = false;
            while (!timerCleared) {
                NamedSimpleReentrantLock.AcquireResult lock = null;
                try {
                    lock = interfaceLock.tryAcquire(interfaceName, LOCK_WAIT_TIME_IN_MILLISECS,
                            TimeUnit.MILLISECONDS);
                    if (!lock.wasAcquired()) {
                        LOG.error("Unable to acquire lock for interface {} waiting {}ms timertask, retrying",
                                interfaceName, LOCK_WAIT_TIME_IN_MILLISECS);
                        continue;
                    }
                    InterfaceJobQueue interfaceQueue = jobQueueMap.get(interfaceName);
                    interfaceQueue.getInterfaceTimerFlag().set(false);
                    timerCleared = true;
                    LOG.info("Timer Cleared for interface {}", interfaceName);
                } finally {
                    if (lock.wasAcquired()) {
                        lock.close();
                    }
                }
            }
        }
    }


    private class SequencerHandler implements Runnable {
        @Override
        @SuppressWarnings("checkstyle:IllegalCatch")
        @SuppressFBWarnings({"UW_UNCOND_WAIT", "RV_RETURN_VALUE_IGNORED"})
        public void run() {
            while (true) {
                for (String infName : jobQueueMap.keySet()) {
                    NamedSimpleReentrantLock.AcquireResult lock = null;
                    try {
                        lock = interfaceLock.tryAcquire(infName, LOCK_WAIT_TIME_IN_MILLISECS,
                                TimeUnit.MILLISECONDS);
                        if (!lock.wasAcquired()) {
                            LOG.error("Unable to acquire lock for interface {} waiting {}ms, come in next round",
                                    infName, LOCK_WAIT_TIME_IN_MILLISECS);
                            continue;
                        }
                        InterfaceJobQueue interfaceQueue = jobQueueMap.get(infName);
                        if (interfaceQueue.getInterfaceTimerFlag().get() == true) {
                            continue;
                        }
                        if (interfaceQueue.getTaskQueue().isEmpty()) {
                            // Remove any leftover interfaces
                            jobQueueMap.remove(infName);
                            LOG.info("Removed Interface {} from jobQueue map 1", infName);
                            continue;
                        }
                        TaskEntry taskEntry = interfaceQueue.getTaskQueue().peek();
                        if (taskEntry.task
                                == InterfaceInventoryStateTaskSequencer.this.getSleepTask()) {
                            interfaceQueue.getInterfaceTimerFlag().set(true);
                            LOG.info("Timer Started for {}", infName);
                            interfaceQueue.getInterfaceTimer().schedule(
                                    new InterfaceTimerTask(infName), INTERFACE_MIGRATION_TIME_IN_MILLISECS);
                        } else {
                            LOG.info("Job Enqueued to DJC for {}", infName);
                            coordinator.enqueueJob(infName, taskEntry.task, taskEntry.retries);
                        }
                        //remove the head element already serviced with peek() above
                        interfaceQueue.getTaskQueue().poll();
                        if (interfaceQueue.getTaskQueue().isEmpty()) {
                            // Enable quicker recovery for producer's to use original DJCs
                            jobQueueMap.remove(infName);
                            LOG.info("Removed Interface {} from jobQueue map 2", infName);
                            continue;
                        }
                    } catch (Exception e) {
                        LOG.error("Exception received when Sequencer processing interface {}", infName, e);
                    } finally {
                        if (lock.wasAcquired()) {
                            lock.close();
                        }
                    } // end try
                } // end for
                synchronized (InterfaceInventoryStateTaskSequencer.this) {
                    try {
                        // Wait for 1 second before going back looping through interfaces
                        InterfaceInventoryStateTaskSequencer.this.wait(1000);
                    } catch (InterruptedException e) {
                        LOG.warn("InterfaceInventoryStateTaskSequencer wait interrupted", e);
                    }
                }
            } //end while
        } // end run()
    } // end SequencerHandler

    @Override
    @SuppressFBWarnings("NN_NAKED_NOTIFY")
    public void enqueueJob(String key, Callable<List<ListenableFuture<Void>>> task, int maxRetries) {
        TaskEntry taskEntry = new TaskEntry(task, maxRetries);
        boolean queuedUp = false;
        NamedSimpleReentrantLock.AcquireResult lock = null;;

        while (!queuedUp) {
            try {
                lock = interfaceLock.tryAcquire(key, LOCK_WAIT_TIME_IN_MILLISECS, TimeUnit.MILLISECONDS);
                if (!lock.wasAcquired()) {
                    LOG.error("Unable to acquire lock for interface {} waiting {}ms, retrying enqueue singletask",
                            key, LOCK_WAIT_TIME_IN_MILLISECS);
                    continue;
                }
                InterfaceJobQueue interfaceQueue = jobQueueMap.computeIfAbsent(key,
                    mapKey -> new InterfaceJobQueue());
                interfaceQueue.getTaskQueue().add(taskEntry);
                LOG.info("Interface Queue size add-on for interface {} size {}",
                        key, interfaceQueue.getTaskQueue().size());
                queuedUp = true;
            } finally {
                if (lock.wasAcquired()) {
                    lock.close();
                }
            }
        }
        synchronized (InterfaceInventoryStateTaskSequencer.this) {
            // Notify waiting SequencerHandler
            InterfaceInventoryStateTaskSequencer.this.notifyAll();
        }
    }



    @Override
    @SuppressFBWarnings("NN_NAKED_NOTIFY")
    public void enqueueJob(String key, List<TaskEntry> taskEntries) {
        boolean queuedUp = false;
        NamedSimpleReentrantLock.AcquireResult lock = null;

        while (!queuedUp) {
            try {
                lock = interfaceLock.tryAcquire(key, LOCK_WAIT_TIME_IN_MILLISECS, TimeUnit.MILLISECONDS);
                if (!lock.wasAcquired()) {
                    LOG.error("Unable to acquire lock for interface {} waiting {}ms, retrying enqueue multitask",
                            key, LOCK_WAIT_TIME_IN_MILLISECS);
                    continue;
                }
                InterfaceJobQueue interfaceQueue = jobQueueMap.computeIfAbsent(key,
                    mapKey -> new InterfaceJobQueue());
                interfaceQueue.getTaskQueue().addAll(taskEntries);
                LOG.info("Interface Queue size into migration, for interface {} size {}",
                        key, interfaceQueue.getTaskQueue().size());
                queuedUp = true;
            } finally {
                if (lock.wasAcquired()) {
                    lock.close();
                }
            }
        }
        synchronized (InterfaceInventoryStateTaskSequencer.this) {
            // Notify waiting SequencerHandler
            InterfaceInventoryStateTaskSequencer.this.notifyAll();
        }
    }
}