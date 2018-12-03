/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.bridge.ref.info.BridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "tep", name = "show-bridges", description = "shows bridges for given DPN-ID")
public class TepShowBridges extends OsgiCommandSupport {

    private static final Logger LOG = LoggerFactory.getLogger(TepShowBridges.class);

    private IITMProvider itmProvider;
    private OvsBridgeRefEntryCache ovsBridgeRefEntryCache;
    private IInterfaceManager interfaceManager;

    public void setItmProvider(IITMProvider itmProvider) {
        this.itmProvider = itmProvider;
    }

    public void setOvsBridgeRefEntryCache(OvsBridgeRefEntryCache ovsBridgeRefEntryCache) {
        this.ovsBridgeRefEntryCache = ovsBridgeRefEntryCache;
    }

    public void setInterfaceManager(IInterfaceManager interfaceManager) {
        this.interfaceManager = interfaceManager;
    }

    @Argument(index = 0, name = "dpnId", description = "DPN-ID", required = false, multiValued = false)
    private BigInteger dpnId;

    @Override
    protected Object doExecute() {
        if (interfaceManager.isItmDirectTunnelsEnabled()) {
            Collection<OvsBridgeRefEntry> ovsBridgeRefEntries = ovsBridgeRefEntryCache.getAllPresent();
            itmProvider.showBridges(ovsBridgeRefEntries.stream()
                    .filter(dpnId == null
                            ? ovsBridgeRefEntry -> true
                            : ovsBridgeRefEntry -> ovsBridgeRefEntry.getDpid().equals(dpnId))
                    .collect(Collectors.toMap(ovsBridgeRefEntry -> ovsBridgeRefEntry.getDpid(),
                        ovsBridgeRefEntry -> ovsBridgeRefEntry.getOvsBridgeReference())));
        } else {
            Map<BigInteger, BridgeRefEntry> bridgeRefEntryMap = interfaceManager.getBridgeRefEntryMap();
            itmProvider.showBridges(bridgeRefEntryMap.keySet().stream()
                    .filter(dpnId == null
                            ? key -> true
                            : key -> key.equals(dpnId))
                    .collect(Collectors.toMap(key -> key,
                        key -> bridgeRefEntryMap.get(key).getBridgeReference())));
        }
        return null;
    }
}
