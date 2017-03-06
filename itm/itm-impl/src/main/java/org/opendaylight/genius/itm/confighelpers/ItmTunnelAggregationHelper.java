/*
 * Copyright (c) 2017 Hewlett Packard Enterprise, Co. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.confighelpers;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.itm.config.TunnelAggregation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
public class ItmTunnelAggregationHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTunnelAggregationHelper.class);
    private static boolean tunnelAggregationEnabled;
    private final IInterfaceManager interfaceManager;

    @Inject
    public ItmTunnelAggregationHelper(final IInterfaceManager interfaceMngr, final ItmConfig itmConfig) {
        interfaceManager = interfaceMngr;
        initTunnelAggregationConfig(itmConfig);
    }

    public static boolean isTunnelAggregationEnabled() {
        return tunnelAggregationEnabled;
    }

    public void createLogicalTunnelSelectGroup(BigInteger srcDpnId, String interfaceName, int lporttag,
                                               IMdsalApiManager mdsalManager) {
        long groupId = interfaceManager.getLogicalTunnelSelectGroupId(lporttag);
        if (groupId == 0 || mdsalManager.groupExists(srcDpnId, groupId)) {
            return;
        }
        LOG.debug("MULTIPLE_VxLAN_TUNNELS: id {} allocated for the logical select group {} srcDpnId {}",
                      groupId, interfaceName, srcDpnId);
        Group group = MDSALUtil.buildGroup(groupId, interfaceName, GroupTypes.GroupSelect,
                                           MDSALUtil.buildBucketLists(Collections.emptyList()));
        mdsalManager.syncInstallGroup(srcDpnId, group, ITMConstants.DELAY_TIME_IN_MILLISECOND);
    }

    private void initTunnelAggregationConfig(ItmConfig itmConfig) {
        // Load balancing of VxLAN feature is guarded by a global configuration option in the ITM,
        // only when the feature is enabled, the logical tunnel interfaces should be created.
        boolean tunnelAggregationConfigEnabled = false;
        List<TunnelAggregation> tunnelsConfig = itmConfig != null ? itmConfig.getTunnelAggregation() : null;
        if (tunnelsConfig != null) {
            for (TunnelAggregation tnlCfg : tunnelsConfig) {
                Class<? extends TunnelTypeBase> tunType = ItmUtils.getTunnelType(tnlCfg.getKey().getTunnelType());
                if (tunType.isAssignableFrom(TunnelTypeVxlan.class)) {
                    tunnelAggregationConfigEnabled = tnlCfg.isEnabled();
                    LOG.info("MULTIPLE_VxLAN_TUNNELS: tunnelAggregationEnabled {}", tunnelAggregationConfigEnabled);
                    break;
                }
            }
        }
        tunnelAggregationEnabled = tunnelAggregationConfigEnabled;
    }

}
