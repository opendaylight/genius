/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.it;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.DeleteIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.DeleteIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdManagerUtils {
    private static final Logger LOG = LoggerFactory.getLogger(IdManagerUtils.class);
    private final IdManagerService idManager;
    private final String idPool;
    private final long poolStart;
    private final long poolSize;
    public static final int INVALID_ID = 0;

    IdManagerUtils(final IdManagerService idManager, final String idPool,
                   final long poolStart, final long poolSize) {
        this.idManager = idManager;
        this.idPool = idPool;
        this.poolStart = poolStart;
        this.poolSize = poolSize;
    }

    public Boolean createIdPool() {
        CreateIdPoolInput createPool = new CreateIdPoolInputBuilder()
                .setPoolName(idPool)
                .setLow(poolStart)
                .setHigh(poolSize)
                .build();

        try {
            Future<RpcResult<Void>> result = idManager.createIdPool(createPool);
            RpcResult<Void> rpcResult = result.get();
            if (rpcResult.isSuccessful()) {
                LOG.info("Successfully created Id Pool");
                return true;
            } else {
                LOG.warn("RPC Call to create Id Pool {} returned with Errors {}", idPool, rpcResult.getErrors());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception when creating Id Pool {}", idPool, e);
        }
        return false;
    }

    public Boolean deleteIdPool() {
        DeleteIdPoolInput builder = new DeleteIdPoolInputBuilder()
                .setPoolName(idPool)
                .build();

        try {
            Future<RpcResult<Void>> result = idManager.deleteIdPool(builder);
            RpcResult<Void> rpcResult = result.get();
            if (rpcResult.isSuccessful()) {
                LOG.info("Successfully deleted Id Pool");
                return true;
            } else {
                LOG.warn("RPC Call to delete Id Pool {} returned with Errors {}", idPool, rpcResult.getErrors());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception when deleting Id Pool {}", idPool, e);
        }
        return false;
    }

    public int allocateId(final String idKey) {
        AllocateIdInput getIdInput = new AllocateIdInputBuilder()
                .setPoolName(idPool)
                .setIdKey(idKey).build();

        try {
            Future<RpcResult<AllocateIdOutput>> result = idManager.allocateId(getIdInput);
            RpcResult<AllocateIdOutput> rpcResult = result.get();
            if (rpcResult.isSuccessful()) {
                LOG.info("Successfully allocated Unique Id {}", rpcResult.getResult().getIdValue().intValue());
                return rpcResult.getResult().getIdValue().intValue();
            } else {
                LOG.warn("RPC Call to allocate Unique Id returned with Errors {}", rpcResult.getErrors());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception when getting Unique Id for key {}", idKey, e);
        }
        return INVALID_ID;
    }

    public Boolean releaseId(final String idKey) {
        ReleaseIdInput idInput = new ReleaseIdInputBuilder()
                .setPoolName(idPool)
                .setIdKey(idKey).build();
        try {
            Future<RpcResult<Void>> result = idManager.releaseId(idInput);
            RpcResult<Void> rpcResult = result.get();
            if (rpcResult.isSuccessful()) {
                LOG.info("Successfully released Unique Id");
                return true;
            } else {
                LOG.warn("RPC Call to release Id {} with Key {} returned with Errors {}",
                        idKey, rpcResult.getErrors());
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception when releasing Id for key {}", idKey, e);
        }
        return false;
    }
}
