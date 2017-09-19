/*
 * Copyright © 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.testutils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class InterfaceMgrTestHelper {

    private Map<String, InterfaceInfo> interfaces = new ConcurrentHashMap<>();
    private Map<String, Boolean> externalInterfaces = new ConcurrentHashMap<>();
    private DataBroker dataBroker;

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceMgrTestHelper.class);

    private InstanceIdentifier<Interface> buildIid(String interfaceName) {
        return InstanceIdentifier.builder(Interfaces.class).child(Interface.class,
                new InterfaceKey(interfaceName)).build();
    }

    public void addInterface(DataBroker dataBroker, InterfaceDetails interfaceDetails)
            throws TransactionCommitFailedException {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, interfaceDetails.getIfaceIid(), interfaceDetails.getIface());
        tx.submit().checkedGet();
        tx = dataBroker.newReadWriteTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, interfaceDetails.getIfStateId(), interfaceDetails.getIfState());
        tx.submit().checkedGet();
        String interfaceName = interfaceDetails.getName();
    }

    public void addInterfaceInfo(String interfaceName , InterfaceInfo interfaceInfo) {
        interfaces.put(interfaceName, interfaceInfo);
    }

    public void addTunnelInterface(DataBroker dataBroker, TunnelInterfaceDetails interfaceDetails)
            throws TransactionCommitFailedException {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, interfaceDetails.getIfaceIid(), interfaceDetails.getIface());
        tx.submit().checkedGet();
        tx = dataBroker.newReadWriteTransaction();
        tx.put(LogicalDatastoreType.OPERATIONAL, interfaceDetails.getIfStateId(), interfaceDetails.getIfState());
        tx.submit().checkedGet();
    }
}
