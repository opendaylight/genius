/*
 * Copyright (c) 2017 Ericsson S.A. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners;

import java.util.concurrent.atomic.LongAdder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.infrautils.metrics.Counter;
import org.opendaylight.infrautils.metrics.MetricDescriptor;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Abstract class providing some common functionality to abstract listeners. This is class is not designed to be
 * extended by the specific listeners, that's why it is package-private. It provides subclasses with access to the
 * {@link DataBroker} passed as constructor argument, listener registration/de-registration and a shutdown method to
 * be implemented if needed by the subclasses (e.g. shutting down services, closing resources, etc.)
 *
 * @param <T> type of the data object the listener is registered to.
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 */
abstract class AbstractDataTreeChangeListener<T extends DataObject> implements DataTreeChangeListener<T>,
        AutoCloseable {

    private final DataBroker dataBroker;
    private final DataTreeIdentifier<T> dataTreeIdentifier;
    private ListenerRegistration<AbstractDataTreeChangeListener<T>> dataChangeListenerRegistration;
    protected MetricProvider metricProvider;
    Counter added;
    Counter updated;
    Counter deleted;

    @Inject
    AbstractDataTreeChangeListener(DataBroker dataBroker, DataTreeIdentifier<T> dataTreeIdentifier) {
        this.dataBroker = dataBroker;
        this.dataTreeIdentifier = dataTreeIdentifier;
    }

    @Inject
    AbstractDataTreeChangeListener(DataBroker dataBroker, LogicalDatastoreType datastoreType,
                                   InstanceIdentifier<T> instanceIdentifier) {
        this(dataBroker, new DataTreeIdentifier<>(datastoreType, instanceIdentifier));
    }

    @Inject
    AbstractDataTreeChangeListener(DataBroker dataBroker,
                                   LogicalDatastoreType datastoreType,
                                   InstanceIdentifier<T> instanceIdentifier,
                                   MetricProvider metricProvider) {
        this(dataBroker, new DataTreeIdentifier<>(datastoreType, instanceIdentifier));
        this.metricProvider = metricProvider;
        added = initCounter("added:");
        updated = initCounter("updated:");
        deleted = initCounter("deleted:");
    }

    void incrementAdded() {
        added.increment();
    }

    void incrementUpdated() {
        updated.increment();
    }

    void incrementDeleted() {
        deleted.increment();
    }

    private Counter initCounter(String type) {
        if (metricProvider == null) {
            //Default incase metric provider is not available
            return new Counter() {
                LongAdder longAdder = new LongAdder();

                @Override
                public void close() {
                }

                @Override
                public void increment(long howMany) {
                    for (long l = 0 ; l < howMany; l++) {
                        longAdder.increment();
                    }
                }

                @Override
                public void decrement(long howMany) {
                    for (long l = 0 ; l < howMany; l++) {
                        longAdder.decrement();
                    }
                }

                @Override
                public long get() {
                    return longAdder.longValue();
                }
            };
        }
        String className = getClass().getSimpleName();
        return metricProvider.newCounter(new MetricDescriptor() {
            @Override
            public Object anchor() {
                return this;
            }

            @Override
            public String project() {
                return "genius";
            }

            @Override
            public String module() {
                return "genius";
            }

            @Override
            public String id() {
                return type + className;
            }
        });
    }

    @PostConstruct
    public void register() {
        this.dataChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(dataTreeIdentifier, this);
    }

    protected DataBroker getDataBroker() {
        return dataBroker;
    }

    @Override
    @PreDestroy
    public void close() {
        if (dataChangeListenerRegistration != null) {
            dataChangeListenerRegistration.close();
        }
    }
}

