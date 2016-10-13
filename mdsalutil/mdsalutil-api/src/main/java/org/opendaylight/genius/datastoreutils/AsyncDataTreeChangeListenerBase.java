/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.datastoreutils;

import com.google.common.base.*;

import java.util.*;
import java.util.concurrent.*;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;

public abstract class AsyncDataTreeChangeListenerBase<T extends DataObject, K extends DataTreeChangeListener>
        implements DataTreeChangeListener<T>, AutoCloseable {

    public static final Logger LOG = LoggerFactory.getLogger(AsyncDataTreeChangeListenerBase.class);

    private static final int DATATREE_CHANGE_HANDLER_THREAD_POOL_CORE_SIZE = 1;
    private static final int DATATREE_CHANGE_HANDLER_THREAD_POOL_MAX_SIZE = 1;
    private static final int DATATREE_CHANGE_HANDLER_THREAD_POOL_KEEP_ALIVE_TIME_SECS = 300;
    private static final int STARTUP_LOOP_TICK = 500;
    private static final int STARTUP_LOOP_MAX_RETRIES = 8;
    private static final long waitTimeBetweenEachTimeMills = 1000L;
    public static final int CONFIGURATION_DS_RETRY_COUNT = 5;

    private ListenerRegistration<K> listenerRegistration;
    private final ChainableDataTreeChangeListenerImpl<T> chainingDelegate = new ChainableDataTreeChangeListenerImpl<>();

    private static ScheduledExecutorService dataTreeChangeHandlerExecutor = new ScheduledThreadPoolExecutor(
            DATATREE_CHANGE_HANDLER_THREAD_POOL_CORE_SIZE);

    protected final Class<T> clazz;
    private final Class<K> eventClazz;
    public static DataBroker broker;

    // TODO ENUM USAGE
    public static final int EVENT_HAS_TO_BE_SUPRESSED = 1;
    public static final int EVENT_HAS_TO_BE_QUEUED = 2;
    public static final int EVENT_HAS_TO_BE_PROCESSED = 3;

    private long waitTimeAddEvent = 5L;
    private long waitTimeUpdateEvent = 5L;
    private long waitTimeRemoveEvent = 5L;

    public static ConcurrentLinkedQueue<DeferedEvent> listenerDependencyHelperQueue;

    Queue<DeferedEvent> deferedEvents = new ArrayDeque<>();

    public static HashMap<InstanceIdentifier, ConcurrentLinkedQueue<DeferedEvent>> getWaitingActualIIDMap() {
        return waitingActualIIDMap;
    }

    // deferEvents are differentiated based on two HASH-MAPs (TimerBasedWaitIIDMap , ListenerBasedWaitIIDMap)
    // Key: Actual - InstanceIdentifier, value: deferedEvent List  => each of access based on Actual-IID.
    public static HashMap<InstanceIdentifier, ConcurrentLinkedQueue<DeferedEvent>> waitingActualIIDMap = new HashMap<>();

    // Stores wildCard Listener IID, which gets updated on registration and shall be closed on completion.
    // Map of Map DataTreeIdentifier(WildCardPath of dependent IID) --> Listener, List of <deferedEvents>
    public class WildCardListenerIIDMap {
        ListenerRegistration<WildCardListener> wildCardListener;
        HashMap<InstanceIdentifier, List<DeferedEvent>> iidDeferedEventMap;

        public WildCardListenerIIDMap(ListenerRegistration<WildCardListener> wildCardListener,
                                      HashMap<InstanceIdentifier, List<DeferedEvent>> iidDeferedEventMap) {
            this.wildCardListener = wildCardListener;
            this.iidDeferedEventMap = iidDeferedEventMap;
        }
    }

    public HashMap<InstanceIdentifier, WildCardListener> wildCardListenerMap;

    public AsyncDataTreeChangeListenerBase(Class<T> clazz, Class<K> eventClazz) {
        this.clazz = Preconditions.checkNotNull(clazz, "Class can not be null!");
        this.eventClazz = Preconditions.checkNotNull(eventClazz, "eventClazz can not be null!");
        listeners.add(this);
    }

    static List<AsyncDataTreeChangeListenerBase> listeners = Collections.synchronizedList(new ArrayList<>());

    //Single thread for all the listeners, fires for every 1Second.
    static private Runnable pendingEventsTimerThread = new Runnable() {
        public void run() {
            for (AsyncDataTreeChangeListenerBase listener : listeners) {
                if (listener.hasPendingTasksToRun()) {
                    LOG.error("Timer Task, time:{}", new Date());
                    dataTreeChangeHandlerExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.runPendingTasks();
                        }
                    });
                }
            }
        }
    };

    void deferEventwithRegisterListener(DeferedEvent deferedEvent) {
        if (deferedEvent.deferTimerBased) {
            // Timer Based wait mechanism
//            deferedEvents.add(deferedEvent);
//            List<DependencyData> dependencyDataList = deferedEvent.getDependentIIDs();
//            for (DependencyData dependencyData : dependencyDataList ) {
//                if (waitingActualIIDMap.get(event.key) == null) {
//                    synchronized (event.key) {
//                        //checking twice to avoid multiple threads, updating the same iid.
//                        if (waitingActualIIDMap.get(event.key) == null) {
//                            waitingActualIIDMap.put(event.key, new ConcurrentLinkedQueue<DeferedEvent>());
//                        }
//                    }
//                }
//            }
        }

        if (!deferedEvent.deferTimerBased){
            // Listener Based wait mechanism
            List<DependencyData> dependentIIDList = deferedEvent.dependentIIDs;
            for (DependencyData dependentIID : dependentIIDList) {
                // getWildCardPath() from dependentIID list
                // databroker has to be passed as parameter to defered events
                DataTreeIdentifier dTidWildCardPath =
                        new DataTreeIdentifier(dependentIID.dsType, dependentIID.wildCardPath);

                //check whether there is already registered listerner for the dTidWildCardPath
                synchronized (dependentIID.wildCardPath) {
                    WildCardListener listener = wildCardListenerMap.get(dependentIID.wildCardPath);
                    if (null == listener) {
                        listener = new WildCardListener (dependentIID.wildCardPath, dependentIID.getDsType(), deferedEvent.db);
                        wildCardListenerMap.put(dependentIID.wildCardPath, listener);
                    }
                    listener = wildCardListenerMap.get(dependentIID.wildCardPath);
                    if (dependentIID.expectData) {
                        listener.addToWaitingForAddQueue(dependentIID.iid, deferedEvent);
                    } else {
                        //add to delete wait queue
                        listener.addToWaitingForDeleteQueue(dependentIID.iid, deferedEvent);
                    }
                }
            }
        }
    }

    protected void runPendingTasks() {
        long currentTime = System.currentTimeMillis();

        /** Handling of newly Queued events **/
        LOG.trace("Running Pending Task : {}", new Date());
        while (deferedEvents.size() > 0) {
            //Dequeue deferedEvent from Queue (deferedEvents).
            DeferedEvent event = null;
            try {
                event = deferedEvents.remove();
            } catch (Exception NoSuchElementException) {
                //if there are no elements exist, just break the loop (may be other thread processed it).
                break;
            }
            LOG.trace("event key: {} type: {}, eventTime: {}, currentTime: {}, dependentIIDs: {}",
                    event.key, event.eventType, event.expiryTime, currentTime, event.dependentIIDs.size());
            //dependent IID are present ?
            if (event.dependentIIDs != null) {
                // every event dequeued has to be mainted in map
                synchronized (event.key) {
                    ConcurrentLinkedQueue<DeferedEvent> actualIIDQueue = waitingActualIIDMap.get(event.key);
                    if (actualIIDQueue == null) {
                        actualIIDQueue = new ConcurrentLinkedQueue<>();
                    }
                    try {
                        // enqueue the event to IID Queue.
                        actualIIDQueue.add(event);
                    } catch (IllegalStateException | NullPointerException e) {
                        e.printStackTrace();
                        LOG.error("IllegalState/NullPointer while enqueuing the " +
                                        "event key: {} type: {}, eventTime: {}, currentTime: {}",
                                event.key, event.eventType, event.expiryTime, currentTime);
                    }
                    waitingActualIIDMap.put(event.key, actualIIDQueue);
                }

                if (!event.deferTimerBased) {
                    // Listener based event: Register Listener.
                    deferEventwithRegisterListener(event);
                }
            } else {
                LOG.error("Event Queued with NULL dependencies event key: {} type: {}, eventTime: {}, currentTime: {}",
                        event.key, event.eventType, event.expiryTime, currentTime);
            }
        }

        /** Handling of existing Timer based events from MAP **/
        for (HashMap.Entry<InstanceIdentifier, ConcurrentLinkedQueue<DeferedEvent>> waitingIID :
                waitingActualIIDMap.entrySet()) {
            DeferedEvent pendingActualIIDEvent = waitingIID.getValue().peek();
            if (pendingActualIIDEvent == null) {
                synchronized (waitingIID.getKey()) {
                    waitingIID.getValue().remove();
                }
            } else if ((pendingActualIIDEvent != null) && (pendingActualIIDEvent.deferTimerBased)) {
                // Timer Based dependency check.
                if (currentTime > pendingActualIIDEvent.expiryTime) {
                    //Dequeue and supress this event, retry_count/expirytime elapsed
                    DeferedEvent expiredActualIIDEvent = null;
                    synchronized (waitingIID.getKey()) {
                        ConcurrentLinkedQueue<DeferedEvent> pendingAcutalIIDQueue =
                                waitingActualIIDMap.get(waitingIID.getKey());
                        expiredActualIIDEvent = pendingAcutalIIDQueue.remove();
                        waitingActualIIDMap.put(waitingIID.getKey(), pendingAcutalIIDQueue);
                    }
                    if (expiredActualIIDEvent != null) {
                        LOG.error("Expired event key: {} type: {}, eventTime: {}, currentTime: {}",
                                expiredActualIIDEvent.key, expiredActualIIDEvent.eventType,
                                expiredActualIIDEvent.expiryTime, currentTime);
                    }
                } else if (currentTime >
                        (pendingActualIIDEvent.lastProcessedTime +
                                pendingActualIIDEvent.waitBetweenDependencyCheckTime)) {
                    // Try for dependency resolution.
                    List<DependencyData> unresolvedDependencyDataList = null;
                    if ((unresolvedDependencyDataList =
                            hasDependencyResolved(pendingActualIIDEvent.dependentIIDs, pendingActualIIDEvent.db)) == null) {
                        // get the event, clear element for IID Trigger actual IID call back
                        DeferedEvent dependencyResolvedEvent = null;
                        synchronized (waitingIID.getKey()) {
                            dependencyResolvedEvent = waitingIID.getValue().remove();
                        }
                        if (dependencyResolvedEvent  != null) {
                            final DeferedEvent finaldependencyResolvedEvent = dependencyResolvedEvent;
                            dataTreeChangeHandlerExecutor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    switch (finaldependencyResolvedEvent.eventType) {
                                        case ADD:
                                            add(finaldependencyResolvedEvent.key,
                                                    (T) finaldependencyResolvedEvent.newData);
                                            break;
                                        case REMOVE:
                                            remove(finaldependencyResolvedEvent.key,
                                                    (T) finaldependencyResolvedEvent.newData);
                                            break;
                                        case UPDATE:
                                            update(finaldependencyResolvedEvent.key,
                                                    (T) finaldependencyResolvedEvent.oldData,
                                                    (T) finaldependencyResolvedEvent.newData);
                                            break;
                                    }
                                }
                            });

                            LOG.trace("Resolved event key: {} type: {}, eventTime: {}, currentTime: {}",
                                    dependencyResolvedEvent.key, dependencyResolvedEvent.eventType, dependencyResolvedEvent.expiryTime, currentTime);
                        }
                    } else {
                        synchronized (waitingIID.getKey()) {
                            ConcurrentLinkedQueue<DeferedEvent> pendingAcutalIIDQueue =
                                    waitingActualIIDMap.get(waitingIID.getKey());
                            DeferedEvent waitingActualIIDEvent = pendingAcutalIIDQueue.peek();
                            waitingActualIIDEvent.dependentIIDs.clear();
                            waitingActualIIDEvent.dependentIIDs.addAll(unresolvedDependencyDataList);
                        }
                    }
                }
            } else {
                // Ignore Listener based events
            }
        }
    }

    static {
        dataTreeChangeHandlerExecutor.scheduleAtFixedRate(pendingEventsTimerThread, 0, 1, TimeUnit.SECONDS);
    }

    public void addAfterListener(DataTreeChangeListener<T> listener) {
        chainingDelegate.addAfterListener(listener);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<T>> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }

        DataTreeChangeHandler dataTreeChangeHandler = new DataTreeChangeHandler(changes);
        dataTreeChangeHandlerExecutor.execute(dataTreeChangeHandler);
    }

    public void registerListener(LogicalDatastoreType dsType, final DataBroker db) {
        final DataTreeIdentifier<T> treeId = new DataTreeIdentifier<>(dsType, getWildCardPath());
        try {
            broker = db;
            TaskRetryLooper looper = new TaskRetryLooper(STARTUP_LOOP_TICK, STARTUP_LOOP_MAX_RETRIES);
            listenerRegistration = looper.loopUntilNoException(() -> db.registerDataTreeChangeListener(treeId, getDataTreeChangeListener()));
        } catch (final Exception e) {
            LOG.warn("{}: Data Tree Change listener registration failed.", eventClazz.getName());
            LOG.debug("{}: Data Tree Change listener registration failed: {}", eventClazz.getName(), e);
            throw new IllegalStateException(eventClazz.getName() + "{}startup failed. System needs restart.", e);
        }
    }

    /**
     * Subclasses override this and place initialization logic here, notably
     * calls to registerListener(). Note that the overriding method MUST repeat
     * the PostConstruct annotation, because JSR 250 specifies that lifecycle
     * methods "are called unless a subclass of the declaring class overrides
     * the method without repeating the annotation".  (The blueprint-maven-plugin
     * would gen. XML which calls PostConstruct annotated methods even if they are
     * in a subclass without repeating the annotation, but this is wrong and not
     * JSR 250 compliant, and while working in BP, then causes issues e.g. when
     * wiring with Guice for tests, so do always repeat it.)
     */
    @PostConstruct
    protected void init() {
    }

    @Override
    @PreDestroy
    public void close() throws Exception {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (final Exception e) {
                LOG.error("Error when cleaning up DataTreeChangeListener.", e);
            }
            listenerRegistration = null;
        }
    }

    protected abstract InstanceIdentifier<T> getWildCardPath();

    protected abstract void remove(InstanceIdentifier<T> key, T dataObjectModification);

    protected abstract void update(InstanceIdentifier<T> key, T dataObjectModificationBefore, T dataObjectModificationAfter);

    protected abstract void add(InstanceIdentifier<T> key, T dataObjectModification);

    protected abstract K getDataTreeChangeListener();

    public class DataTreeChangeHandler implements Runnable {
        private final Collection<DataTreeModification<T>> changes;

        public DataTreeChangeHandler(Collection<DataTreeModification<T>> changes) {
            this.changes = changes;
        }

        @Override
        public void run() {
            for (DataTreeModification<T> change : changes) {
                final InstanceIdentifier<T> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<T> mod = change.getRootNode();

                /*
                    isEventToBeProcessed has to be verified here.
                 */

                switch (mod.getModificationType()) {
                    case DELETE:
                        if (isEventToBeProcessed(key, null, null,
                                DeferedEvent.EventType.REMOVE, waitTimeRemoveEvent, CONFIGURATION_DS_RETRY_COUNT)) {
                            //process Remove event here.
                            remove(key, mod.getDataBefore());
                        } else {
                            LOG.trace("Event Remove either Queued/Suppressed, key:{}, eventType:{}", key, "UPDATE");
                        }
                        break;
                    case SUBTREE_MODIFIED:
                        if (isEventToBeProcessed(key, mod.getDataBefore(), mod.getDataAfter(),
                                DeferedEvent.EventType.UPDATE, waitTimeUpdateEvent,
                                CONFIGURATION_DS_RETRY_COUNT)) {
                            //process Update event here
                            update(key, mod.getDataBefore(), mod.getDataAfter());
                        } else {
                            LOG.trace("Event Update either Queued/Suppressed, key:{}, eventType:{}", key, "UPDATE");
                        }
                        break;
                    case WRITE:
                        if (mod.getDataBefore() == null) {
                            if (isEventToBeProcessed(key, null, mod.getDataAfter(),
                                    DeferedEvent.EventType.ADD, waitTimeAddEvent,
                                    CONFIGURATION_DS_RETRY_COUNT)) {
                                //process ADD event here
                                add(key, mod.getDataAfter());
                            } else {
                                // Queue the event, which is already taken care inside isEventToBeProcessed
                                LOG.trace("Event Add either Queued/Suppressed, key:{}, eventType:{}", key, "UPDATE");
                            }
                        } else {
                            if (isEventToBeProcessed(key, mod.getDataBefore(), mod.getDataAfter(),
                                    DeferedEvent.EventType.UPDATE, waitTimeUpdateEvent,
                                    CONFIGURATION_DS_RETRY_COUNT)) {
                                //process Update event here
                                update(key, mod.getDataBefore(), mod.getDataAfter());
                            } else {
                                LOG.trace("Event Update either Queued/Suppressed, key:{}, eventType:{}", key, "UPDATE");
                            }
                        }
                        break;
                    default:
                        // FIXME: May be not a good idea to throw.
                        throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
                }
            }
            chainingDelegate.notifyAfterOnDataTreeChanged(changes);
        }
    }

    /*
      isEventToBeProcessed : deals with only re-ordering. If there a EVENT pending for same IID which has to be processed
      this api will take action: Queue/Process/Supress.
      This is verified only when there is a new event add/update/remove called from MD-SAL.
     */
    protected boolean isEventToBeProcessed(InstanceIdentifier<T> key, T oldData,
                                           T newData, DeferedEvent.EventType eventType, long waitTime,
                                           int retry_count) {
        int whatToDoWithEvent = whatToDoWithEventFromDS(key, eventType);
        if (whatToDoWithEvent == EVENT_HAS_TO_BE_QUEUED) {
            // DEFERing the event here is mainly due to DEPENDENT EVENT (not IID) is pending to be processed
            // as there are no dependency list, we can set "deferTimerBased is SET to TRUE" and
            // "dependentIidResultList is SET to NULL"
            deferEvent(key, oldData, newData, eventType, waitTime, null, retry_count, true, null);
        } else if (whatToDoWithEvent == EVENT_HAS_TO_BE_PROCESSED) {
            // Event has to be processed.
            return true;
        } else if (whatToDoWithEvent == EVENT_HAS_TO_BE_SUPRESSED) {
            LOG.error("Event suppressed, key:{}, eventType:{}", key, "UPDATE");
        } else {
            LOG.error("Unknown result in Event processing, key:{}, eventType:{}", key, eventType);
        }
        return false;
    }

    //returns TRUE, if this event to be suppressed
    protected int whatToDoWithEventFromDS(InstanceIdentifier<T> currentKey, DeferedEvent.EventType currentEventType) {
        List<DeferedEvent> actualIIDQueuedEvents = new ArrayList<DeferedEvent>();
        /*
        Multiple things to consider before queuing the event:
        -----------------------------------------------------
        CURRENT-EVENT        QUEUED-EVENT           ACTION
        -----------------------------------------------------
        ADD                     ADD                 NOT EXPECTED
        ADD                     REMOVE              QUEUE THE EVENT.
        ADD                     UPDATE              NOT EXPECTED.
        ----
        UPDATE                  ADD                 QUEUE THE EVENT.
        UPDATE                  UPDATE              QUEUE THE EVENT,
        UPDATE                  REMOVE              NOT EXPECTED
        ----
        REMOVE                  ADD                 SUPPRESS BOTH.
        REMOVE                  UPDATE              EXECUTE REMOVE SUPPRESS UDPATE
        REMOVE                  REMOVE              NOT EXPECTED.
                */

        if (currentKey == null) {
            return EVENT_HAS_TO_BE_SUPRESSED;
        }

        //every deferEvent call, iterate through the set of deferred events and find out the events which has saem KEY.
        ConcurrentLinkedQueue<DeferedEvent> actualIIDpendingQueue = waitingActualIIDMap.get(currentKey);
        for (DeferedEvent pendingEvents : actualIIDpendingQueue ) {
                //actualIIDQueuedEvents contains, list of events pending for same KEY.
                actualIIDQueuedEvents.add(pendingEvents);
        }

        //Incase there are events pending for the same KEY, apply event-suppression on need basis
        if (!actualIIDQueuedEvents.isEmpty()) {
            //suppress LOGIC

            if (currentEventType == DeferedEvent.EventType.ADD) {
                // CURRENT EVENT TYPE == ADD

                for (DeferedEvent pendingEvent : actualIIDQueuedEvents) {
                    if (pendingEvent.eventType == DeferedEvent.EventType.ADD) {
                        // QUEUED EVENT TYPE == ADD
                        // Add() followed by Add(): NOT EXPECTED
                        LOG.error("LDH: ADD() received when there is pending ADD() in Queue" +
                                        "event key: {} oldEventQueuedTime: {}, entries with SameKey: {}",
                                pendingEvent.key, pendingEvent.queuedTime, actualIIDQueuedEvents.size());
                        return EVENT_HAS_TO_BE_SUPRESSED;
                    } else if (pendingEvent.eventType == DeferedEvent.EventType.UPDATE) {
                        // QUEUED EVENT TYPE == UPDATE
                        // Update() followed by an ADD() : Not expected.
                        LOG.error("LDH: ADD() received when there is pending UPDATE() in Queue" +
                                        "event key: {} oldEventQueuedTime: {}, entries with SameKey: {}",
                                pendingEvent.key, pendingEvent.queuedTime, actualIIDQueuedEvents.size());
                        return EVENT_HAS_TO_BE_SUPRESSED;
                    } else if (pendingEvent.eventType == DeferedEvent.EventType.REMOVE) {
                        // QUEUED EVENT TYPE == REMOVE
                        // let the event to be QUEUED.
                        return EVENT_HAS_TO_BE_QUEUED;
                    }
                }
            } else if (currentEventType == DeferedEvent.EventType.UPDATE) {
                // CURRENT EVENT TYPE == UPDATE

                for (DeferedEvent pendingEvent : actualIIDQueuedEvents) {
                    if (pendingEvent.eventType == DeferedEvent.EventType.ADD) {
                        // QUEUED EVENT TYPE == ADD
                        // let the event to be QUEUED
                        return EVENT_HAS_TO_BE_QUEUED;
                    } else if (pendingEvent.eventType == DeferedEvent.EventType.UPDATE) {
                        // QUEUED EVENT TYPE == UPDATE
                        // let the event to be QUEUED
                        return EVENT_HAS_TO_BE_QUEUED;
                    } else if (pendingEvent.eventType == DeferedEvent.EventType.REMOVE) {
                        // QUEUED EVENT TYPE == REMOVE
                        // Remove() followed by an Update() : Not expected.
                        LOG.error("LDH: UPDATE() received when there is pending REMOVE() in Queue" +
                                        "event key: {} oldEventQueuedTime: {}, entries with SameKey: {}",
                                pendingEvent.key, pendingEvent.queuedTime, actualIIDQueuedEvents.size());
                        return EVENT_HAS_TO_BE_SUPRESSED;
                    }
                }
            } else if (currentEventType == DeferedEvent.EventType.REMOVE) {
                // CURRENT EVENT TYPE == REMOVE

                for (DeferedEvent pendingEvent : actualIIDQueuedEvents) {
                    if (pendingEvent.eventType == DeferedEvent.EventType.ADD) {
                        // QUEUED EVENT TYPE == ADD
                        //  Add() is present in Queue, now we received Remove(): remove pending add(), supress remove()
                        suppressAddFollowedByRemoveEvent(pendingEvent);
                        return EVENT_HAS_TO_BE_SUPRESSED;
                    } else if (pendingEvent.eventType == DeferedEvent.EventType.UPDATE) {
                        // QUEUED EVENT TYPE == UPDATE
                        //Remove() followed by Update(): Supress Update and execute Remove().
                        // TODO : for now, decided to process this event, as there can be state changes on UPDATE.
                        return EVENT_HAS_TO_BE_QUEUED;
                    } else if (pendingEvent.eventType == DeferedEvent.EventType.REMOVE) {
                        // QUEUED EVENT TYPE == REMOVE
                        // Remove() followed by an Remove() : Not expected.
                        LOG.error("LDH: REMOVE() received when there is pending REMOVE() in Queue" +
                                        "event key: {} oldEventQueuedTime: {}, entries with SameKey: {}",
                                pendingEvent.key, pendingEvent.queuedTime, actualIIDQueuedEvents.size());
                        return EVENT_HAS_TO_BE_SUPRESSED;
                    }
                }
            }
        }
        return EVENT_HAS_TO_BE_PROCESSED;
    }

    protected void deferEvent(InstanceIdentifier<T> key, T dataObjectModificationBefore, T dataObjectModificationAfter,
                              DeferedEvent.EventType eventType, long expiryTIme,
                              List<DependencyData> dependentIIDs, int retryCount, boolean deferTimerBased,
                              DataBroker db) {
        if (key != null) {
            deferedEvents.add(new DeferedEvent(key, dataObjectModificationBefore, dataObjectModificationAfter, eventType,
                    expiryTIme, dependentIIDs, retryCount, deferTimerBased, db));
        }
    }

    protected boolean genericSuppressEvent(DeferedEvent suppressedEvent, DeferedEvent.EventType eventType) {
        if (suppressedEvent != null) {
            if (eventType != suppressedEvent.eventType) {
                return false;
            }
            deferedEvents.remove(suppressedEvent);
            return true;
        }
        return false;
    }

    protected boolean suppressUpdateFollowedByRemoveEvent(DeferedEvent suppressedAddEvent) {
        return genericSuppressEvent(suppressedAddEvent, DeferedEvent.EventType.UPDATE);
    }

    protected boolean suppressRemoveFollowedByRemoveEvent(DeferedEvent suppressedAddEvent) {
        return genericSuppressEvent(suppressedAddEvent, DeferedEvent.EventType.REMOVE);
    }

    protected boolean suppressAddFollowedByAddEvent(DeferedEvent suppressedAddEvent) {
        return genericSuppressEvent(suppressedAddEvent, DeferedEvent.EventType.ADD);
    }

    protected boolean suppressAddFollowedByRemoveEvent(DeferedEvent suppressedAddEvent) {
        return genericSuppressEvent(suppressedAddEvent, DeferedEvent.EventType.ADD);
    }

    protected boolean hasPendingTasksToRun() {
        DeferedEvent event = deferedEvents.peek();
        if (event != null) {
            if (event.expiryTime < System.currentTimeMillis()) {
                return true;
            }
        }
        return false;
    }

    protected Queue<DeferedEvent> getPendingEventsQueue() {
        return listenerDependencyHelperQueue;
    }

    /**
     * Takes dependency List and returns pending dependent IID's as list
     */
    protected List<DependencyData> hasDependencyResolved(List<DependencyData> dependentList) {

        List<DependencyData> unresolvedDependentIids = new ArrayList<DependencyData>();

        for (DependencyData dependentIID : dependentList) {
            ReadOnlyTransaction tx = broker.newReadOnlyTransaction();
            Optional<?> data = null;//TODO
            try {
                data = tx.read(dependentIID.dsType, (InstanceIdentifier<? extends DataObject>) dependentIID.iid).get();
                if (dependentIID.expectData) {
                    if (!data.isPresent()) {
                        unresolvedDependentIids.add(dependentIID);
                    }
                } else { //dont expect DATA to be present
                    if (data.isPresent()) {
                        unresolvedDependentIids.add(dependentIID);
                    }
                }
            } catch (Exception e) {

            }
        }
        return unresolvedDependentIids;
    }

    /**
     * Takes dependency List and returns pending dependent IID's as list
     */
    protected List<DependencyData> hasDependencyResolved(List<DependencyData> dependentList, DataBroker db) {

        List<DependencyData> unresolvedDependentIids = new ArrayList<DependencyData>();

        for (DependencyData dependentIID : dependentList) {
            ReadOnlyTransaction tx = db.newReadOnlyTransaction();
            Optional<?> data = null;//TODO
            try {
                data = tx.read(dependentIID.dsType, (InstanceIdentifier<? extends DataObject>) dependentIID.iid).get();
                if (dependentIID.expectData) {
                    if (!data.isPresent()) {
                        unresolvedDependentIids.add(dependentIID);
                    }
                } else { //dont expect DATA to be present
                    if (data.isPresent()) {
                        unresolvedDependentIids.add(dependentIID);
                    }
                }
            } catch (Exception e) {

            }
        }
        return unresolvedDependentIids;
    }

    /**
     * Returns the wildCard path of iid/childNode
     *
     * @param leafPath child node IID, leaf node where data is either expected/not-expected.
     * @return wildCradPath parentNode/wildCardPath of the leaf node.
     */
    InstanceIdentifier getWildcardPathForLeaf(InstanceIdentifier leafPath) {
        return leafPath;//TODO make it abstract later
    }

}
