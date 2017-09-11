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
import java.util.concurrent.ConcurrentHashMap;
import org.mockito.Mockito;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;

/**
 * IInterfaceManager implementation for tests.
 *
 * @author Michael Vorburger
 */
public abstract class TestInterfaceManager implements IInterfaceManager {

    // Implementation similar to e.g. the org.opendaylight.genius.mdsalutil.interfaces.testutils.TestIMdsalApiManager

    public static TestInterfaceManager newInstance() {
        TestInterfaceManager testInterfaceManager = Mockito.mock(TestInterfaceManager.class, realOrException());
        testInterfaceManager.interfaceInfos = new ConcurrentHashMap<>();
        testInterfaceManager.interfaces = new ConcurrentHashMap<>();
        return testInterfaceManager;
    }

    private Map<String, InterfaceInfo> interfaceInfos;
    private Map<String, Interface> interfaces;

    public void addInterfaceInfo(InterfaceInfo interfaceInfo) {
        interfaceInfos.put(interfaceInfo.getInterfaceName(), interfaceInfo);
    }

    public void addInterface(Interface iface) {
        interfaces.put(iface.getName(), iface);
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
    public InterfaceInfo getInterfaceInfoFromOperationalDSCache(String interfaceName) {
        return getInterfaceInfo(interfaceName);
    }

    @Override
    public Interface getInterfaceInfoFromConfigDataStore(String interfaceName) {
        Interface iface = interfaces.get(interfaceName);
        if (iface == null) {
            throw new IllegalStateException(
                "must addInterface() to TestInterfaceManager before getInterfaceInfoFromConfigDataStore: " + interfaceName);
        }
        return iface;

    }
}
