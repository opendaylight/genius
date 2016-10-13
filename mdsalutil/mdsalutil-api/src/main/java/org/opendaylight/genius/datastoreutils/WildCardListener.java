package org.opendaylight.genius.datastoreutils;

import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;
import java.util.*;

import static org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase.LOG;

public class WildCardListener implements AutoCloseable, ClusteredDataTreeChangeListener<DataObject> {

    // copied from AsyncDataTreeChangeListenerBase class.
    private static final int STARTUP_LOOP_TICK = 500;
    private static final int STARTUP_LOOP_MAX_RETRIES = 8;

    InstanceIdentifier wildCardPath;

    Map<InstanceIdentifier, List<DeferedEvent>> waitingForAddIids = new HashMap<>();
    Map<InstanceIdentifier, List<DeferedEvent>> waitingForDeleteIids = new HashMap<>();
    LogicalDatastoreType datastoreType;
    ListenerRegistration<WildCardListener> registration;

    public WildCardListener(InstanceIdentifier wildCardPath, LogicalDatastoreType dsType, final DataBroker db) {
        this.wildCardPath = wildCardPath;
        datastoreType = dsType;
        registerListener(dsType, db);
    }

    void addToWaitingForAddQueue(InstanceIdentifier iid,  DeferedEvent event) {
        if (waitingForAddIids.get(iid) == null) {
            waitingForAddIids.put(iid, new ArrayList<>());
        }
        waitingForAddIids.get(iid).add(event);
    }

    void addToWaitingForDeleteQueue(InstanceIdentifier iid,  DeferedEvent event) {
        if (waitingForDeleteIids.get(iid) == null) {
            waitingForDeleteIids.put(iid, new ArrayList<>());
        }
        waitingForDeleteIids.get(iid).add(event);
    }

    public void registerListener(LogicalDatastoreType dsType, final DataBroker db) {
        final DataTreeIdentifier treeId = new DataTreeIdentifier<>(dsType, wildCardPath);
        try {
            TaskRetryLooper looper = new TaskRetryLooper(STARTUP_LOOP_TICK, STARTUP_LOOP_MAX_RETRIES);
            registration = looper.loopUntilNoException(() -> db.registerDataTreeChangeListener(treeId, this));
        } catch (final Exception e) {
        }
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<DataObject>> changes) {
        for (DataTreeModification<DataObject> change : changes) {
            final InstanceIdentifier<DataObject> key = change.getRootPath().getRootIdentifier();
                final DataObjectModification<DataObject> mod = change.getRootNode();
                switch (mod.getModificationType()) {
                    case SUBTREE_MODIFIED:
                    case WRITE:
                        if (waitingForAddIids.containsKey(key)) {
                            List<DeferedEvent> waitingEvents = waitingForAddIids.remove(key);
                            for (DeferedEvent deferedEvent : waitingEvents) {
                                deferedEvent.removeAddDependency(key, datastoreType);
                                if (deferedEvent.areDependenciesResolved()) {
                                    DeferedEvent dependencyResolvedEvent =
                                    AsyncDataTreeChangeListenerBase.getWaitingActualIIDMap().get(deferedEvent.key).remove();
                                    LOG.trace("Resolved event key: {} type: {}, eventTime: {}, currentTime: {}",
                                            dependencyResolvedEvent.key, dependencyResolvedEvent.eventType,
                                            dependencyResolvedEvent.expiryTime, System.currentTimeMillis());
                                }
                            }
                        }
                        break;
                    case DELETE:
                        if (waitingForAddIids.containsKey(key)) {
                            List<DeferedEvent> waitingEvents = waitingForDeleteIids.remove(key);
                            for (DeferedEvent deferedEvent : waitingEvents) {
                                deferedEvent.removeDeleteDependency(key, datastoreType);
                                if (deferedEvent.areDependenciesResolved()) {
                                    DeferedEvent dependencyResolvedEvent =
                                            AsyncDataTreeChangeListenerBase.getWaitingActualIIDMap().get(deferedEvent.key).remove();
                                    LOG.trace("Resolved event key: {} type: {}, eventTime: {}, currentTime: {}",
                                            dependencyResolvedEvent.key, dependencyResolvedEvent.eventType,
                                            dependencyResolvedEvent.expiryTime, System.currentTimeMillis());
                                }
                            }
                        }
                        break;
                }
        }
    }
}