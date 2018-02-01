/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelMetaUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev161113.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsTunnelConfigUpdateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsTunnelConfigUpdateHelper.class);

    public static List<ListenableFuture<Void>> updateConfiguration(DataBroker dataBroker, IdManagerService idManager,
                                                                   IMdsalApiManager mdsalApiManager,
                                                                   Interface interfaceNew, Interface interfaceOld,
                                                                   TunnelStateCache tunnelStateCache) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();

        // If any of the port attributes are modified, treat it as a delete and recreate scenario
        if (portAttributesModified(interfaceOld, interfaceNew)) {
            LOG.debug("port attributes modified, requires a delete and recreate of {} configuration", interfaceNew
                .getName());
            futures.addAll(OvsTunnelConfigRemoveHelper.removeConfiguration(dataBroker, interfaceOld, idManager,
                    mdsalApiManager, interfaceOld.getAugmentation(ParentRefs.class), tunnelStateCache));
            futures.addAll(OvsTunnelConfigAddHelper.addTunnelConfiguration(dataBroker, interfaceNew));
            return futures;
        }

        // If there is no operational state entry for the interface, treat it as create
        StateTunnelList stateTnlList = TunnelUtils.getTunnelFromOperationalDS(interfaceNew.getName(), dataBroker,
                tunnelStateCache);
        if (stateTnlList == null) {
            futures.addAll(OvsTunnelConfigAddHelper.addTunnelConfiguration(dataBroker, interfaceNew));
            return futures;
        }

        //WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        if (tunnelMonitoringAttributesModified(interfaceOld, interfaceNew)) {
            handleTunnelMonitorUpdates(futures, transaction, interfaceNew, interfaceOld, dataBroker);
            return futures;
        }

        // ITM Direct Tunnes Is this Reqired ??
        /*
        if (interfaceNew.isEnabled() != interfaceOld.isEnabled()) {
            handleInterfaceAdminStateUpdates(futures, transaction, interfaceNew, dataBroker, ifState);
        }
         */
        futures.add(transaction.submit());
        return futures;
    }

    private static boolean portAttributesModified(Interface interfaceOld, Interface interfaceNew) {
        ParentRefs parentRefsOld = interfaceOld.getAugmentation(ParentRefs.class);
        ParentRefs parentRefsNew = interfaceNew.getAugmentation(ParentRefs.class);
        if (checkAugmentations(parentRefsOld, parentRefsNew)) {
            return true;
        }

        IfL2vlan ifL2vlanOld = interfaceOld.getAugmentation(IfL2vlan.class);
        IfL2vlan ifL2vlanNew = interfaceNew.getAugmentation(IfL2vlan.class);
        if (checkAugmentations(ifL2vlanOld, ifL2vlanNew)) {
            return true;
        }

        IfTunnel ifTunnelOld = interfaceOld.getAugmentation(IfTunnel.class);
        IfTunnel ifTunnelNew = interfaceNew.getAugmentation(IfTunnel.class);
        if (checkAugmentations(ifTunnelOld,ifTunnelNew)) {
            if (!ifTunnelNew.getTunnelDestination().equals(ifTunnelOld.getTunnelDestination())
                    || !ifTunnelNew.getTunnelSource().equals(ifTunnelOld.getTunnelSource())
                    || (ifTunnelNew.getTunnelGateway() != null && ifTunnelOld.getTunnelGateway() != null
                    && !ifTunnelNew.getTunnelGateway().equals(ifTunnelOld.getTunnelGateway()))) {
                return true;
            }
        }

        return false;
    }

    private static boolean tunnelMonitoringAttributesModified(Interface interfaceOld, Interface interfaceNew) {
        IfTunnel ifTunnelOld = interfaceOld.getAugmentation(IfTunnel.class);
        IfTunnel ifTunnelNew = interfaceNew.getAugmentation(IfTunnel.class);
        if (checkAugmentations(ifTunnelOld, ifTunnelNew)) {
            return true;
        }
        return false;
    }

    /*
     * if the tunnel monitoring attributes have changed, handle it based on the tunnel type.
     * As of now internal vxlan tunnels use LLDP monitoring and external tunnels use BFD monitoring.
     */
    private static void handleTunnelMonitorUpdates(List<ListenableFuture<Void>> futures, WriteTransaction transaction,
                                                   Interface interfaceNew, Interface interfaceOld,
                                                   DataBroker dataBroker) {
        LOG.debug("tunnel monitoring attributes modified for interface {}", interfaceNew.getName());
        // update termination point on switch, if switch is connected
        OvsBridgeRefEntry bridgeRefEntry =
                TunnelMetaUtils.getBridgeReferenceForInterface(interfaceNew, dataBroker);
        IfTunnel ifTunnel = interfaceNew.getAugmentation(IfTunnel.class);
        if (SouthboundUtils.isMonitorProtocolBfd(ifTunnel)
                && TunnelMetaUtils.bridgeExists(bridgeRefEntry, dataBroker)) {
            SouthboundUtils.updateBfdParamtersForTerminationPoint(bridgeRefEntry.getOvsBridgeReference().getValue(),
                    interfaceNew.getAugmentation(IfTunnel.class),
                    interfaceNew.getName(), transaction);
        } else {
            // ITM Direct Tunnels Not required -- CHECK
        }
        futures.add(transaction.submit());
    }

    private static <T> boolean checkAugmentations(T oldAug, T newAug) {
        if ((oldAug != null && newAug == null) || (oldAug == null && newAug != null)) {
            return true;
        }

        if (newAug != null && oldAug != null && !newAug.equals(oldAug)) {
            return true;
        }

        return false;
    }
}

