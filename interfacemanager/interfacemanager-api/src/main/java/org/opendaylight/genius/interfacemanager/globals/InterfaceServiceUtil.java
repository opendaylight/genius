/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.globals;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.FlowInfoKey;
import org.opendaylight.genius.mdsalutil.GroupInfoKey;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchFieldType;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceTypeFlowBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.StypeOpenflow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.StypeOpenflowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;

import com.google.common.base.Optional;

public class InterfaceServiceUtil {

    public static ServicesInfo buildServiceInfo(String serviceName, short serviceIndex, int servicePriority,
            BigInteger cookie, List<Instruction> instructions) {
        List<BoundServices>  boundService = new ArrayList<>();
        boundService.add(new BoundServicesBuilder().setServicePriority((short)servicePriority).setServiceName(serviceName).build());
        return new ServicesInfoBuilder().setBoundServices(boundService).setKey(new ServicesInfoKey(serviceName, ServiceModeIngress.class)).build();
    }

    public static BoundServices getBoundServices(String serviceName, short servicePriority, int flowPriority,
                                                 BigInteger cookie, List<Instruction> instructions) {
        StypeOpenflowBuilder augBuilder = new StypeOpenflowBuilder().setFlowCookie(cookie).setFlowPriority(flowPriority).setInstruction(instructions);
        return new BoundServicesBuilder().setKey(new BoundServicesKey(servicePriority))
                .setServiceName(serviceName).setServicePriority(servicePriority)
                .setServiceType(ServiceTypeFlowBased.class).addAugmentation(StypeOpenflow.class, augBuilder.build()).build();
    }

    public static ServicesInfo buildServiceInfo(String serviceName, short serviceIndex, int servicePriority,
            BigInteger cookie) {
        List<BoundServices>  boundService = new ArrayList<>();
        boundService.add(new BoundServicesBuilder().setServicePriority((short)servicePriority).setServiceName(serviceName).build());
        return new ServicesInfoBuilder().setBoundServices(boundService).setKey(new ServicesInfoKey(serviceName, ServiceModeIngress.class)).build();
    }

    public static List<MatchInfo> getMatchInfoForVlanLPort(BigInteger dpId, long portNo, long vlanId, boolean isVlanTransparent) {
        List<MatchInfo> matches = new ArrayList<>();
        matches.add(new MatchInfo(MatchFieldType.in_port, new BigInteger[] {dpId, BigInteger.valueOf(portNo)}));
        if (vlanId != 0 && !isVlanTransparent) {
            matches.add(new MatchInfo(MatchFieldType.vlan_vid, new long[] { vlanId }));
        }
        return matches;
    }

    public static short getVlanId(String interfaceName, DataBroker broker) {
        InstanceIdentifier<Interface> id = InstanceIdentifier.builder(Interfaces.class)
                .child(Interface.class, new InterfaceKey(interfaceName)).build();
        Optional<Interface> ifInstance = MDSALUtil.read(LogicalDatastoreType.CONFIGURATION, id, broker);
        if (ifInstance.isPresent()) {
            IfL2vlan vlanIface =ifInstance.get().getAugmentation(IfL2vlan.class);
            short vlanId = vlanIface.getVlanId() == null ? 0 : vlanIface.getVlanId().getValue().shortValue();
            return vlanId;
        }
        return -1;
    }

    public static Set<Object> getStatRequestKeys(BigInteger dpId, short tableId, List<MatchInfo> matches, String flowId, long groupId) {
        Set<Object> statRequestKeys = new HashSet<>();
        statRequestKeys.add(getFlowStatisticsKey(dpId, tableId, matches, flowId));
        statRequestKeys.add(getGroupStatisticsKey(dpId, groupId));
        return statRequestKeys;
    }

    public static GroupInfoKey getGroupStatisticsKey(BigInteger dpId, long groupId) {
        return new GroupInfoKey(dpId, groupId);
    }

    public static FlowInfoKey getFlowStatisticsKey(BigInteger dpId, short tableId, List<MatchInfo> matches, String flowId) {
        return new FlowInfoKey(dpId, tableId, MDSALUtil.buildMatches(matches), flowId);
    }

    public static List<MatchInfo> getLPortDispatcherMatches(short serviceIndex, int interfaceTag) {
        List<MatchInfo> mkMatches = new ArrayList<>();
        mkMatches.add(new MatchInfo(MatchFieldType.metadata, new BigInteger[] {
                 MetaDataUtil.getMetaDataForLPortDispatcher(interfaceTag, serviceIndex),
                 MetaDataUtil.getMetaDataMaskForLPortDispatcher() }));
        return mkMatches;
    }

}
