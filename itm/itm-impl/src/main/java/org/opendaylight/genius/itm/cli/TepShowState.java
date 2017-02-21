/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import java.util.ArrayList;
import java.util.List;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.utils.cache.DataStoreCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;

@Command(scope = "tep", name = "show-state", description = "Monitors tunnel state")

public class TepShowState extends OsgiCommandSupport {

    private IITMProvider itmProvider;

    public void setItmProvider(IITMProvider itmProvider) {
        this.itmProvider = itmProvider;
    }

    @Override
    protected Object doExecute() {
        List<StateTunnelList> tunnels = null ;
        List<Object> values = null ;
        values = DataStoreCache.getValues(ITMConstants.TUNNEL_STATE_CACHE_NAME);
        if (values != null) {
             tunnels = new ArrayList<>() ;
            for (Object value : values) {
                 tunnels.add((StateTunnelList)value) ;
            }
        }
        if (tunnels != null) {
           itmProvider.showState(tunnels, session);
        } else {
            session.getConsole().println("No Internal Tunnels configured on the switch");
        }
        return null;
    }
}