/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.felix.service.command.CommandSession;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.utils.cache.DataStoreCache;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.MDSALDataStoreUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBfd;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeLldp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorIntervalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TepTypeInternal;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelOperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZonesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.opendaylight.genius.interfacemgr.util.OperationalIfmUtil;


public class TepCommandHelper {

    private static final Logger LOG = LoggerFactory.getLogger(TepCommandHelper.class);
    private DataBroker dataBroker;
    static int check = 0;
    static short flag = 0;
    /*
     * boolean flag add_or_delete --- can be set to true if the last called tep
     * command is Tep-add else set to false when Tep-delete is called
     * tepCommandHelper object is created only once in session initiated
     */
    final Map<String, Map<SubnetObject, List<Vteps>>> tZones = new HashMap<>();
    private List<Subnets> subnetList = new ArrayList<>();
    private List<TransportZone> tZoneList = new ArrayList<>();
    private List<Vteps> vtepDelCommitList = new ArrayList<>();
    private IInterfaceManager interfaceManager;

    // private List<InstanceIdentifier<? extends DataObject>> vtepPaths = new
    // ArrayList<>();


    public TepCommandHelper(final DataBroker broker) {
        this.dataBroker = broker;
    }


    public void setInterfaceManager(IInterfaceManager interfaceManager) {
        this.interfaceManager = interfaceManager;
    }

    public void createLocalCache(BigInteger dpnId, String portName, Integer vlanId, String ipAddress,
                                 String subnetMask, String gatewayIp, String transportZone, CommandSession session) throws TepException{

        check++;
        IpAddress ipAddressObj = null;
        IpAddress gatewayIpObj = null;
        IpPrefix subnetMaskObj = null;
        VtepsKey vtepkey = new VtepsKey(dpnId, portName);
        try {
            ipAddressObj = new IpAddress(ipAddress.toCharArray());
            gatewayIpObj = new IpAddress("0.0.0.0".toCharArray());
            if (gatewayIp != null) {
                gatewayIpObj = new IpAddress(gatewayIp.toCharArray());
            } else {
                LOG.debug("gateway is null");
            }
        } catch (Exception e) {
            handleError("Invalid IpAddress. Expected: 1.0.0.0 to 254.255.255.255", session);
            return;
        }
        try {
            subnetMaskObj = new IpPrefix(subnetMask.toCharArray());
        } catch (Exception e) {
            handleError("Invalid Subnet Mask. Expected: 0.0.0.0/0 to 255.255.255.255/32", session);
            return;
        }

        if (!validateIPs(ipAddress, subnetMask, gatewayIp)) {
            handleError("IpAddress and gateWayIp should belong to the subnet provided", session);
            return;
        }

        if (checkTepPerTzPerDpn(transportZone, dpnId)) {
            if(session  != null) {
                session.getConsole().println("Only one end point per transport Zone per Dpn is allowed");
            }
            return;
        }
        Vteps vtepCli = new VtepsBuilder().setDpnId(dpnId).setIpAddress(ipAddressObj).setKey(vtepkey)
                .setPortname(portName).build();
        validateForDuplicates(vtepCli, transportZone);

        SubnetsKey subnetsKey = new SubnetsKey(subnetMaskObj);
        SubnetObject subObCli = new SubnetObject(gatewayIpObj, subnetsKey, subnetMaskObj, vlanId);
        if (tZones.containsKey(transportZone)) {
            Map<SubnetObject, List<Vteps>> subVtepMapTemp = tZones.get(transportZone);
            if (subVtepMapTemp.containsKey(subObCli)) { // if Subnet exists
                List<Vteps> vtepListTemp = subVtepMapTemp.get(subObCli);
                if (vtepListTemp.contains(vtepCli)) {
                    // do nothing
                } else {
                    vtepListTemp.add(vtepCli);
                }
            } else { // subnet doesnt exist
                if (checkExistingSubnet(subVtepMapTemp, subObCli)) {
                    if(session != null) {
                        session.getConsole().println("subnet with subnet mask " + subObCli.get_key() + "already exists");
                    }
                    return;
                }
                List<Vteps> vtepListTemp = new ArrayList<>();
                vtepListTemp.add(vtepCli);
                subVtepMapTemp.put(subObCli, vtepListTemp);
            }
        } else {
            List<Vteps> vtepListTemp = new ArrayList<>();
            vtepListTemp.add(vtepCli);
            Map<SubnetObject, List<Vteps>> subVtepMapTemp = new HashMap<>();
            subVtepMapTemp.put(subObCli, vtepListTemp);
            tZones.put(transportZone, subVtepMapTemp);
        }
    }

