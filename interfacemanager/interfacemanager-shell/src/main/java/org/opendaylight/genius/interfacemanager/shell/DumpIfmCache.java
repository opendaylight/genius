/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.shell;

import java.util.Map;
import java.util.Map.Entry;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "ifm-cache", name = "show", description = "view the ifm caches")
public class DumpIfmCache extends OsgiCommandSupport {
    private static final Logger LOG = LoggerFactory.getLogger(DumpIfmCache.class);
    private IInterfaceManager interfaceManager;

    public void setInterfaceManager(IInterfaceManager interfaceManager) {
        this.interfaceManager = interfaceManager;
    }

    @Override
    protected Object doExecute() {
        LOG.debug("Executing show ifm-cache command");
        Map<String, OvsdbTerminationPointAugmentation> tpMap = interfaceManager.getTerminationPointCache();
        Map<String, Interface.OperStatus> bfdMap = interfaceManager.getBfdStateCache();

        if (!tpMap.isEmpty()) {
            IfmCLIUtil.showInterfaceToTpHeader(session);
        }
        for (Entry<String, OvsdbTerminationPointAugmentation> tpEntry: tpMap.entrySet()) {
            IfmCLIUtil.showInterfaceToTpOutput(tpEntry.getKey(), tpEntry.getValue(), session);
        }

        if (!bfdMap.isEmpty()) {
            IfmCLIUtil.printBfdCachesHeader(session);
        }
        for (Entry<String, Interface.OperStatus> bfdEntry: bfdMap.entrySet()) {
            IfmCLIUtil.printBfdCachesOutput(bfdEntry.getKey(), bfdEntry.getValue(), session);
        }
        return null;
    }
}
