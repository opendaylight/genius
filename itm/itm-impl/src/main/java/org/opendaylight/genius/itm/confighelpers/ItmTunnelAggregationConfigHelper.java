/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.confighelpers;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.AllocateIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.itm.config.TunnelAggregation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
public class ItmTunnelAggregationConfigHelper {

    private static final Logger logger = LoggerFactory.getLogger(ItmTunnelAggregationConfigHelper.class);

    @Inject
    public ItmTunnelAggregationConfigHelper(IdManagerService idManager) {
        createGroupIdPool(idManager);
    }

    public static void createGroupIdPool(IdManagerService idManager) {
        CreateIdPoolInput createPool = new CreateIdPoolInputBuilder()
            .setPoolName(ITMConstants.VXLAN_GROUP_POOL_NAME)
            .setLow(ITMConstants.VXLAN_GROUP_POOL_START)
            .setHigh(ITMConstants.VXLAN_GROUP_POOL_END)
            .build();
        try {
            Future<RpcResult<Void>> result = idManager.createIdPool(createPool);
            if (result != null && result.get().isSuccessful()) {
                logger.debug("MULTIPLE_VxLAN_TUNNELS: created GroupIdPool");
            } else {
                logger.error("MULTIPLE_VxLAN_TUNNELS: unable to create GroupIdPool");
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("MULTIPLE_VxLAN_TUNNELS: failed to create pool for tunnel aggregation service", e);
        }
    }

    public static boolean isTunnelAggregationEnabled(ItmConfig itmConfig) {
        // Load balancing of VxLAN feature is guarded by a global configuration option in the ITM,
        // only when the feature is enabled, the logical tunnel interfaces should be created.
        boolean tunnelAggregationEnabled = false;
        List<TunnelAggregation> tunnelsConfig = itmConfig != null ? itmConfig.getTunnelAggregation() : null;
        if (tunnelsConfig != null) {
            for (TunnelAggregation tnlCfg : tunnelsConfig) {
                Class<? extends TunnelTypeBase> tunType = ItmUtils.getTunnelType(tnlCfg.getKey().getTunnelType());
                if (tunType.isAssignableFrom(TunnelTypeVxlan.class)) {
                    tunnelAggregationEnabled = tnlCfg.isEnabled();
                    logger.debug("MULTIPLE_VxLAN_TUNNELS: tunnelAggregationEnabled {}", tunnelAggregationEnabled);
                    break;
                }
            }
        }
        return tunnelAggregationEnabled;
    }

    public static void createEgressSelectGroup(BigInteger srcDpnId, String interfaceName,
                                               IdManagerService idManager,
                                               IMdsalApiManager mdsalManager) {
        long groupId = 0;
        AllocateIdInput getIdInput = new AllocateIdInputBuilder()
                .setPoolName(ITMConstants.VXLAN_GROUP_POOL_NAME).setIdKey(interfaceName).build();
        try {
            Future<RpcResult<AllocateIdOutput>> result = idManager.allocateId(getIdInput);
            RpcResult<AllocateIdOutput> rpcResult = result.get();
            groupId = rpcResult.getResult().getIdValue();
        } catch (NullPointerException | InterruptedException | ExecutionException e) {
            logger.trace("",e);
        }
        if (groupId == 0) {
            logger.debug("MULTIPLE_VxLAN_TUNNELS: group id was not allocated for {} srcDpnId {}",
                          interfaceName, srcDpnId);
            return;
        }
        logger.debug("MULTIPLE_VxLAN_TUNNELS: id {} allocated for the logical select group {} srcDpnId {}",
                      groupId, interfaceName, srcDpnId);
        List<Bucket> listBuckets = new ArrayList<>();

        Group group = MDSALUtil.buildGroup(groupId, interfaceName, GroupTypes.GroupSelect,
                                           MDSALUtil.buildBucketLists(listBuckets));
        mdsalManager.syncInstallGroup(srcDpnId, group, ITMConstants.DELAY_TIME_IN_MILLISECOND);
    }

}