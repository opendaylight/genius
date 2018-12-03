/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import com.google.common.base.Optional;

import java.math.BigInteger;
import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.BridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "tep", name = "show-bridges", description = "shows bridges for given DPN-ID")
public class TepShowBridges extends OsgiCommandSupport {
    private static final Logger LOG = LoggerFactory.getLogger(TepShowBridges.class);
    private IITMProvider itmProvider;
    private OvsBridgeRefEntryCache ovsBridgeRefEntryCache;
    private IInterfaceManager interfaceManager;
    private DataBroker dataBroker;

    public void setItmProvider(IITMProvider itmProvider) {
        this.itmProvider = itmProvider;
    }

    public void setOvsBridgeRefEntryCache(OvsBridgeRefEntryCache ovsBridgeRefEntryCache) {
        this.ovsBridgeRefEntryCache = ovsBridgeRefEntryCache;
    }

    public void setInterfaceManager(IInterfaceManager interfaceManager) {
        this.interfaceManager = interfaceManager;
    }

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
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
                    .collect(Collectors.groupingBy((ovsBridgeRefEntry) ->
                                    ovsBridgeRefEntry.getDpid(),
                            Collectors.mapping((ovsBridgeRefEntry) ->
                                            ovsBridgeRefEntry.getOvsBridgeReference(), Collectors.toSet()))));
        } else {
            Optional<BridgeRefInfo> bridgeRefInfo = ItmUtils.read(
                    LogicalDatastoreType.OPERATIONAL,
                    InstanceIdentifier.builder(BridgeRefInfo.class).build(),
                    dataBroker);
            if (!bridgeRefInfo.isPresent()) {
                LOG.trace("Bridge Reference Information not found!");
                return null;
            }
            itmProvider.showBridges(bridgeRefInfo.get().getBridgeRefEntry().stream()
                    .filter(dpnId == null
                            ? ovsBridgeRefEntry -> true
                            : ovsBridgeRefEntry -> ovsBridgeRefEntry.getDpid().equals(dpnId))
                    .collect(Collectors.groupingBy((bridgeRefEntry) ->
                                    bridgeRefEntry.getDpid(),
                            Collectors.mapping((bridgeRefEntry) ->
                                    bridgeRefEntry.getBridgeReference(), Collectors.toSet()))));
        }
        return null;
    }
}
