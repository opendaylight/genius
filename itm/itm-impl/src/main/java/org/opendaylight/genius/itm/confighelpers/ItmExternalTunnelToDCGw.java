/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.monitor.params.MonitorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ItmExternalTunnelToDCGw implements TunnelWorkerInterface {
    private static final Logger LOG = LoggerFactory.getLogger(ItmExternalTunnelToDCGw.class);

    private IdManagerService idManagerService;
    private DataBroker dataBroker;
    private Class<? extends  TunnelTypeBase> tunnelType;
    private List<DPNTEPsInfo> cfgDpnList;
    private Integer monitorInterval;
    private boolean monitorEnabled;
    private Class<? extends TunnelMonitoringTypeBase> monitorProtocol;
    private IpAddress destinationIP;
    private List<MonitorConfig> monitorConfigList;

    public ItmExternalTunnelToDCGw(TunnelParameter tunnelParameter){

        idManagerService = tunnelParameter.getIdManagerService();
        dataBroker = tunnelParameter.getDataBroker();
        cfgDpnList = tunnelParameter.getCfgdDpnList();
        monitorInterval = tunnelParameter.getMonitorInterval();
        monitorEnabled = tunnelParameter.isMonitorEnabled();
        monitorProtocol  = tunnelParameter.getMonitorProtocol();
        tunnelType = tunnelParameter.getTunnelType();
        destinationIP = tunnelParameter.getDestinationIP();
        monitorConfigList = tunnelParameter.getMonitorConfig();

    }

    @Override
    public List<ListenableFuture<Void>> buildTunnelFutureList() {

            List<ListenableFuture<Void>> futures = new ArrayList<>();
            WriteTransaction t = dataBroker.newWriteOnlyTransaction();
//            monitorEnabled = ItmUtils.readMonitoringStateFromCache(dataBroker);

            if (null != cfgDpnList) {
                for (DPNTEPsInfo teps : cfgDpnList) {
                    // CHECK -- Assumption -- Only one End Point / Dpn for GRE/Vxlan Tunnels
                    TunnelEndPoints firstEndPt = teps.getTunnelEndPoints().get(0);
                    String interfaceName = firstEndPt.getInterfaceName();

                    String trunkInterfaceName = ItmUtils.getTrunkInterfaceName(idManagerService, interfaceName, firstEndPt.getIpAddress().getIpv4Address().getValue(), destinationIP.getIpv4Address().getValue(), tunnelType );
                    char[] subnetMaskArray = firstEndPt.getSubnetMask().getValue();
                    boolean useOfTunnel = ItmUtils.falseIfNull(firstEndPt.isOptionOfTunnel());
                    String subnetMaskStr = String.valueOf(subnetMaskArray);
                    SubnetUtils utils = new SubnetUtils(subnetMaskStr);
                    String dcGwyIpStr = String.valueOf(destinationIP.getValue());
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress gatewayIpObj = new IpAddress("0.0.0.0".toCharArray());
                    IpAddress gwyIpAddress = utils.getInfo().isInRange(dcGwyIpStr) ? gatewayIpObj : firstEndPt.getGwIpAddress();
                    LOG.debug(" Creating Trunk Interface with parameters trunk I/f Name - {}, parent I/f name - {}, source IP - {}, DC Gateway IP - {} gateway IP - {}", trunkInterfaceName, interfaceName, firstEndPt.getIpAddress(), destinationIP, gwyIpAddress);
                    Interface iface = ItmUtils.buildTunnelInterface(teps.getDPNID(), trunkInterfaceName,
                            String.format("%s %s", ItmUtils.convertTunnelTypetoString(tunnelType), "Trunk Interface"), true,
                            tunnelType, firstEndPt.getIpAddress(), destinationIP, gwyIpAddress, firstEndPt.getVLANID(), false, monitorEnabled,
                            ITMConstants.DEFAULT_MONITOR_PROTOCOL, monitorInterval, useOfTunnel,monitorConfigList);
                    LOG.debug(" Trunk Interface builder - {} ", iface);
                    InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(trunkInterfaceName);
                    LOG.debug(" Trunk Interface Identifier - {} ", trunkIdentifier);
                    LOG.trace(" Writing Trunk Interface to Config DS {}, {} ", trunkIdentifier, iface);
                    t.merge(LogicalDatastoreType.CONFIGURATION, trunkIdentifier, iface, true);
                    // update external_tunnel_list ds
                    InstanceIdentifier<ExternalTunnel> path = InstanceIdentifier.create(
                            ExternalTunnelList.class)
                            .child(ExternalTunnel.class, new ExternalTunnelKey(destinationIP.toString(), teps.getDPNID().toString(), tunnelType));
                    ExternalTunnel tnl = ItmUtils.buildExternalTunnel(  teps.getDPNID().toString(), destinationIP.toString(),
                            tunnelType, trunkInterfaceName);
                    t.merge(LogicalDatastoreType.CONFIGURATION, path, tnl, true);
                }

                futures.add(t.submit());
            }
            return futures;
    }
}
