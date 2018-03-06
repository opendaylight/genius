/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import com.google.common.base.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.cache.InstanceIdDataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TepTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches StateTunnelList objects.
 *
 * @author Thomas Pantelis
 */
@Singleton
public class TunnelStateCache extends InstanceIdDataObjectCache<StateTunnelList> {

    private final static Logger LOG = LoggerFactory.getLogger(TunnelStateCache.class);

    private final DataBroker dataBroker;
    @Inject
    public TunnelStateCache(DataBroker dataBroker, CacheProvider cacheProvider) {
        super(StateTunnelList.class, dataBroker, LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(TunnelsState.class).child(StateTunnelList.class).build(), cacheProvider);
        this.dataBroker = dataBroker;
    }

    public boolean isInternalBasedOnState(String tunnelName) {
        StateTunnelList stateTunnelList = getTunnelFromOperationalDS(tunnelName);
        return stateTunnelList != null && TepTypeInternal.class.equals(stateTunnelList.getDstInfo().getTepDeviceType());
    }

    public long getNodeConnectorIdFromInterface(String interfaceName) {
        StateTunnelList stateTnl = getTunnelFromOperationalDS(interfaceName);
        if (stateTnl != null) {
            return Long.parseLong(stateTnl.getPortNumber());
        } else {
            return ITMConstants.INVALID_PORT_NO;
        }
    }

    private StateTunnelList getTunnelFromOperationalDS(String tunnelName) {

        try {
            Optional<StateTunnelList> stateTunnelList =
                    this.get(getStateTunnelListIdentifier(new StateTunnelListKey(tunnelName)));
            if (stateTunnelList.isPresent()) {
                return stateTunnelList.get();
            }
        } catch (ReadFailedException exception) {
            LOG.debug("read failed for tunnel {}, while reading TunnelStateCache", tunnelName);
        }
        InstanceIdentifier<StateTunnelList> stateTnlII =
                ItmUtils.buildStateTunnelListId(new StateTunnelListKey(tunnelName));

        Optional<StateTunnelList> tnlStateOptional =
                ItmUtils.read(LogicalDatastoreType.OPERATIONAL, stateTnlII, dataBroker);
        if (!tnlStateOptional.isPresent()) {
            return null;
        }
        return tnlStateOptional.get();
    }

    private InstanceIdentifier<StateTunnelList> getStateTunnelListIdentifier(StateTunnelListKey key) {
        return InstanceIdentifier.builder(TunnelsState.class).child(StateTunnelList.class, key).build();
    }
}
