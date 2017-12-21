/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.testutils;

import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.testutils.interfacemanager.InterfaceHelper;
import org.opendaylight.genius.testutils.interfacemanager.InterfaceStateHelper;
import org.opendaylight.genius.testutils.interfacemanager.TunnelInterfaceDetails;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * IInterfaceManager implementation for tests.
 *
 * @author Michael Vorburger
 */
public abstract class TestInterfaceManager implements IInterfaceManager {

    private Map<String, InterfaceInfo> interfaceInfos;
    private Map<String, Interface> interfaces;
    private Map<String, Boolean> externalInterfaces;
    private Optional<DataBroker> optDataBroker;

    /**
     * Deprecated factory method.
     * @deprecated Use {@link TestInterfaceManager#newInstance(DataBroker)} instead now.
     */
    @Deprecated
    public static TestInterfaceManager newInstance() {
        TestInterfaceManager testInterfaceManager = Mockito.mock(TestInterfaceManager.class, realOrException());
        testInterfaceManager.interfaceInfos = new ConcurrentHashMap<>();
        testInterfaceManager.interfaces = new ConcurrentHashMap<>();
        testInterfaceManager.externalInterfaces = new ConcurrentHashMap<>();
        testInterfaceManager.optDataBroker = Optional.empty();
        return testInterfaceManager;
    }

    public static TestInterfaceManager newInstance(DataBroker dataBroker) {
        TestInterfaceManager testInterfaceManager = newInstance();
        testInterfaceManager.optDataBroker = Optional.of(dataBroker);
        return testInterfaceManager;
    }

    public void addInterfaceInfo(InterfaceInfo interfaceInfo)
            throws TransactionCommitFailedException {
        interfaceInfos.put(interfaceInfo.getInterfaceName(), interfaceInfo);
        if (optDataBroker.isPresent()) {
            // Can't use ifPresent() here because of checked exception from tx.submit().checkedGet();
            DataBroker dataBroker = optDataBroker.get();
            ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();

            Interface iface = InterfaceHelper.buildVlanInterfaceFromInfo(interfaceInfo);
            InstanceIdentifier<Interface> ifaceIId = InterfaceHelper.buildIId(interfaceInfo.getInterfaceName());
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces
                    .rev140508.interfaces.state.Interface>
                    ifaceStateIId = InterfaceStateHelper.buildStateInterfaceIid(interfaceInfo.getInterfaceName());
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state
                    .Interface
                    ifaceState = InterfaceStateHelper.buildStateFromInterfaceInfo(interfaceInfo);

            // Add the interface and state to both config and operational
            tx.put(LogicalDatastoreType.CONFIGURATION, ifaceIId, iface);
            tx.put(LogicalDatastoreType.CONFIGURATION, ifaceStateIId, ifaceState);
            tx.put(LogicalDatastoreType.OPERATIONAL, ifaceIId, iface);
            tx.put(LogicalDatastoreType.OPERATIONAL, ifaceStateIId, ifaceState);

            tx.submit().checkedGet();
            addInterface(iface);
        }
    }

    public void addInterface(Interface iface) {
        interfaces.put(iface.getName(), iface);
    }

    public void addTunnelInterface(TunnelInterfaceDetails tunnelInterfaceDetails)
            throws TransactionCommitFailedException {
        InterfaceInfo interfaceInfo = tunnelInterfaceDetails.getInterfaceInfo();
        interfaceInfos.put(interfaceInfo.getInterfaceName(), interfaceInfo);

        if (optDataBroker.isPresent()) {
            // Can't use ifPresent() here because of checked exception from tx.submit().checkedGet();
            DataBroker dataBroker = optDataBroker.get();

            Interface iface = InterfaceHelper.buildVxlanTunnelInterfaceFromInfo(tunnelInterfaceDetails);

            ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
            tx.put(LogicalDatastoreType.CONFIGURATION,
                    InterfaceHelper.buildIId(interfaceInfo.getInterfaceName()),
                    iface);

            tx.put(LogicalDatastoreType.OPERATIONAL,
                    InterfaceStateHelper.buildStateInterfaceIid(interfaceInfo.getInterfaceName()),
                    InterfaceStateHelper.buildStateFromInterfaceInfo(interfaceInfo));
            tx.submit().checkedGet();
            externalInterfaces.put(interfaceInfo.getInterfaceName(), true);
            addInterface(iface);
        }
    }

    @Override
    public InterfaceInfo getInterfaceInfo(String interfaceName) {
        InterfaceInfo interfaceInfo = interfaceInfos.get(interfaceName);
        if (interfaceInfo == null) {
            throw new IllegalStateException(
                    "must addInterfaceInfo() to TestInterfaceManager before getInterfaceInfo: " + interfaceName);
        }
        return interfaceInfo;
    }

    @Override
    public InterfaceInfo getInterfaceInfoFromOperationalDataStore(String interfaceName) {
        return getInterfaceInfo(interfaceName);
    }

    @Override
    public InterfaceInfo getInterfaceInfoFromOperationalDataStore(
            String interfaceName, InterfaceInfo.InterfaceType interfaceType) {
        return interfaceInfos.get(interfaceName);
    }

    @Override
    public InterfaceInfo getInterfaceInfoFromOperationalDSCache(String interfaceName) {
        return getInterfaceInfo(interfaceName);
    }

    @Override
    public Interface getInterfaceInfoFromConfigDataStore(String interfaceName) {
        Interface iface = interfaces.get(interfaceName);
        if (iface == null) {
            throw new IllegalStateException(
                    "must addInterface() to TestInterfaceManager before getInterfaceInfoFromConfigDataStore: "
                    + interfaceName);
        }
        return iface;
    }

    @Override
    public BigInteger getDpnForInterface(String interfaceName) {
        return interfaceInfos.get(interfaceName).getDpId();
    }

    @Override
    public BigInteger getDpnForInterface(Interface intrface) {
        return interfaceInfos.get(intrface.getName()).getDpId();
    }

    @Override
    public boolean isExternalInterface(String interfaceName) {
        return externalInterfaces.containsKey(interfaceName);
    }

}
