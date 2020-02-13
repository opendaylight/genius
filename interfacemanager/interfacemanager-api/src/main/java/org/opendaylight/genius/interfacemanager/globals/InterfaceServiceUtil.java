/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.globals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.genius.mdsalutil.FlowInfoKey;
import org.opendaylight.genius.mdsalutil.GroupInfoKey;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.matches.MatchInPort;
import org.opendaylight.genius.mdsalutil.matches.MatchMetadata;
import org.opendaylight.genius.mdsalutil.matches.MatchVlanVid;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeIngress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceTypeFlowBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.StypeOpenflow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.StypeOpenflowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServicesKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class InterfaceServiceUtil {

    private InterfaceServiceUtil() {
    }

    /**
     * Build a service information structure.
     *
     * @deprecated Use {@link #buildServiceInfo(String, int)}.
     */
    @Deprecated
    public static ServicesInfo buildServiceInfo(String serviceName, short serviceIndex, Uint8 servicePriority,
            Uint64 cookie, List<Instruction> instructions) {
        return buildServiceInfo(serviceName, servicePriority);
    }

    /**
     * Build a service information structure.
     *
     * @deprecated Use {@link #buildServiceInfo(String, int)}.
     */
    @Deprecated
    public static ServicesInfo buildServiceInfo(String serviceName, short serviceIndex, Uint8 servicePriority,
            Uint64 cookie) {
        return buildServiceInfo(serviceName, servicePriority);
    }

    public static ServicesInfo buildServiceInfo(String serviceName, Uint8 servicePriority) {
        List<BoundServices> boundService = new ArrayList<>();
        boundService.add(new BoundServicesBuilder().setServicePriority(servicePriority)
                .setServiceName(serviceName).build());
        return new ServicesInfoBuilder().setBoundServices(boundService)
                .withKey(new ServicesInfoKey(serviceName, ServiceModeIngress.class)).build();
    }

    public static BoundServices getBoundServices(String serviceName, Uint8 servicePriority, Uint16 flowPriority,
            Uint64 cookie, List<Instruction> instructions) {
        StypeOpenflowBuilder augBuilder = new StypeOpenflowBuilder().setFlowCookie(cookie).setFlowPriority(flowPriority)
                .setInstruction(instructions);
        return new BoundServicesBuilder().withKey(new BoundServicesKey(servicePriority)).setServiceName(serviceName)
                .setServicePriority(servicePriority).setServiceType(ServiceTypeFlowBased.class)
                .addAugmentation(StypeOpenflow.class, augBuilder.build()).build();
    }

    public static List<MatchInfo> getMatchInfoForVlanLPort(Uint64 dpId, long portNo, long vlanId,
            boolean isVlanTransparent) {
        List<MatchInfo> matches = new ArrayList<>();
        matches.add(new MatchInPort(dpId, portNo));
        if (vlanId != 0 && !isVlanTransparent) {
            matches.add(new MatchVlanVid((int) vlanId));
        }
        return matches;
    }

    /**
     * If matches contains MatchMetadata in its list and match is of type MatchMetadata, then this
     * function will merge the MatchMetadatas using "or" of the masks and the values, otherwise it will add
     * the match to the matches list.
     *
     * @param matches - matches list
     * @param match - metadata or other match
     */
    public static void mergeMetadataMatchsOrAdd(List<MatchInfoBase> matches, MatchInfoBase match) {
        Iterator<MatchInfoBase> iter = matches.iterator();
        while (iter.hasNext()) {
            MatchInfoBase match2 = iter.next();
            if (match2 instanceof MatchMetadata) {
                if (match instanceof MatchMetadata) {
                    MatchMetadata metadataMatch = (MatchMetadata) match;
                    Uint64 value = MetaDataUtil.mergeMetadataValues(((MatchMetadata) match2).getMetadata(),
                            metadataMatch.getMetadata());
                    Uint64 mask = MetaDataUtil.mergeMetadataMask(((MatchMetadata) match2).getMask(),
                            metadataMatch.getMask());
                    match = new MatchMetadata(value, mask);
                    iter.remove();
                }
                break;
            }
        }
        matches.add(match);
    }

    public static short getVlanId(String interfaceName, DataBroker broker) throws ReadFailedException {
        InstanceIdentifier<Interface> id = InstanceIdentifier.builder(Interfaces.class)
                .child(Interface.class, new InterfaceKey(interfaceName)).build();
        Optional<Interface> ifInstance = SingleTransactionDataBroker.syncReadOptional(broker,
                LogicalDatastoreType.CONFIGURATION, id);
        if (ifInstance.isPresent()) {
            IfL2vlan vlanIface = ifInstance.get().augmentation(IfL2vlan.class);
            return vlanIface.getVlanId() == null ? 0 : vlanIface.getVlanId().getValue().shortValue();
        }
        return -1;
    }

    public static Set<Object> getStatRequestKeys(Uint64 dpId, short tableId, List<MatchInfo> matches, String flowId,
            long groupId) {
        Set<Object> statRequestKeys = new HashSet<>();
        statRequestKeys.add(getFlowStatisticsKey(dpId, tableId, matches, flowId));
        statRequestKeys.add(getGroupStatisticsKey(dpId, groupId));
        return statRequestKeys;
    }

    public static GroupInfoKey getGroupStatisticsKey(Uint64 dpId, long groupId) {
        return new GroupInfoKey(dpId, groupId);
    }

    public static FlowInfoKey getFlowStatisticsKey(Uint64 dpId, short tableId, List<MatchInfo> matches,
            String flowId) {
        return new FlowInfoKey(dpId, tableId, MDSALUtil.buildMatches(matches), flowId);
    }

    public static List<MatchInfo> getLPortDispatcherMatches(short serviceIndex, int interfaceTag) {
        List<MatchInfo> mkMatches = new ArrayList<>();
        mkMatches.add(new MatchMetadata(
                 MetaDataUtil.getMetaDataForLPortDispatcher(interfaceTag, serviceIndex),
                 MetaDataUtil.getMetaDataMaskForLPortDispatcher()));
        return mkMatches;
    }
}
