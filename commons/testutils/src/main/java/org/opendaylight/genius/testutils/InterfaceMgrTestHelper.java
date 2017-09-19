/*
 * Copyright © 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.testutils;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class InterfaceMgrTestHelper {

    TestInterfaceManager testInterfaceManager;

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceMgrTestHelper.class);

    private InstanceIdentifier<Interface> buildIid(String interfaceName) {
        return InstanceIdentifier.builder(Interfaces.class).child(Interface.class,
                new InterfaceKey(interfaceName)).build();
    }

    public void addInterface(DataBroker dataBroker, InterfaceDetails interfaceDetails)
            throws TransactionCommitFailedException {
        testInterfaceManager.addInterface(dataBroker, interfaceDetails);
    }

    public void addInterfaceInfo(String interfaceName , InterfaceInfo interfaceInfo) {
    }

    public void addTunnelInterface(DataBroker dataBroker, TunnelInterfaceDetails interfaceDetails)
            throws TransactionCommitFailedException {
        testInterfaceManager.addTunnelInterface(dataBroker, interfaceDetails);
    }

    public static void addEgressActionInfosForInterface(int ifIndex, int actionKeyStart, List<ActionInfo> result) {
    }
}
