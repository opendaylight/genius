/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import java.util.Collection;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;

@Service
@Command(scope = "tep", name = "show-state", description = "Monitors tunnel state")
public class TepShowState implements Action {

    private @Reference IITMProvider itmProvider;
    private @Reference TunnelStateCache tunnelStateCache;

    @Override
    @SuppressWarnings("checkstyle:RegexpSingleLineJava")
    public Object execute() throws Exception {
        Collection<StateTunnelList> tunnels = tunnelStateCache.getAllPresent();
        if (!tunnels.isEmpty()) {
            itmProvider.showState(tunnels);
        } else {
            System.out.println("No Internal Tunnels configured on the switch");
        }
        return null;
    }
}
