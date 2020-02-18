/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.genius.datastoreutils.TransactionCommitFailedExceptionMapper.SUBMIT_MAPPER;

import com.google.common.base.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.RetryingManagedNewTransactionRunner;
import org.opendaylight.infrautils.utils.concurrent.ListenableFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

/**
 * Utility methods for single transaction DataBroker usage.
 *
 * <p>Please consider using a {@link ManagedNewTransactionRunner} instead.
 *
 * @author Michael Vorburger
 */
// do *NOT* make this a BP @Singleton; see https://wiki.opendaylight.org/view/BestPractices/DI_Guidelines#Nota_Bene for why
public class SingleTransactionDataBroker {

    private static final Logger LOG = LoggerFactory.getLogger(SingleTransactionDataBroker.class);

    private static final int DEFAULT_RETRIES = 3; // duplicated in RetryingManagedNewTransactionRunnerImpl

    private final DataBroker broker;

    // do *NOT* use BP @Inject here, see comment above
    public SingleTransactionDataBroker(@NonNull DataBroker broker) {
        this.broker = requireNonNull(broker, "dataBroker");
    }

    /**
     * Synchronously read; preferred &amp; strongly recommended method variant
     * over other ones offered by this class (because this is the most explicit
     * variant).
     *
     * <p>See {@link ReadTransaction#read(LogicalDatastoreType, InstanceIdentifier)}.
     *
     * @param datastoreType
     *            Logical data store from which read should occur.
     * @param path
     *            Path which uniquely identifies subtree which client want to read
     * @param <T>
     *            DataObject subclass
     *
     * @return If the data at the supplied path exists, returns an Optional
     *         object containing the data; if the data at the supplied path does
     *         not exist, returns Optional#absent().
     * @throws ReadFailedException in case of a technical (!) error while reading
     */
    public <T extends DataObject> Optional<T> syncReadOptional(
            LogicalDatastoreType datastoreType, InstanceIdentifier<T> path)
            throws ExecutionException, InterruptedException {
        return syncReadOptional(broker, datastoreType, path);
    }

    public static <T extends DataObject> Optional<T> syncReadOptional(
            DataBroker broker, LogicalDatastoreType datastoreType, InstanceIdentifier<T> path)
            throws ExecutionException, InterruptedException {

        try (ReadOnlyTransaction tx = broker.newReadOnlyTransaction()) {
            return tx.read(datastoreType, path).get();
        }
    }

    /**
     * Synchronously read; method variant to use by code which expecting that data MUST exist at given path.
     *
     * <p>This variant is only recommended if the calling code would treat the Optional
     * returned by the other method variant as a terminal failure anyway, and would itself throw
     * an Exception for that.
     *
     * <p>If calling code can more sensibly handle non-present data, then use
     * {@link #syncReadOptional(LogicalDatastoreType, InstanceIdentifier)} instead of this.
     *
     * <p>See {@link ReadTransaction#read(LogicalDatastoreType, InstanceIdentifier)}.
     *
     * @param datastoreType
     *            Logical data store from which read should occur.
     * @param path
     *            Path which uniquely identifies subtree which client want to read
     * @param <T>
     *            DataObject subclass
     *
     * @return If the data at the supplied path exists, returns the data.
     * @throws ReadFailedException in case of a technical (!) error while reading
     * @throws ExpectedDataObjectNotFoundException a ReadFailedException sub-type, if no data exists at path
     */
    public <T extends DataObject> T syncRead(
            LogicalDatastoreType datastoreType, InstanceIdentifier<T> path)
            throws ReadFailedException {
        return syncRead(broker, datastoreType, path);
    }

    public static <T extends DataObject> T syncRead(
            DataBroker broker, LogicalDatastoreType datastoreType, InstanceIdentifier<T> path)
            throws ExecutionException, InterruptedException, ExpectedDataObjectNotFoundException {

        try (ReadOnlyTransaction tx = broker.newReadOnlyTransaction()) {
            Optional<T> optionalDataObject = tx.read(datastoreType, path).get();
            if (optionalDataObject.isPresent()) {
                return optionalDataObject.get();
            } else {
                throw new ExpectedDataObjectNotFoundException(datastoreType, path);
            }
        }
    }

    /**
     * Synchronously read; swallowing (!) ReadFailedException.
     *
     * <p>See {@link ReadTransaction#read(LogicalDatastoreType, InstanceIdentifier)}.
     *
     * @deprecated This variant is not recommended, and only exists for legacy
     *             purposes for code which does not yet correctly propagate
     *             technical exceptions. Prefer using
     *             {@link #syncReadOptional(LogicalDatastoreType, InstanceIdentifier)}.
     *
     * @param datastoreType
     *            Logical data store from which read should occur.
     * @param path
     *            Path which uniquely identifies subtree which client want to read
     * @param <T>
     *            DataObject subclass
     *
     * @return If the data at the supplied path exists, returns an Optional
     *         object containing the data; if the data at the supplied path does
     *         not exist, or a technical error occurred (logged), returns
     *         Optional#absent().
     */
    @Deprecated
    public <T extends DataObject> Optional<T> syncReadOptionalAndTreatReadFailedExceptionAsAbsentOptional(
            LogicalDatastoreType datastoreType, InstanceIdentifier<T> path) {
        return syncReadOptionalAndTreatReadFailedExceptionAsAbsentOptional(broker, datastoreType, path);
    }