    private boolean validateIPs(String ipAddress, String subnetMask, String gatewayIp) {
        SubnetUtils utils = new SubnetUtils(subnetMask);
        if ((utils.getInfo().isInRange(ipAddress)) && ((gatewayIp == null) || (utils.getInfo().isInRange(gatewayIp)))) {
            return true;
        } else {
            LOG.trace("InValid IP");
            return false;
        }
    }

    /**
     * Validate for duplicates.
     *
     * @param inputVtep
     *            the input vtep
     * @param transportZone
     *            the transport zone
     */
    public void validateForDuplicates(Vteps inputVtep, String transportZone) {
        Map<String, TransportZone> tZoneMap = getAllTransportZonesAsMap();

        boolean isConfiguredTepGreType = isGreTunnelType(transportZone, tZoneMap);
        // Checking for duplicates in local cache
        for (String tZ : tZones.keySet()) {
            boolean isGreType = isGreTunnelType(tZ, tZoneMap);
            Map<SubnetObject, List<Vteps>> subVtepMapTemp = tZones.get(tZ);
            for (SubnetObject subOb : subVtepMapTemp.keySet()) {
                List<Vteps> vtepList = subVtepMapTemp.get(subOb);
                validateForDuplicateAndSingleGreTep(inputVtep, isConfiguredTepGreType, isGreType, vtepList);
            }
        }
        // Checking for duplicates in config DS
        for (TransportZone tZ : tZoneMap.values()) {
            boolean isGreType = false;
            if (tZ.getTunnelType().equals(TunnelTypeGre.class)) {
                isGreType = true;
            }
            for (Subnets sub : ItmUtils.emptyIfNull(tZ.getSubnets())) {
                List<Vteps> vtepList = sub.getVteps();
                validateForDuplicateAndSingleGreTep(inputVtep, isConfiguredTepGreType, isGreType, vtepList);
            }
        }
    }

    private void validateForDuplicateAndSingleGreTep(Vteps inputVtep, boolean isConfiguredTepGreType, boolean isGreType,
                                                     List<Vteps> vtepList) {
        if (ItmUtils.isEmpty(vtepList)) {
            return;
        }
        if (vtepList.contains(inputVtep)) {
            Preconditions.checkArgument(false, "VTEP already exists");
        }
        BigInteger dpnId = inputVtep.getDpnId();
        if (isConfiguredTepGreType && isGreType) {
            for (Vteps vtep : vtepList) {
                if (vtep.getDpnId().equals(dpnId)) {
                    String errMsg = new StringBuilder("DPN [").append(dpnId)
                            .append("] already configured with GRE TEP. Mutiple GRE TEP's on a single DPN are not allowed.")
                            .toString();
                    Preconditions.checkArgument(false, errMsg);
                }
            }
        }
    }

    /**
     * Gets all transport zones as map.
     *
     * @return all transport zones as map
     */
    private Map<String, TransportZone> getAllTransportZonesAsMap() {
        TransportZones tZones = getAllTransportZones();
        Map<String, TransportZone> tZoneMap = new HashMap<>();
        if( null != tZones) {
            for (TransportZone tzone : ItmUtils.emptyIfNull(tZones.getTransportZone())) {
                tZoneMap.put(tzone.getZoneName(), tzone);
            }
        }
        return tZoneMap;
    }

    /**
     * Checks if is gre tunnel type.
     *
     * @param tZoneName
     *            the zone name
     * @param tZoneMap
     *            the zone map
     * @return true, if is gre tunnel type
     */
    private boolean isGreTunnelType(String tZoneName, Map<String, TransportZone> tZoneMap) {
        TransportZone tzone = tZoneMap.get(tZoneName);
        /*
        if (tzone != null && StringUtils.equalsIgnoreCase(ITMConstants.TUNNEL_TYPE_GRE, tzone.getTunnelType())) {
            return true;
        }
        */
        if( (tzone != null) && (tzone.getTunnelType()).equals(TunnelTypeGre.class) ) {
            return true;
        }
        return false;
    }

