/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.lockmanager.impl;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.lockmanager.LockManager;
import org.opendaylight.genius.lockmanager.LockManagerException;
import org.opendaylight.infrautils.utils.function.CheckedCallable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.UnlockInputBuilder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

@Singleton
public class LockManagerImpl implements LockManager {

    private final LockManagerService lockManagerService;

    @Inject
    public LockManagerImpl(LockManagerService lockManagerService) {
        this.lockManagerService = lockManagerService;
    }

    @Override
    public <T, E extends Exception> T runUnderLock(String lockName, CheckedCallable<T, E> callable)
            throws E, LockManagerException {
        check(lockManagerService.lock(new LockInputBuilder().setLockName(lockName).build()));
        try {
            return callable.call();
        } finally {
            check(lockManagerService.unlock(new UnlockInputBuilder().setLockName(lockName).build()));
        }
    }

    @Override
    public <E extends Exception> void runUnderLock(String lockName, CheckedRunnable<E> runnable)
            throws E, LockManagerException {
        runUnderLock(lockName, () -> {
            runnable.run();
            return null;
        });
    }

    // TODO This isn't perfect, but good enough; it will be removed in a later
    // refactoring when LockManagerImpl doesn't go through RPC
    private void check(Future<RpcResult<Void>> future) throws LockManagerException {
        RpcResult<Void> rpcResult;
        try {
            rpcResult = future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new LockManagerException("LockManagerService failed", e);
        }
        if (!rpcResult.isSuccessful()) {
            Collection<RpcError> rpcErrors = rpcResult.getErrors();
            if (!rpcErrors.isEmpty()) {
                RpcError firstRpcError = rpcErrors.iterator().next();
                if (firstRpcError.getCause() != null) {
                    throw new LockManagerException(firstRpcError.getMessage(), firstRpcError.getCause());
                } else {
                    throw new LockManagerException(firstRpcError.getMessage());
                }
            } else {
                throw new LockManagerException();
            }
        }
    }

}
