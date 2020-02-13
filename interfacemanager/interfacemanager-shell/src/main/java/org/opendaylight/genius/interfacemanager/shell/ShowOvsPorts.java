/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.shell;

import java.math.BigInteger;
import java.util.List;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "ovs-ports", name = "show", description = "view the OVS ports on a DPN")
public class ShowOvsPorts extends OsgiCommandSupport {
    @Argument(index = 0, name = "dpnId", description = "DPN-ID", required = true, multiValued = false)
    private BigInteger dpnId;

    private static final Logger LOG = LoggerFactory.getLogger(ShowOvsPorts.class);
    private IInterfaceManager interfaceManager;
    private DataBroker dataBroker;

    public void setInterfaceManager(IInterfaceManager interfaceManager) {
        this.interfaceManager = interfaceManager;
    }

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    protected Object doExecute() {
        LOG.debug("Executing show ovs-ports command");
        final Uint64 id = Uint64.valueOf(dpnId);
        List<OvsdbTerminationPointAugmentation> ports = interfaceManager.getPortsOnBridge(id);

        if (!ports.isEmpty()) {
            IfmCLIUtil.showBridgePortsHeader(session, id);
        }
        for (OvsdbTerminationPointAugmentation port: ports) {
            IfmCLIUtil.showBridgePortsOutput(session, port);
        }
        return null;
    }
}
