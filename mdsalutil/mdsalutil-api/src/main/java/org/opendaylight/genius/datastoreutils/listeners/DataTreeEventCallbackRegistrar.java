/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners;

import static org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar.NextAction.UNREGISTER;

import com.google.common.annotations.Beta;
import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Service to register callbacks for data tree changes for a fixed instance.
 *
 * <p>The current implementation assumes that the expected instance will eventually be added, updated or deleted; if
 * that never happens in the first place, then an internal data structure could fill up and theoretically lead to OOM
 * problems if you do not specify a timeout.  Likewise, if {@link NextAction#CALL_AGAIN} is used but no next event
 * matching path ever occurs.  In the "Full Sync Upgrade" scenario, for which this utility was originally created, this
 * is not a problem, as we are sure the subsequent changes are about to happen (just out of originally expected order).
 *
 * @author Josh original idea and design feedback
 * @author Michael Vorburger.ch API design and first implementation
 * @author Tom Pantelis review and feedback on concurrency issue in implementation
 */
@Beta // we may still change this API
@NonNullByDefault
public interface DataTreeEventCallbackRegistrar {

    enum NextAction {
        /**
         * When the callback function returns this, then it is unregistered and will
         * never be invoked anymore.
         */
        UNREGISTER,

        /**
         * When the callback function returns this, then it will be called again for
         * add, update OR remove on the given store &amp; path.
         */
        CALL_AGAIN
    }

    <T extends DataObject> void onAdd(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Function<T, NextAction> callback,
            Duration timeoutDuration, Consumer<DataTreeIdentifier<T>> timedOutCallback);

    <T extends DataObject> void onAdd(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Function<T, NextAction> callback);

    default <T extends DataObject> void onAdd(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Consumer<T> callback) {
        onAdd(store, path, t -> {
            callback.accept(t);
            return UNREGISTER;
        });
    }

    default <T extends DataObject> void onAdd(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Consumer<T> callback, Duration timeoutDuration, Consumer<DataTreeIdentifier<T>> timedOutCallback) {
        onAdd(store, path, t -> {
            callback.accept(t);
            return UNREGISTER;
        }, timeoutDuration, timedOutCallback);
    }


    <T extends DataObject> void onUpdate(LogicalDatastoreType store, InstanceIdentifier<T> path,
            BiFunction<T, T, NextAction> callback,
            Duration timeoutDuration, Consumer<DataTreeIdentifier<T>> timedOutCallback);

    <T extends DataObject> void onUpdate(LogicalDatastoreType store, InstanceIdentifier<T> path,
            BiFunction<T, T, NextAction> callback);

    default <T extends DataObject> void onUpdate(LogicalDatastoreType store, InstanceIdentifier<T> path,
            BiConsumer<T, T> callback) {
        onUpdate(store, path, (t1, t2) -> {
            callback.accept(t1, t2);
            return UNREGISTER;
        });
    }

    default <T extends DataObject> void onUpdate(LogicalDatastoreType store, InstanceIdentifier<T> path,
            BiConsumer<T, T> callback,
            Duration timeoutDuration, Consumer<DataTreeIdentifier<T>> timedOutCallback) {
        onUpdate(store, path, (t1, t2) -> {
            callback.accept(t1, t2);
            return UNREGISTER;
        }, timeoutDuration, timedOutCallback);
    }


    <T extends DataObject> void onAddOrUpdate(LogicalDatastoreType store, InstanceIdentifier<T> path,
            BiFunction<@Nullable T, T, NextAction> callback,
            Duration timeoutDuration, Consumer<DataTreeIdentifier<T>> timedOutCallback);

    /**
     * Call back when expected instance was added or updated, with NextAction support.
     * @param store the expected data store
     * @param path the path to watch for changes on
     * @param callback the callback as {@link BiFunction}, where the first argument is the data before the update
     *             or null in case of an add, the second argument is the data after the update (or add; never null),
     *             and the returned value determines whether to keep listening for changes or not anymore.
     */
    <T extends DataObject> void onAddOrUpdate(LogicalDatastoreType store, InstanceIdentifier<T> path,
                                              BiFunction<@Nullable T, T, NextAction> callback);

    /**
     * Call back when expected instance was added or updated, with implicit {@link NextAction#UNREGISTER}.
     * See {@link #onAddOrUpdate(LogicalDatastoreType, InstanceIdentifier, BiFunction)} for more details.
     */
    default <T extends DataObject> void onAddOrUpdate(LogicalDatastoreType store, InstanceIdentifier<T> path,
                                                 BiConsumer<@Nullable T, T> callback) {
        onAddOrUpdate(store, path, (t1, t2) -> {
            callback.accept(t1, t2);
            return UNREGISTER;
        });
    }

    default <T extends DataObject> void onAddOrUpdate(LogicalDatastoreType store, InstanceIdentifier<T> path,
            BiConsumer<@Nullable T, T> callback,
            Duration timeoutDuration, Consumer<DataTreeIdentifier<T>> timedOutCallback) {
        onAddOrUpdate(store, path, (t1, t2) -> {
            callback.accept(t1, t2);
            return UNREGISTER;
        }, timeoutDuration, timedOutCallback);
    }


    <T extends DataObject> void onRemove(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Function<T, NextAction> callback,
            Duration timeoutDuration, Consumer<DataTreeIdentifier<T>> timedOutCallback);

    <T extends DataObject> void onRemove(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Function<T, NextAction> callback);

    default <T extends DataObject> void onRemove(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Consumer<T> callback) {
        onRemove(store, path, t -> {
            callback.accept(t);
            return UNREGISTER;
        });
    }

    default <T extends DataObject> void onRemove(LogicalDatastoreType store, InstanceIdentifier<T> path,
            Consumer<T> callback,
            Duration timeoutDuration, Consumer<DataTreeIdentifier<T>> timedOutCallback) {
        onRemove(store, path, t -> {
            callback.accept(t);
            return UNREGISTER;
        }, timeoutDuration, timedOutCallback);
    }

}
