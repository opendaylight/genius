/*
 * Copyright © 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.networkutils.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.apache.aries.blueprint.annotation.service.Service;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.networkutils.VniUtils;
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
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Service(classes = VniUtils.class)
public class VniUtilsImpl implements VniUtils {

    private static final Logger LOG = LoggerFactory.getLogger(VniUtilsImpl.class);

    private final IdManagerService idManagerService;
    private final DataBroker dataBroker;
    private final NetworkConfig networkConfig;

    @Inject
    public VniUtilsImpl(NetworkConfig networkConfig, IdManagerService idManagerService,
                    @Reference DataBroker dataBroker) throws ReadFailedException {
        this.idManagerService = idManagerService;
        this.dataBroker = dataBroker;
        this.networkConfig = networkConfig;
        validateAndCreateVxlanVniPool();
    }

    @Override
    public Uint64 getVNI(String vniKey) throws ExecutionException, InterruptedException {
        AllocateIdInput getIdInput = new AllocateIdInputBuilder().setPoolName(NwConstants.ODL_VNI_POOL_NAME)
                .setIdKey(vniKey).build();
        Future<RpcResult<AllocateIdOutput>> result = idManagerService.allocateId(getIdInput);
        RpcResult<AllocateIdOutput> rpcResult = result.get();

        return rpcResult.isSuccessful() ? Uint64.valueOf(rpcResult.getResult().getIdValue()) : null;
    }

    @Override
    public void releaseVNI(String vniKey) throws ExecutionException, InterruptedException {
        ReleaseIdInput releaseIdInput = new ReleaseIdInputBuilder().setPoolName(NwConstants.ODL_VNI_POOL_NAME)
                .setIdKey(vniKey).build();
        RpcResult<ReleaseIdOutput> rpcResult = idManagerService.releaseId(releaseIdInput).get();
        if (!rpcResult.isSuccessful()) {
            LOG.warn("releaseVNI : Unable to release ID {} from OpenDaylight VXLAN VNI range pool. Error {}",
                    vniKey, rpcResult.getErrors());
        }
    }

    @Override
    public Optional<IdPool> getVxlanVniPool() throws ExecutionException, InterruptedException {
        return SingleTransactionDataBroker.syncReadOptional(dataBroker,
                LogicalDatastoreType.CONFIGURATION, buildIdPoolInstanceIdentifier(NwConstants.ODL_VNI_POOL_NAME));
    }

    private void validateAndCreateVxlanVniPool() throws ReadFailedException {
        /*
         * 1. If VNI Pool doesn't exist create it.
         * 2. If VNI Pool exists, but the range value is changed incorrectly
         * (say some allocations exist in the old range), we should NOT honor the new range .
         * Throw the WARN but continue running with Old range.
         * 3. If VNI Pool exists, but the given range is wider than the earlier one, we should
         * attempt to allocate with the new range again(TODO)
         */
        long lowLimit = NwConstants.VNI_DEFAULT_LOW_VALUE;
        long highLimit = NwConstants.VNI_DEFAULT_HIGH_VALUE;
        String configureVniRange = networkConfig.getOpendaylightVniRanges();
        if (configureVniRange != null) {
            String[] configureVniRangeSplit = configureVniRange.split(":");
            lowLimit = Long.parseLong(configureVniRangeSplit[0]);
            highLimit = Long.parseLong(configureVniRangeSplit[1]);
        }

        Optional<IdPool> existingIdPool = SingleTransactionDataBroker.syncReadOptional(dataBroker,
                LogicalDatastoreType.CONFIGURATION,
                buildIdPoolInstanceIdentifier(NwConstants.ODL_VNI_POOL_NAME));
        if (existingIdPool.isPresent()) {
            IdPool odlVniIdPool = existingIdPool.get();
            long currentStartLimit = odlVniIdPool.getAvailableIdsHolder().getStart().toJava();
            long currentEndLimit = odlVniIdPool.getAvailableIdsHolder().getEnd().toJava();

            if (lowLimit == currentStartLimit && highLimit == currentEndLimit) {
                LOG.debug("validateAndCreateVxlanVniPool : OpenDaylight VXLAN VNI range pool already exists "
                        + "with configured Range");
            } else {
                if (odlVniIdPool.getIdEntries() != null && odlVniIdPool.getIdEntries().size() != 0) {
                    LOG.warn("validateAndCreateVxlanVniPool : Some Allocation already exists with old Range. "
                            + "Cannot modify existing limit of OpenDaylight VXLAN VNI range pool");
                } else {
                    LOG.debug("validateAndCreateVxlanVniPool : No VNI's allocated from OpenDaylight VXLAN VNI range "
                            + "pool. Delete and re-create pool with new configured Range {}-{}",lowLimit, highLimit);
                    deleteOpenDaylightVniRangesPool();
                    createOpenDaylightVniRangesPool(lowLimit, highLimit);
                }
            }
        } else {
            createOpenDaylightVniRangesPool(lowLimit, highLimit);
        }
    }

    private void createOpenDaylightVniRangesPool(long lowLimit, long highLimit) {
        CreateIdPoolInput createPool = new CreateIdPoolInputBuilder()
                .setPoolName(NwConstants.ODL_VNI_POOL_NAME).setLow(lowLimit).setHigh(highLimit).build();
        try {
            Future<RpcResult<CreateIdPoolOutput>> result = idManagerService.createIdPool(createPool);
            if (result != null && result.get().isSuccessful()) {
                LOG.debug("createOpenDaylightVniRangesPool : Created OpenDaylight VXLAN VNI range pool {} "
                        + "with range {}-{}", NwConstants.ODL_VNI_POOL_NAME, lowLimit, highLimit);
            } else {
                LOG.error("createOpenDaylightVniRangesPool : Failed to create OpenDaylight VXLAN VNI range pool {} "
                        + "with range {}-{}", NwConstants.ODL_VNI_POOL_NAME, lowLimit, highLimit);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("createOpenDaylightVniRangesPool : Failed to create OpenDaylight VXLAN VNI range pool {} "
                    + "with range {}-{}", NwConstants.ODL_VNI_POOL_NAME, lowLimit, highLimit);
        }
    }

    private void deleteOpenDaylightVniRangesPool() {

        DeleteIdPoolInput deletePool = new DeleteIdPoolInputBuilder()
                .setPoolName(NwConstants.ODL_VNI_POOL_NAME).build();
        Future<RpcResult<DeleteIdPoolOutput>> result = idManagerService.deleteIdPool(deletePool);
        try {
            if (result != null && result.get().isSuccessful()) {
                LOG.debug("deleteOpenDaylightVniRangesPool : Deleted OpenDaylight VXLAN VNI range pool {} successfully",
                        NwConstants.ODL_VNI_POOL_NAME);
            } else {
                LOG.error("deleteOpenDaylightVniRangesPool : Failed to delete OpenDaylight VXLAN VNI range pool {} ",
                        NwConstants.ODL_VNI_POOL_NAME);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("deleteOpenDaylightVniRangesPool : Failed to delete OpenDaylight VXLAN VNI range pool {} ",
                    NwConstants.ODL_VNI_POOL_NAME, e);
        }
    }

    @VisibleForTesting
    InstanceIdentifier<IdPool> buildIdPoolInstanceIdentifier(String poolName) {
        InstanceIdentifier.InstanceIdentifierBuilder<IdPool> idPoolBuilder =
                InstanceIdentifier.builder(IdPools.class).child(IdPool.class, new IdPoolKey(poolName));
        return idPoolBuilder.build();
    }
}
