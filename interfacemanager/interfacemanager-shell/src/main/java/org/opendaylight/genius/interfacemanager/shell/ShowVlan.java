/*
 * Copyright (c) 2016 - 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "vlan", name = "show", description = "view the configured vlan ports")
public class ShowVlan extends OsgiCommandSupport {
    private static final Logger LOG = LoggerFactory.getLogger(ShowVlan.class);
    private IInterfaceManager interfaceManager;

    public void setInterfaceManager(IInterfaceManager interfaceManager) {
        this.interfaceManager = interfaceManager;
    }

    @Override
    protected Object doExecute() {
        LOG.debug("Executing show VLAN command");
        List<Interface> vlanList = interfaceManager.getVlanInterfaces();
        LOG.debug("vlanInterface list fetched {}", vlanList);
        if (!vlanList.isEmpty()) {
            IfmCLIUtil.showVlanHeaderOutput(session);
        }
        for (Interface iface : vlanList) {
            InterfaceInfo ifaceState = interfaceManager.getInterfaceInfoFromOperationalDataStore(iface.getName());
            IfmCLIUtil.showVlanOutput(ifaceState, iface);
        }
        return null;
    }
}
