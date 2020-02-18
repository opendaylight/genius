/*
 * Copyright © 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.networkutils.impl;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.apache.aries.blueprint.annotation.service.Service;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.networkutils.RDUtils;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.DeleteIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.DeleteIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.DeleteIdPoolOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.ReleaseIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.networkutils.config.rev181129.NetworkConfig;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Service(classes = RDUtils.class)
public class RDUtilsImpl implements RDUtils {

    private static final Logger LOG = LoggerFactory.getLogger(RDUtilsImpl.class);

    private final IdManagerService idManagerService;
    private final DataBroker dataBroker;
    private final NetworkConfig networkConfig;

    @Inject
    public RDUtilsImpl(NetworkConfig networkConfig, IdManagerService idManagerService,
                       @Reference DataBroker dataBroker) throws ReadFailedException {
        this.idManagerService = idManagerService;
        this.dataBroker = dataBroker;
        this.networkConfig = networkConfig;
        validateAndCreateRDPool();
    }

    /*
     * if the base RD is 100:1 and generated idValue is 100, this fn returns 100:101
     * if the base RD is 200:2000 and generated RD value is 64000, this fn should return 201:2465
     *
     *   if RD is x:y
     *   y can go till 65535(RD_MAX_VALUE_FIELD) and then x is incremented with y reset to 0
     */

    public String convertIdValuetoRD(long idValue) {
        String configuredRDStartValue = networkConfig.getOpendaylightRdStartValue();
        String[] configureRDSplit = NwConstants.RD_DEFAULT_LOW_VALUE.split(":");
        if (configuredRDStartValue != null) {
            configureRDSplit = configuredRDStartValue.split(":");
        }
        long baseAsNum = Long.parseLong(configureRDSplit[0]);
        long baseValue = Long.parseLong(configureRDSplit[1]);
        baseAsNum = baseAsNum + (baseValue + idValue) / NwConstants.RD_MAX_VALUE_FIELD ;
        baseValue = (baseValue + idValue) % NwConstants.RD_MAX_VALUE_FIELD ;

        return String.valueOf(baseAsNum) + ":" + String.valueOf(baseValue);
    }

    @Override
    public String getRD(String rdKey) throws ExecutionException, InterruptedException {
        AllocateIdInput getIdInput = new AllocateIdInputBuilder().setPoolName(NwConstants.ODL_RD_POOL_NAME)
                .setIdKey(rdKey).build();
        Future<RpcResult<AllocateIdOutput>> result = idManagerService.allocateId(getIdInput);
        RpcResult<AllocateIdOutput> rpcResult = result.get();
        if (rpcResult.isSuccessful()) {
            String rd = convertIdValuetoRD(rpcResult.getResult().getIdValue().toJava());
            return rd;
        }
        return null;
    }

    @Override
    public void releaseRD(String rdKey) throws ExecutionException, InterruptedException {
        ReleaseIdInput releaseIdInput = new ReleaseIdInputBuilder().setPoolName(NwConstants.ODL_RD_POOL_NAME)
                .setIdKey(rdKey).build();
        RpcResult<ReleaseIdOutput> rpcResult = idManagerService.releaseId(releaseIdInput).get();
        if (!rpcResult.isSuccessful()) {
            LOG.warn("releaseRD : Unable to release ID {} from OpenDaylight RD pool. Error {}",
                    rdKey, rpcResult.getErrors());
        }
    }

    @Override
    public Optional<IdPool> getRDPool() throws ExecutionException, InterruptedException {
        return SingleTransactionDataBroker.syncReadOptional(dataBroker,
                LogicalDatastoreType.CONFIGURATION, buildIdPoolInstanceIdentifier(NwConstants.ODL_RD_POOL_NAME));
    }

    private void validateAndCreateRDPool() throws ReadFailedException {
        long lowLimit = 0L;
        Uint32 highConfig = networkConfig.getOpendaylightRdCount();
        long highLimit = highConfig == null ? 0 : highConfig.toJava();
        if (highLimit == 0L) {
            highLimit = NwConstants.RD_DEFAULT_COUNT;
        }
        Optional<IdPool> existingIdPool = SingleTransactionDataBroker.syncReadOptional(dataBroker,
                LogicalDatastoreType.CONFIGURATION,
                buildIdPoolInstanceIdentifier(NwConstants.ODL_RD_POOL_NAME));
        if (existingIdPool.isPresent()) {
            IdPool odlRDPool = existingIdPool.get();
            long currentStartLimit = odlRDPool.getAvailableIdsHolder().getStart().toJava();
            long currentEndLimit = odlRDPool.getAvailableIdsHolder().getEnd().toJava();

            if (lowLimit == currentStartLimit && highLimit == currentEndLimit) {
                LOG.debug("validateAndCreateRDPool : OpenDaylight RD pool already exists "
                        + "with configured Range");
            } else {
                if (odlRDPool.getIdEntries() != null && odlRDPool.getIdEntries().size() != 0) {
                    LOG.warn("validateAndCreateRDPool : Some Allocation already exists with old Range. "
                            + "Cannot modify existing limit of OpenDaylight RD pool");
                } else {
                    LOG.debug("validateAndCreateRDPool : No RDs allocated from OpenDaylight RD pool "
                            + "Delete and re-create pool with new configured Range {}-{}",lowLimit, highLimit);
                    deleteOpenDaylightRDPool();
                    createOpenDaylightRDPool(lowLimit, highLimit);
                }
            }
        } else {
            createOpenDaylightRDPool(lowLimit, highLimit);
        }
    }

    private void createOpenDaylightRDPool(long lowLimit, long highLimit) {
        CreateIdPoolInput createPool = new CreateIdPoolInputBuilder()
                .setPoolName(NwConstants.ODL_RD_POOL_NAME).setLow(lowLimit).setHigh(highLimit).build();
        try {
            Future<RpcResult<CreateIdPoolOutput>> result = idManagerService.createIdPool(createPool);
            if (result != null && result.get().isSuccessful()) {
                LOG.debug("createOpenDaylightRDPool : Created OpenDaylight RD pool {} "
                        + "with range {}-{}", NwConstants.ODL_RD_POOL_NAME, lowLimit, highLimit);
            } else {
                LOG.error("createOpenDaylightRDPool : Failed to create OpenDaylight RD pool {} "
                        + "with range {}-{}", NwConstants.ODL_RD_POOL_NAME, lowLimit, highLimit);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("createOpenDaylightRDPool : Failed to create OpenDaylight RD pool {} "
                    + "with range {}-{}", NwConstants.ODL_RD_POOL_NAME, lowLimit, highLimit);
        }
    }

    private void deleteOpenDaylightRDPool() {

        DeleteIdPoolInput deletePool = new DeleteIdPoolInputBuilder()
                .setPoolName(NwConstants.ODL_RD_POOL_NAME).build();
        Future<RpcResult<DeleteIdPoolOutput>> result = idManagerService.deleteIdPool(deletePool);
        try {
            if (result != null && result.get().isSuccessful()) {
                LOG.debug("deleteOpenDaylightRDPool : Deleted OpenDaylight RD pool {} successfully",
                        NwConstants.ODL_RD_POOL_NAME);
            } else {
                LOG.error("deleteOpenDaylightRDPool : Failed to delete OpenDaylight RD pool {} ",
                        NwConstants.ODL_RD_POOL_NAME);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("deleteOpenDaylightRDPool : Failed to delete OpenDaylight RD range pool {} ",
                    NwConstants.ODL_RD_POOL_NAME, e);
        }
    }

    @VisibleForTesting
    InstanceIdentifier<IdPool> buildIdPoolInstanceIdentifier(String poolName) {
        InstanceIdentifier.InstanceIdentifierBuilder<IdPool> idPoolBuilder =
                InstanceIdentifier.builder(IdPools.class).child(IdPool.class, new IdPoolKey(poolName));
        return idPoolBuilder.build();
    }
}
