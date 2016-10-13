package org.opendaylight.genius.datastoreutils;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeferedEvent<T extends DataObject> implements Comparable<DeferedEvent<T>> {

    private DataBroker dataBroker;

    public void removeAddDependency(InstanceIdentifier key, LogicalDatastoreType datastoreType) {
        for (DependencyData dependencyData : dependentIIDs) {
            if (dependencyData.getDsType().equals(datastoreType) && dependencyData.iid.equals(key) &&
                    dependencyData.expectData) {
                //remove this
            }
        }
    }

    public void removeDeleteDependency(InstanceIdentifier key, LogicalDatastoreType datastoreType) {
        for (DependencyData dependencyData : dependentIIDs) {
            if (dependencyData.getDsType().equals(datastoreType) && dependencyData.iid.equals(key) &&
                    !dependencyData.expectData) {
                //remove this
            }
        }
    }

    boolean areDependenciesResolved() {
        if (dependentIIDs == null || dependentIIDs.isEmpty()) {
            return true;
        }
        return false;
    }


    //eventTYPE: to be executed as part of listener
    public enum EventType {
        ADD, REMOVE, UPDATE
    }

    ;
    //key = Instance Identified
    public InstanceIdentifier<?> key;
    // OLD Data :: valid only in case of UPDATE event.
    public T oldData;
    // Data: going to be modified/updated
    public T newData;

    // TimeStamp: time at which WAITING FOR THE DEPENDECIES is insignificant.
    public long expiryTime = 0L;

    // TimeStamp: latest time at which CHECKING FOR THE DEPENDECIES happened.
    public long lastProcessedTime = 0L;

    // TimeStamp: at which the event is queued to LDH.
    public long queuedTime = 0L;

    // TimeStamp: wait time between each dependency check
    public long waitBetweenDependencyCheckTime = 0L;

    public List<DependencyData> getDependentIIDs() {
        return dependentIIDs;
    }

    // Dependent IID's
    public List<DependencyData> dependentIIDs = new ArrayList<DependencyData>();


    EventType eventType;

    LogicalDatastoreType datastoreType;

    public int retryCount = 5;

    DataBroker db;

    public boolean deferTimerBased = false;

    public DeferedEvent(InstanceIdentifier<T> key, T oldData, T newData, EventType eventType,
                        long waitBetweenDependencyCheckTime, List<DependencyData> dependentIIDs,
                        int retryCount, boolean deferTimerBased, final DataBroker db) {
        this.key = key;
        this.oldData = oldData;
        this.newData = newData;
        this.eventType = eventType;
        this.expiryTime = System.currentTimeMillis() + (retryCount * waitBetweenDependencyCheckTime);
        this.queuedTime = System.currentTimeMillis();
        this.dependentIIDs = Collections.synchronizedList(dependentIIDs);
        this.retryCount = retryCount;
        this.deferTimerBased = deferTimerBased;
        this.waitBetweenDependencyCheckTime = waitBetweenDependencyCheckTime;
        this.db = db;
    }

    @Override
    public int compareTo(DeferedEvent<T> other) {
        return (int) (this.queuedTime - other.queuedTime);
    }
}
