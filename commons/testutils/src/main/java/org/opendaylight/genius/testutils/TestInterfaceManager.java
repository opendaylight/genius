/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.testutils;

import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.realOrException;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.testutils.interfacemanager.InterfaceHelper;
import org.opendaylight.genius.testutils.interfacemanager.InterfaceStateHelper;
import org.opendaylight.genius.testutils.interfacemanager.TunnelInterfaceDetails;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yangtools.yang.common.Uint64;

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
            throws ExecutionException, InterruptedException {
        interfaceInfos.put(interfaceInfo.getInterfaceName(), interfaceInfo);
        if (optDataBroker.isPresent()) {
            // Can't use ifPresent() here because of checked exception from tx.commit().get();
            DataBroker dataBroker = optDataBroker.get();
            ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();

            Interface iface = InterfaceHelper.buildVlanInterfaceFromInfo(interfaceInfo);
            //Add the interface to config ds so that if the application reads from configds it finds it there
            tx.put(LogicalDatastoreType.CONFIGURATION,
                    InterfaceHelper.buildIId(interfaceInfo.getInterfaceName()),
                    iface);

            //Add the interface to oper ds so that if the application reads from configds it finds it there
            tx.put(LogicalDatastoreType.OPERATIONAL,
                    InterfaceStateHelper.buildStateInterfaceIid(interfaceInfo.getInterfaceName()),
                    InterfaceStateHelper.buildStateFromInterfaceInfo(interfaceInfo));
            tx.commit().get();
            addInterface(iface);
        }
    }

    public void addInterface(Interface iface) {
        interfaces.put(iface.getName(), iface);
    }

    public void addTunnelInterface(TunnelInterfaceDetails tunnelInterfaceDetails)
            throws ExecutionException, InterruptedException {
        InterfaceInfo interfaceInfo = tunnelInterfaceDetails.getInterfaceInfo();
        interfaceInfos.put(interfaceInfo.getInterfaceName(), interfaceInfo);

        if (optDataBroker.isPresent()) {
            // Can't use ifPresent() here because of checked exception from tx.commit().get();
            DataBroker dataBroker = optDataBroker.get();

            Interface iface = InterfaceHelper.buildVxlanTunnelInterfaceFromInfo(tunnelInterfaceDetails);

            ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
            tx.put(LogicalDatastoreType.CONFIGURATION,
                    InterfaceHelper.buildIId(interfaceInfo.getInterfaceName()),
                    iface);

            tx.put(LogicalDatastoreType.OPERATIONAL,
                    InterfaceStateHelper.buildStateInterfaceIid(interfaceInfo.getInterfaceName()),
                    InterfaceStateHelper.buildStateFromInterfaceInfo(interfaceInfo));
            tx.commit().get();
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
    public Uint64 getDpnForInterface(String interfaceName) {
        return interfaceInfos.get(interfaceName).getDpId();
    }

    @Override
    public Uint64 getDpnForInterface(Interface intrface) {
        return interfaceInfos.get(intrface.getName()).getDpId();
    }

    @Override
    public boolean isExternalInterface(String interfaceName) {
        return externalInterfaces.containsKey(interfaceName);
    }

    @Override
    public boolean isItmDirectTunnelsEnabled() {
        return false;
    }
}
