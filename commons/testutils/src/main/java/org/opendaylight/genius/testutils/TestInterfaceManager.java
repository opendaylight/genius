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
        return testInterfaceManager;
    }

    private Map<String, InterfaceInfo> interfaceInfos;

    public void addInterfaceInfo(InterfaceInfo interfaceInfo) {
        interfaceInfos.put(interfaceInfo.getInterfaceName(), interfaceInfo);
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
}
