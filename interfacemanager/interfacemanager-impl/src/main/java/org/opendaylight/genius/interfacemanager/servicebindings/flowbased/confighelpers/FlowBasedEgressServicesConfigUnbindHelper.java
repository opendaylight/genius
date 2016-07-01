/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesRemovable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.ServicesInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlowBasedEgressServicesConfigUnbindHelper implements FlowBasedServicesRemovable{
    private static final Logger LOG = LoggerFactory.getLogger(FlowBasedEgressServicesConfigUnbindHelper.class);

    private InterfacemgrProvider interfaceMgrProvider;
    private static volatile FlowBasedServicesRemovable flowBasedEgressServicesRemovable;

    private FlowBasedEgressServicesConfigUnbindHelper(InterfacemgrProvider interfaceMgrProvider) {
        this.interfaceMgrProvider = interfaceMgrProvider;
    }

    public static void intitializeFlowBasedEgressServicesConfigRemoveHelper(InterfacemgrProvider interfaceMgrProvider) {
        if (flowBasedEgressServicesRemovable == null) {
            synchronized (FlowBasedEgressServicesConfigUnbindHelper.class) {
                if (flowBasedEgressServicesRemovable == null) {
                    flowBasedEgressServicesRemovable = new FlowBasedEgressServicesConfigUnbindHelper(interfaceMgrProvider);
                }
            }
        }
    }

    public static FlowBasedServicesRemovable getFlowBasedEgressServicesRemoveHelper() {
        if (flowBasedEgressServicesRemovable == null) {
            LOG.error("FlowBasedIngressBindHelper`` is not initialized");
        }
        return flowBasedEgressServicesRemovable;
    }

    public List<ListenableFuture<Void>> unbindService(InstanceIdentifier<BoundServices> instanceIdentifier,
                                                             BoundServices boundServiceOld) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        //TODO
        return futures;
    }

}