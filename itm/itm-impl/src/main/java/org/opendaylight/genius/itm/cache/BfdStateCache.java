/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Singleton;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface;

@Singleton
public class BfdStateCache {

    private final ConcurrentMap<String, Interface.OperStatus> bfdStateMap = new ConcurrentHashMap<>();

    public void add(String interfaceName, Interface.OperStatus operStatus) {
        bfdStateMap.put(interfaceName, operStatus);
    }

    public Interface.OperStatus remove(String interfaceName) {
        return bfdStateMap.remove(interfaceName);
    }

    public Interface.OperStatus get(String interfaceName) {
        return bfdStateMap.get(interfaceName);
    }
}