    /**
     * Synchronously read; swallowing (!) ReadFailedException.
     *
     * @deprecated This variant is not recommended, and only exists for legacy
     *             purposes for code which does not yet correctly propagate
     *             technical exceptions. Prefer using
     *             {@link #syncReadOptional(DataBroker, LogicalDatastoreType, InstanceIdentifier)}.
     */
    @Deprecated
    public static <T extends DataObject> Optional<T> syncReadOptionalAndTreatReadFailedExceptionAsAbsentOptional(
            DataBroker broker, LogicalDatastoreType datastoreType, InstanceIdentifier<T> path) {

        try (ReadOnlyTransaction tx = broker.newReadOnlyTransaction()) {
            return tx.read(datastoreType, path).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("ReadFailedException while reading data from {} store path {}; returning Optional.absent()",
                    datastoreType, path, e);
            return Optional.absent();
        }
    }

    public <T extends DataObject> void syncWrite(
            LogicalDatastoreType datastoreType, InstanceIdentifier<T> path, T data)
            throws TransactionCommitFailedException {
        syncWrite(broker, datastoreType, path, data);
    }

    public <T extends DataObject> void syncWrite(
            LogicalDatastoreType datastoreType, InstanceIdentifier<T> path, T data, int maxRetries)
            throws TransactionCommitFailedException {
        syncWrite(broker, datastoreType, path, data, maxRetries);
    }

    public static <T extends DataObject> void syncWrite(
            DataBroker broker, LogicalDatastoreType datastoreType, InstanceIdentifier<T> path, T data)
            throws TransactionCommitFailedException {
        syncWrite(broker, datastoreType, path, data, DEFAULT_RETRIES);
    }

    public static <T extends DataObject> void syncWrite(
            DataBroker broker, LogicalDatastoreType datastoreType, InstanceIdentifier<T> path, T data, int maxRetries)
            throws TransactionCommitFailedException {

        RetryingManagedNewTransactionRunner runner = new RetryingManagedNewTransactionRunner(broker, maxRetries);
        ListenableFutures.get(
                runner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.put(datastoreType, path, data, true)),
                SUBMIT_MAPPER);
    }

    public <T extends DataObject> void syncUpdate(
            LogicalDatastoreType datastoreType, InstanceIdentifier<T> path, T data)
            throws TransactionCommitFailedException {
        syncUpdate(broker, datastoreType, path, data);
    }

    public <T extends DataObject> void syncUpdate(
            LogicalDatastoreType datastoreType, InstanceIdentifier<T> path, T data, int maxRetries)
            throws TransactionCommitFailedException {
        syncUpdate(broker, datastoreType, path, data, maxRetries);
    }

    public static <T extends DataObject> void syncUpdate(
            DataBroker broker, LogicalDatastoreType datastoreType, InstanceIdentifier<T> path, T data)
            throws TransactionCommitFailedException {
        syncUpdate(broker, datastoreType, path, data, DEFAULT_RETRIES);
    }

    public static <T extends DataObject> void syncUpdate(
            DataBroker broker, LogicalDatastoreType datastoreType, InstanceIdentifier<T> path, T data, int maxRetries)
            throws TransactionCommitFailedException {
        RetryingManagedNewTransactionRunner runner = new RetryingManagedNewTransactionRunner(broker, maxRetries);
        ListenableFutures.get(
                runner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.merge(datastoreType, path, data, true)),
                SUBMIT_MAPPER);
    }

    public <T extends DataObject> void syncDelete(
            LogicalDatastoreType datastoreType, InstanceIdentifier<T> path)
            throws TransactionCommitFailedException {
        syncDelete(broker, datastoreType, path);
    }

    public <T extends DataObject> void syncDelete(
            LogicalDatastoreType datastoreType, InstanceIdentifier<T> path, int maxRetries)
            throws TransactionCommitFailedException {
        syncDelete(broker, datastoreType, path, maxRetries);
    }

    public static <T extends DataObject> void syncDelete(
            DataBroker broker, LogicalDatastoreType datastoreType, InstanceIdentifier<T> path)
            throws TransactionCommitFailedException {
        syncDelete(broker, datastoreType, path, DEFAULT_RETRIES);
    }

    public static <T extends DataObject> void syncDelete(
            DataBroker broker, LogicalDatastoreType datastoreType, InstanceIdentifier<T> path, int maxRetries)
            throws TransactionCommitFailedException {

        RetryingManagedNewTransactionRunner runner = new RetryingManagedNewTransactionRunner(broker, maxRetries);
        ListenableFutures.get(
                runner.callWithNewWriteOnlyTransactionAndSubmit(tx -> tx.delete(datastoreType, path)), SUBMIT_MAPPER);
    }

}