    /**
     * Gets the transport zone.
     *
     * @param tzone
     *            the tzone
     * @return the transport zone
     */
    public TransportZone getTransportZone(String tzone) {
        InstanceIdentifier<TransportZone> tzonePath = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(tzone)).build();
        Optional<TransportZone> tZoneOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tzonePath,
                dataBroker);
        if (tZoneOptional.isPresent()) {
            return tZoneOptional.get();
        }
        return null;
    }

    /**
     * Gets the transport zone from config ds.
     *
     * @param tzone
     *            the tzone
     * @return the transport zone
     */
    public TransportZone getTransportZoneFromConfigDS(String tzone) {
        InstanceIdentifier<TransportZone> tzonePath = InstanceIdentifier.builder(TransportZones.class)
                .child(TransportZone.class, new TransportZoneKey(tzone)).build();
        Optional<TransportZone> tZoneOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tzonePath,
                dataBroker);
        if (tZoneOptional.isPresent()) {
            return tZoneOptional.get();
        }
        return null;
    }

    /**
     * Gets all transport zones.
     *
     * @return all transport zones
     */
    public TransportZones getAllTransportZones() {
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> tZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker);
        if (tZonesOptional.isPresent()) {
            return tZonesOptional.get();
        }
        return null;
    }

    public boolean checkExistingSubnet(Map<SubnetObject, List<Vteps>> subVtepMapTemp, SubnetObject subObCli) {
        for (SubnetObject subOb : subVtepMapTemp.keySet()) {
            if (subOb.get_key().equals(subObCli.get_key())) {
                if (!(subOb.get_vlanId().equals(subObCli.get_vlanId())))
                    return true;
                if (!(subOb.get_gatewayIp().equals(subObCli.get_gatewayIp())))
                    return true;
            }
        }
        return false;
    }

    public boolean checkTepPerTzPerDpn(String tzone, BigInteger dpnId) {
        // check in local cache
        if (tZones.containsKey(tzone)) {
            Map<SubnetObject, List<Vteps>> subVtepMapTemp = tZones.get(tzone);
            for (SubnetObject subOb : subVtepMapTemp.keySet()) {
                List<Vteps> vtepList = subVtepMapTemp.get(subOb);
                for (Vteps vtep : vtepList)
                    if (vtep.getDpnId().equals(dpnId))
                        return true;
            }
        }

        // check in DS
        InstanceIdentifier<TransportZone> tzonePath =
                InstanceIdentifier.builder(TransportZones.class)
                        .child(TransportZone.class, new TransportZoneKey(tzone)).build();
        Optional<TransportZone> tZoneOptional =
                ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tzonePath, dataBroker);
        if (tZoneOptional.isPresent()) {
            TransportZone tz = tZoneOptional.get();
            if (tz.getSubnets() == null || tz.getSubnets().isEmpty())
                return false;
            for (Subnets sub : tz.getSubnets()) {
                if (sub.getVteps() == null || sub.getVteps().isEmpty())
                    continue;
                for (Vteps vtep : sub.getVteps()) {
                    if (vtep.getDpnId().equals(dpnId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void buildTeps() {
        TransportZones tZonesBuilt = null;
        TransportZone tZone = null;
        try {
            LOG.debug("no of teps added" + check);
            if (tZones != null || !tZones.isEmpty()) {
                tZoneList = new ArrayList<>();
                for (String tZ : tZones.keySet()) {
                    LOG.debug("tZones" + tZ);
                    subnetList = new ArrayList<>();
                    Map<SubnetObject, List<Vteps>> subVtepMapTemp = tZones.get(tZ);
                    for (SubnetObject subOb : subVtepMapTemp.keySet()) {
                        LOG.debug("subnets" + subOb.get_prefix());
                        List<Vteps> vtepList = subVtepMapTemp.get(subOb);
                        Subnets subnet =
                                new SubnetsBuilder().setGatewayIp(subOb.get_gatewayIp())
                                        .setKey(subOb.get_key()).setPrefix(subOb.get_prefix())
                                        .setVlanId(subOb.get_vlanId()).setVteps(vtepList).build();
                        subnetList.add(subnet);
                        LOG.debug("vteps" + vtepList);
                    }
                    InstanceIdentifier<TransportZone> tZonepath =
                            InstanceIdentifier.builder(TransportZones.class)
                                    .child(TransportZone.class, new TransportZoneKey(tZ)).build();
                    Optional<TransportZone> tZoneOptional =
                            ItmUtils.read(LogicalDatastoreType.CONFIGURATION, tZonepath, dataBroker);
                    LOG.debug("read container from DS");
                    if (tZoneOptional.isPresent()) {
                        TransportZone tzoneFromDs = tZoneOptional.get();
                        LOG.debug("read tzone container" + tzoneFromDs.toString());
                        if (tzoneFromDs.getTunnelType() == null
                                || (tzoneFromDs.getTunnelType()).equals(TunnelTypeVxlan.class)) {
                            tZone =
                                    new TransportZoneBuilder().setKey(new TransportZoneKey(tZ))
                                            .setTunnelType(TunnelTypeVxlan.class).setSubnets(subnetList)
                                            .setZoneName(tZ).build();
                        } else if ((tzoneFromDs.getTunnelType()).equals(TunnelTypeGre.class)) {
                            tZone =
                                    new TransportZoneBuilder().setKey(new TransportZoneKey(tZ))
                                            .setTunnelType(TunnelTypeGre.class).setSubnets(subnetList)
                                            .setZoneName(tZ).build();
                        }
                    } else {
                        tZone =
                                new TransportZoneBuilder().setKey(new TransportZoneKey(tZ))
                                        .setTunnelType(TunnelTypeVxlan.class).setSubnets(subnetList).setZoneName(tZ)
                                        .build();
                    }
                    LOG.debug("tzone object" + tZone);
                    tZoneList.add(tZone);
                }
                tZonesBuilt = new TransportZonesBuilder().setTransportZone(tZoneList).build();
                InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
                LOG.debug("InstanceIdentifier" + path);
                ItmUtils.asyncUpdate(LogicalDatastoreType.CONFIGURATION, path, tZonesBuilt, dataBroker,
                        ItmUtils.DEFAULT_CALLBACK);
                LOG.debug("wrote to Config DS" + tZonesBuilt);
                tZones.clear();
                tZoneList.clear();
                subnetList.clear();
                LOG.debug("Everything cleared");
            } else {
                LOG.debug("NO vteps were configured");
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    public void showTeps(boolean monitorEnabled, int monitorInterval, CommandSession session) throws TepException {
        boolean flag = false;
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> tZonesOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker);
        if (tZonesOptional.isPresent()) {
            TransportZones tZones = tZonesOptional.get();
            if(tZones.getTransportZone() == null || tZones.getTransportZone().isEmpty())
            {
                handleError("No teps configured", session);
                return;
            }
            List<String> result = new ArrayList<>();
            result.add(String.format("Tunnel Monitoring (for VXLAN tunnels): %s", (monitorEnabled ? "On" : "Off")));
            result.add(String.format("Tunnel Monitoring Interval (for VXLAN tunnels): %d", monitorInterval));
            result.add(System.lineSeparator());
            result.add(String.format("%-16s  %-16s  %-16s  %-12s  %-12s %-12s %-16s %-12s", "TransportZone", "TunnelType", "SubnetMask",
                    "GatewayIP", "VlanID", "DpnID", "IPAddress", "PortName"));
            result.add("------------------------------------------------------------------------------------------------------------------------------");
            for (TransportZone tZ : tZones.getTransportZone()) {
                if (tZ.getSubnets() == null || tZ.getSubnets().isEmpty()) {
                    LOG.error("Transport Zone " + tZ.getZoneName() + "has no subnets");
                    continue;
                }
                for (Subnets sub : tZ.getSubnets()) {
                    if (sub.getVteps() == null || sub.getVteps().isEmpty()) {
                        LOG.error("Transport Zone " + tZ.getZoneName() + "subnet " + sub.getPrefix() + "has no vteps");
                        continue;
                    }
                    for (Vteps vtep : sub.getVteps()) {
                        flag = true;
                        String strTunnelType ;
                        if( (tZ.getTunnelType()).equals(TunnelTypeGre.class) )
                            strTunnelType = ITMConstants.TUNNEL_TYPE_GRE ;
                        else
                            strTunnelType = ITMConstants.TUNNEL_TYPE_VXLAN ;
                        result.add(String.format("%-16s  %-16s  %-16s  %-12s  %-12s %-12s %-16s %-12s", tZ.getZoneName(), strTunnelType, sub
                                .getPrefix().getIpv4Prefix().getValue(), sub.getGatewayIp().getIpv4Address()
                                .getValue(), sub.getVlanId().toString(), vtep.getDpnId().toString(), vtep
                                .getIpAddress().getIpv4Address().getValue(), vtep.getPortname()));
                    }
                }
            }
            if (flag == true) {
                for (String p : result) {
                    if(session != null) {
                        session.getConsole().println(p);
                    }
                }
            } else if (session != null) {
                    session.getConsole().println("No teps to display");
            }
        } else if(session != null){
            session.getConsole().println("No teps configured");
        }
    }

public void showCache(String cacheName) {

        if( !DataStoreCache.isCacheValid(cacheName)) {
            System.out.println( " " + cacheName + " is not a valid Cache Name ") ;
            return ;
        }
        List<Object> keys = null ;
        keys = DataStoreCache.getKeys(cacheName);
        if( keys != null && !keys.isEmpty()) {
            System.out.println( "Dumping the data in cache for " + cacheName ) ;
            for( Object key : keys ) {
                System.out.println( " KEY:  " + key + " Value: " + DataStoreCache.get(cacheName, key) ) ;
                System.out.println() ;
            }

        }else
            System.out.println( "No data in cache for " + cacheName ) ;
        }

    public void deleteVtep(BigInteger dpnId, String portName, Integer vlanId, String ipAddress, String subnetMask,
                           String gatewayIp, String transportZone,CommandSession session) throws TepException {

        IpAddress ipAddressObj = null;
        IpAddress gatewayIpObj = null;
        IpPrefix subnetMaskObj = null;
        VtepsKey vtepkey = new VtepsKey(dpnId, portName);
        try {
            ipAddressObj = new IpAddress(ipAddress.toCharArray());
            gatewayIpObj = new IpAddress("0.0.0.0".toCharArray());
            if (gatewayIp != null) {
                gatewayIpObj = new IpAddress(gatewayIp.toCharArray());
            } else {
                LOG.debug("gateway is null");
            }
        } catch (Exception e) {
            handleError("Invalid IpAddress. Expected: 1.0.0.0 to 254.255.255.255", session);
            return;
        }
        try {
            subnetMaskObj = new IpPrefix(subnetMask.toCharArray());
        } catch (Exception e) {
            handleError("Invalid Subnet Mask. Expected: 0.0.0.0/0 to 255.255.255.255/32", session);
            return;
        }

        if (!validateIPs(ipAddress, subnetMask, gatewayIp)) {
            handleError("IpAddress and gateWayIp should belong to the subnet provided", session);
            return;
        }
        SubnetsKey subnetsKey = new SubnetsKey(subnetMaskObj);
        Vteps vtepCli = null;
        Subnets subCli = null;

        InstanceIdentifier<Vteps> vpath =
                InstanceIdentifier.builder(TransportZones.class)
                        .child(TransportZone.class, new TransportZoneKey(transportZone))
                        .child(Subnets.class, subnetsKey).child(Vteps.class, vtepkey).build();

        // check if present in tzones and delete from cache
        boolean existsInCache = isInCache(dpnId, portName, vlanId, ipAddress, subnetMask, gatewayIp, transportZone, session);
        if (!existsInCache) {
            Optional<Vteps> vtepOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, vpath, dataBroker);
            if (vtepOptional.isPresent()) {
                vtepCli = vtepOptional.get();
                if(vtepCli.getIpAddress().equals(ipAddressObj)){
                    InstanceIdentifier<Subnets> spath =
                            InstanceIdentifier
                                    .builder(TransportZones.class)
                                    .child(TransportZone.class, new TransportZoneKey(transportZone))
                                    .child(Subnets.class, subnetsKey).build();
                    Optional<Subnets> subOptional = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, spath, dataBroker);
                    if (subOptional.isPresent()) {
                        subCli = subOptional.get();
                        if(subCli.getGatewayIp().equals(gatewayIpObj) && subCli.getVlanId().equals(vlanId)){
                            vtepDelCommitList.add(vtepCli);
                        }
                        else if(session != null) {
                                session.getConsole().println("vtep with this vlan or gateway doesnt exist");
                        }
                    }
                }
                else if(session != null) {
                        session.getConsole().println("Vtep with this ipaddress doesnt exist");
                }
            } else if(session != null) {
                    session.getConsole().println("Vtep Doesnt exist");
            }
        }
    }

    public <T extends DataObject> void deleteOnCommit() {
        List<InstanceIdentifier<T>> vtepPaths = new ArrayList<>();
        List<InstanceIdentifier<T>> subnetPaths = new ArrayList<>();
        List<InstanceIdentifier<T>> tzPaths = new ArrayList<>();
        List<Subnets> subDelList = new ArrayList<>();
        List<TransportZone> tzDelList = new ArrayList<>();
        List<Vteps> vtepDelList = new ArrayList<>();
        List<InstanceIdentifier<T>> allPaths = new ArrayList<>();
        try {
            if (vtepDelCommitList != null && !vtepDelCommitList.isEmpty()) {
                InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
                Optional<TransportZones> tZonesOptional =
                        ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker);
                if (tZonesOptional.isPresent()) {
                    TransportZones tZones = tZonesOptional.get();
                    for (TransportZone tZ : tZones.getTransportZone()) {
                        if (tZ.getSubnets() == null || tZ.getSubnets().isEmpty())
                            continue;
                        for (Subnets sub : tZ.getSubnets()) {
                            vtepDelList.addAll(vtepDelCommitList);
                            for (Vteps vtep : vtepDelList) {
                                InstanceIdentifier<T> vpath =
                                        (InstanceIdentifier<T>) InstanceIdentifier
                                                .builder(TransportZones.class)
                                                .child(TransportZone.class, tZ.getKey())
                                                .child(Subnets.class, sub.getKey())
                                                .child(Vteps.class, vtep.getKey()).build();
                                if (sub.getVteps().remove(vtep)) {
                                    vtepPaths.add(vpath);
                                    if (sub.getVteps().size() == 0 || sub.getVteps() == null) {
                                        subDelList.add(sub);
                                    }

                                }
                            }
                        }
                    }

                    for (TransportZone tZ : tZones.getTransportZone()) {
                        if (tZ.getSubnets() == null || tZ.getSubnets().isEmpty())
                            continue;
                        for (Subnets sub : subDelList) {
                            if (tZ.getSubnets().remove(sub)) {
                                InstanceIdentifier<T> spath =
                                        (InstanceIdentifier<T>) InstanceIdentifier
                                                .builder(TransportZones.class)
                                                .child(TransportZone.class, tZ.getKey())
                                                .child(Subnets.class, sub.getKey()).build();
                                subnetPaths.add(spath);
                                if (tZ.getSubnets() == null || tZ.getSubnets().size() == 0) {
                                    tzDelList.add(tZ);
                                }
                            }
                        }
                    }

                    for (TransportZone tZ : tzDelList) {
                        if (tZones.getTransportZone().remove(tZ)) {
                            InstanceIdentifier<T> tpath =
                                    (InstanceIdentifier<T>) InstanceIdentifier.builder(TransportZones.class)
                                            .child(TransportZone.class, tZ.getKey()).build();
                            tzPaths.add(tpath);
                            if (tZones.getTransportZone() == null || tZones.getTransportZone().size() == 0) {
                                MDSALDataStoreUtils.asyncRemove(dataBroker, LogicalDatastoreType.CONFIGURATION, path,
                                        ItmUtils.DEFAULT_CALLBACK);
                                return;
                            }
                        }
                    }
                    allPaths.addAll(vtepPaths);
                    allPaths.addAll(subnetPaths);
                    allPaths.addAll(tzPaths);
                    ItmUtils.asyncBulkRemove(dataBroker, LogicalDatastoreType.CONFIGURATION, allPaths,
                            ItmUtils.DEFAULT_CALLBACK);
                }
                vtepPaths.clear();
                subnetPaths.clear();
                tzPaths.clear();
                allPaths.clear();
                vtepDelCommitList.clear();
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    public void showState(List<StateTunnelList> tunnelLists, boolean tunnelMonitorEnabled,CommandSession session) throws TepException{
        if (tunnelLists == null || tunnelLists.isEmpty()) {
            handleError("No Internal Tunnels Exist", session);
            return;
        }
        if (!tunnelMonitorEnabled) {
            if(session != null) {
                session.getConsole().println("Tunnel Monitoring is Off");
            }
        }
        String displayFormat = "%-16s  %-16s  %-16s  %-16s  %-16s  %-10s  %-10s";
        session.getConsole().println(String.format(displayFormat, "Tunnel Name", "Source-DPN",
                        "Destination-DPN", "Source-IP", "Destination-IP", "Trunk-State", "Transport Type"));
        session.getConsole().println("-------------------------------------------------------------------------------------------------------------------------------------");

        for (StateTunnelList tunnelInst : tunnelLists) {
           // Display only the internal tunnels
            if(  tunnelInst.getDstInfo().getTepDeviceType().equals(TepTypeInternal.class)) {
                String tunnelInterfaceName = tunnelInst.getTunnelInterfaceName();
                LOG.trace("tunnelInterfaceName::: {}", tunnelInterfaceName);
                String tunnelState = ITMConstants.TUNNEL_STATE_UNKNOWN;
                if (tunnelInst.getOperState() == TunnelOperStatus.Up)
                    tunnelState = ITMConstants.TUNNEL_STATE_UP;
                else if (tunnelInst.getOperState() == TunnelOperStatus.Down )
                    tunnelState = ITMConstants.TUNNEL_STATE_DOWN;
                Class<? extends TunnelTypeBase> tunType = tunnelInst.getTransportType();
                String tunnelType = ITMConstants.TUNNEL_TYPE_VXLAN;
                if( tunType.equals(TunnelTypeVxlan.class))
                   tunnelType = ITMConstants.TUNNEL_TYPE_VXLAN ;
                else if( tunType.equals(TunnelTypeGre.class) )
                   tunnelType = ITMConstants.TUNNEL_TYPE_GRE ;
                else if (tunType.equals(TunnelTypeMplsOverGre.class))
                    tunnelType = ITMConstants.TUNNEL_TYPE_MPLSoGRE;
                session.getConsole().println(String.format(displayFormat, tunnelInst.getTunnelInterfaceName(), tunnelInst.getSrcInfo().getTepDeviceId(),
                        tunnelInst.getDstInfo().getTepDeviceId(), tunnelInst.getSrcInfo().getTepIp().getIpv4Address().getValue(), tunnelInst.getDstInfo().getTepIp().getIpv4Address().getValue(), tunnelState,
                        tunnelType));
                
            }
        }
    }

    // deletes from ADD-cache if it exists.
    public boolean isInCache(BigInteger dpnId, String portName, Integer vlanId, String ipAddress, String subnetMask,
                             String gatewayIp, String transportZone, CommandSession session) {
        boolean exists = false;
        VtepsKey vtepkey = new VtepsKey(dpnId, portName);
        IpAddress ipAddressObj = new IpAddress(ipAddress.toCharArray());
        IpPrefix subnetMaskObj = new IpPrefix(subnetMask.toCharArray());
        IpAddress gatewayIpObj = new IpAddress("0.0.0.0".toCharArray());
        if (gatewayIp != null) {
            gatewayIpObj = new IpAddress(gatewayIp.toCharArray());
        } else {
            LOG.debug("gateway is null");
        }
        SubnetsKey subnetsKey = new SubnetsKey(subnetMaskObj);
        Vteps vtepCli =
                new VtepsBuilder().setDpnId(dpnId).setIpAddress(ipAddressObj).setKey(vtepkey)
                        .setPortname(portName).build();
        SubnetObject subObCli = new SubnetObject(gatewayIpObj, subnetsKey, subnetMaskObj, vlanId);

        if (tZones.containsKey(transportZone)) {
            Map<SubnetObject, List<Vteps>> subVtepMapTemp = tZones.get(transportZone);
            if (subVtepMapTemp.containsKey(subObCli)) { // if Subnet exists
                List<Vteps> vtepListTemp = subVtepMapTemp.get(subObCli);
                if (vtepListTemp.contains(vtepCli)) {
                    exists = true; // return true if tzones has vtep
                    vtepListTemp.remove(vtepCli);
                    if (vtepListTemp.size() == 0) {
                        subVtepMapTemp.remove(subObCli);
                        if (subVtepMapTemp.size() == 0) {
                            tZones.remove(transportZone);
                        }
                    }
                } else if(session != null) {
                        session.getConsole().println("Vtep " + "has not been configured");
                }
            }
        }
        return exists;
    }

    public void configureTunnelType(String tZoneName, String tunnelType) {
        LOG.debug("configureTunnelType {} for transportZone {}", tunnelType, tZoneName);

        TransportZone tZoneFromConfigDS = getTransportZoneFromConfigDS(tZoneName);
        validateTunnelType(tZoneName, tunnelType,tZoneFromConfigDS);

        if (tZoneFromConfigDS != null) {
            LOG.debug("Transport zone {} with tunnel type {} already exists. No action required.", tZoneName,
                    tunnelType);
            return;
        }
        TransportZones transportZones = null;
        List<TransportZone> tZoneList = null;
        InstanceIdentifier<TransportZones> path = InstanceIdentifier.builder(TransportZones.class).build();
        Optional<TransportZones> tZones = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker);
        Class<? extends TunnelTypeBase> tunType = TunnelTypeVxlan.class;
        if( tunnelType.equals(ITMConstants.TUNNEL_TYPE_VXLAN))
            tunType = TunnelTypeVxlan.class ;
        else if( tunnelType.equals(ITMConstants.TUNNEL_TYPE_GRE) )
            tunType = TunnelTypeGre.class ;

        TransportZone tZone = new TransportZoneBuilder().setKey(new TransportZoneKey(tZoneName))
                .setTunnelType(tunType).build();
        if (tZones.isPresent()) {
            tZoneList = tZones.get().getTransportZone();
            if(tZoneList == null || tZoneList.isEmpty()) {
                tZoneList = new ArrayList<>();
            }
        } else {
            tZoneList = new ArrayList<>();
        }
        tZoneList.add(tZone);
        transportZones = new TransportZonesBuilder().setTransportZone(tZoneList).build();
        ItmUtils.syncWrite(LogicalDatastoreType.CONFIGURATION, path, transportZones, dataBroker);

    }

    /**
     * Validate tunnel type.
     *
     * @param tZoneName
     *            the t zone name
     * @param tunnelType
     *            the tunnel type
     */
    private void validateTunnelType(String tZoneName, String tunnelType,TransportZone tZoneFromConfigDS) {
        /*
        String strTunnelType = ItmUtils.validateTunnelType(tunnelType);

        TransportZone tZone = getTransportZone(tZoneName);
        if (tZone != null) {
            if (!StringUtils.equalsIgnoreCase(strTunnelType, tZone.getTunnelType())
                    && ItmUtils.isNotEmpty(tZone.getSubnets())) {
                String errorMsg = new StringBuilder("Changing the tunnel type from ").append(tZone.getTunnelType())
                        .append(" to ").append(strTunnelType)
                        .append(" is not allowed for already configured transport zone [").append(tZoneName)
                        .append("].").toString();
                Preconditions.checkArgument(false, errorMsg);
            }
        }
        */
        String strTunnelType = ItmUtils.validateTunnelType(tunnelType);
        Class<? extends TunnelTypeBase> tunType ;
        if( strTunnelType.equals(ITMConstants.TUNNEL_TYPE_VXLAN))
            tunType = TunnelTypeVxlan.class ;
        else
            tunType = TunnelTypeGre.class ;
        //TransportZone tZone = getTransportZone(tZoneName);
        // if (tZone != null) {
        if (tZoneFromConfigDS != null) {
            if( (!tZoneFromConfigDS.getTunnelType().equals(tunType))  && ItmUtils.isNotEmpty(tZoneFromConfigDS.getSubnets())) {
                String errorMsg = new StringBuilder("Changing the tunnel type from ").append(tZoneFromConfigDS.getTunnelType())
                        .append(" to ").append(strTunnelType)
                        .append(" is not allowed for already configured transport zone [").append(tZoneName)
                        .append("].").toString();
                Preconditions.checkArgument(false, errorMsg);
            }
        }
    }

    public void configureTunnelMonitorParams(boolean monitorEnabled, String monitorProtocol) {
        InstanceIdentifier<TunnelMonitorParams> path = InstanceIdentifier.builder(TunnelMonitorParams.class).build();
        Optional<TunnelMonitorParams> storedTunnelMonitor = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker);
        Class<? extends TunnelMonitoringTypeBase> monitorType ;
        if(storedTunnelMonitor.isPresent() && storedTunnelMonitor.get().getMonitorProtocol()!=null )
            monitorType = storedTunnelMonitor.get().getMonitorProtocol();
        else
        {
            if(monitorProtocol!=null && monitorProtocol.equalsIgnoreCase(ITMConstants.MONITOR_TYPE_LLDP))
                monitorType = TunnelMonitoringTypeLldp.class ;
            else
                monitorType = TunnelMonitoringTypeBfd.class ;
        }
        if (!storedTunnelMonitor.isPresent()|| storedTunnelMonitor.get().isEnabled() != monitorEnabled)  {
            TunnelMonitorParams tunnelMonitor = new TunnelMonitorParamsBuilder().setEnabled(monitorEnabled).setMonitorProtocol(monitorType).build();
            ItmUtils.asyncUpdate(LogicalDatastoreType.CONFIGURATION, path, tunnelMonitor, dataBroker,
                    ItmUtils.DEFAULT_CALLBACK);

        }
    }

    public void configureTunnelMonitorInterval(int interval) {
        InstanceIdentifier<TunnelMonitorInterval> path = InstanceIdentifier.builder(TunnelMonitorInterval.class).build();
        Optional<TunnelMonitorInterval> storedTunnelMonitor = ItmUtils.read(LogicalDatastoreType.CONFIGURATION, path, dataBroker);
        if (!storedTunnelMonitor.isPresent() || storedTunnelMonitor.get().getInterval() != interval) {
            TunnelMonitorInterval tunnelMonitor = new TunnelMonitorIntervalBuilder().setInterval(interval).build();
            ItmUtils.asyncUpdate(LogicalDatastoreType.CONFIGURATION, path, tunnelMonitor, dataBroker,
                    ItmUtils.DEFAULT_CALLBACK);
        }
    }

    public void handleError(String errorMessage, CommandSession session) throws TepException {
        if(session != null) {
            session.getConsole().println(errorMessage);
        } else {
            throw new TepException(errorMessage);
        }

    }
}
