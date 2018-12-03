/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import java.math.BigInteger;
import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "tep", name = "show-bridges", description = "shows bridges for given DPN-ID")
public class TepShowBridges extends OsgiCommandSupport {
    private static final Logger LOG = LoggerFactory.getLogger(TepShowBridges.class);
    private IITMProvider itmProvider;
    private OvsBridgeRefEntryCache ovsBridgeRefEntryCache;

    public void setItmProvider(IITMProvider itmProvider) {
        this.itmProvider = itmProvider;
    }

    public void setOvsBridgeRefEntryCache(OvsBridgeRefEntryCache ovsBridgeRefEntryCache) {
        this.ovsBridgeRefEntryCache = ovsBridgeRefEntryCache;
    }

    @Argument(index = 0, name = "dpnId", description = "DPN-ID", required = false, multiValued = false)
    private BigInteger dpnId;

    @Override
    protected Object doExecute() {
        Collection<OvsBridgeRefEntry> ovsBridgeRefEntries = ovsBridgeRefEntryCache.getAllPresent();
        if (!ovsBridgeRefEntries.isEmpty()) {
            System.out.printf("%-16s  %-16s  %-36s%n", "DPN-ID", "Bridge-Name", "Bridge-UUID");
            System.out.printf("------------------------------------------------------------------------%n");
            if (dpnId == null) {
                ovsBridgeRefEntries.stream()
                        .collect(Collectors.groupingBy((x) ->
                                x.getDpid(),
                                Collectors.mapping((x) ->
                                        x.getOvsBridgeReference(),
                                        Collectors.toSet())))
                        .forEach((k, v) -> itmProvider.showBridges(k, v));
            } else {
                itmProvider.showBridges(dpnId, ovsBridgeRefEntries.stream()
                        .filter(i -> i.getDpid().equals(dpnId))
                        .map(i -> i.getOvsBridgeReference())
                        .collect(Collectors.toList()));
            }
        } else {
            LOG.info("No Bridges available");
        }
        return null;
    }
}
