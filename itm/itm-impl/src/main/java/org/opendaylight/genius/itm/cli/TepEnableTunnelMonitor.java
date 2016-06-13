/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "tep", name = "enable-tunnel-monitor", description = "switch ON/OFF supervision of VxLAN tunnels")
public class TepEnableTunnelMonitor extends OsgiCommandSupport {

    private static final Logger logger = LoggerFactory.getLogger(TepEnableTunnelMonitor.class);

    @Argument(index = 0, name = "true|false", description = "true|false to enable/disable Tunnel Monitoring", required = true, multiValued = false)
    private Boolean enableTunnelMonitor;
    @Argument(index = 1, name = "monitorProtocol", description = "monitorProtocol", required = false, multiValued = false)
    private String monitorProtocol;

    private IITMProvider itmProvider;

    public void setItmProvider(IITMProvider itmProvider) {
        this.itmProvider = itmProvider;
    }

    @Override
    protected Object doExecute() {
        try {
            logger.debug("Executing Enable Tunnel Monitor command");
            itmProvider.configureTunnelMonitorParams(enableTunnelMonitor, monitorProtocol);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }
}
