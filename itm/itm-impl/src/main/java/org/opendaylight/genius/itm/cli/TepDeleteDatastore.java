/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.opendaylight.genius.interfacemgr.util.ConfigIfmUtil;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.itm.op.rev150701.Tunnels;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.itm.op.rev150701.TunnelsState;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.itm.op.rev150701.tunnels_state.StateTunnelList;

@Command(scope = "tep", name = "deleteDatastore", description = "view the configured tunnel endpoints")
public class TepDeleteDatastore <T extends DataObject>  extends OsgiCommandSupport {
    private static final Logger logger = LoggerFactory.getLogger(TepDeleteDatastore.class);
    private IITMProvider itmProvider;

    public void setItmProvider(IITMProvider itmProvider) {
        this.itmProvider = itmProvider;
    }

    @Override
    protected Object doExecute() {
            DataBroker dataBroker =  itmProvider.getDataBroker();
            InstanceIdentifier< TransportZones > itmConfigPath = InstanceIdentifier.builder(TransportZones.class).build();
           // InstanceIdentifier<Tunnels> tunnelsConfigPath = InstanceIdentifier.builder(Tunnels.class).build();
            InstanceIdentifier<Interfaces> InterfacesConfigPath = InstanceIdentifier.builder(Interfaces.class).build();
           // InstanceIdentifier<TunnelsState> tnStateOpPath = InstanceIdentifier.builder(TunnelsState.class).build();
            InstanceIdentifier<InterfacesState> ifStateOpPath = InstanceIdentifier.builder(InterfacesState.class).build();
            InstanceIdentifier<Nodes> frmConfigPath = InstanceIdentifier.builder(Nodes.class).build();
            List<InstanceIdentifier<T>> allConfigPaths =
                            new ArrayList<>();
            allConfigPaths.add((InstanceIdentifier<T>) itmConfigPath);
            allConfigPaths.add((InstanceIdentifier<T>) InterfacesConfigPath);
            allConfigPaths.add((InstanceIdentifier<T>) frmConfigPath);
            //allConfigPaths.add((InstanceIdentifier<T>) tunnelsConfigPath);
            ItmUtils.asyncBulkRemove(dataBroker, LogicalDatastoreType.CONFIGURATION,allConfigPaths,
                            ItmUtils.DEFAULT_CALLBACK);
            List<InstanceIdentifier<T>> allOperationalPaths =
                            new ArrayList<>();
           // allOperationalPaths.add((InstanceIdentifier<T>) tnStateOpPath);
            allOperationalPaths.add((InstanceIdentifier<T>) ifStateOpPath);
            ItmUtils.asyncBulkRemove(dataBroker, LogicalDatastoreType.OPERATIONAL, allOperationalPaths,
                            ItmUtils.DEFAULT_CALLBACK);
            
        return null;
    }

}
