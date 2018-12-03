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

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;

@Service
@Command(scope = "tep", name = "show-bridges", description = "shows bridges for given DPN-ID")
public class TepShowBridges implements Action {

    private @Reference IITMProvider itmProvider;
    private @Reference OvsBridgeRefEntryCache ovsBridgeRefEntryCache;

    @Argument(index = 0, name = "dpnId", description = "DPN-ID", required = true, multiValued = false)
    private BigInteger dpnId;

    @Override
    public Object execute() throws Exception {
        Collection<OvsBridgeRefEntry> ovsBridgeRefEntries = ovsBridgeRefEntryCache.getAllPresent();
        if (!ovsBridgeRefEntries.isEmpty()) {
            itmProvider.showBridges(dpnId, ovsBridgeRefEntries.stream()
                    .filter(i -> i.getDpid().equals(dpnId))
                    .map(i -> i.getOvsBridgeReference())
                    .collect(Collectors.toList()));
        } else {
            System.out.println("No Bridges available");
        }
        return null;
    }
}
