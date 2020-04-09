/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.shell;

import java.util.List;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "vxlan", name = "show", description = "view the configured vxlan ports")
public class ShowVxlan extends OsgiCommandSupport {
    private static final Logger LOG = LoggerFactory.getLogger(ShowVxlan.class);
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
        LOG.debug("Executing show Vxlan command");
        List<Interface> vxlanList = interfaceManager.getVxlanInterfaces();
        if (!vxlanList.isEmpty()) {
            IfmCLIUtil.showVxlanHeaderOutput(session);
        }
        for (Interface iface : vxlanList) {
            InterfaceInfo ifaceState = interfaceManager.getInterfaceInfoFromOperationalDSCache(iface.getName());
            IfmCLIUtil.showVxlanOutput(iface, ifaceState, session);
        }
        return null;
    }
}
