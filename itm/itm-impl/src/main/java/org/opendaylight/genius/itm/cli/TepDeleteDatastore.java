/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import java.util.*;
import org.apache.karaf.shell.commands.*;
import org.apache.karaf.shell.console.*;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.*;
import org.opendaylight.genius.itm.api.*;
import org.opendaylight.genius.itm.impl.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.*;
import org.opendaylight.yangtools.yang.binding.*;


@Command(scope = "tep", name = "deleteDatastore", description = "view the configured tunnel endpoints")
public class TepDeleteDatastore<T extends DataObject>  extends OsgiCommandSupport {
    private IITMProvider itmProvider;

    public void setItmProvider(IITMProvider itmProvider) {
        this.itmProvider = itmProvider;
    }

    @Override
    protected Object doExecute() {
            DataBroker dataBroker =  itmProvider.getDataBroker();
            InstanceIdentifier<TransportZones> itmConfigPath = InstanceIdentifier.builder(TransportZones.class)
                                                               .build();
           // InstanceIdentifier<Tunnels> tunnelsConfigPath = InstanceIdentifier.builder(Tunnels.class).build();
            InstanceIdentifier<Interfaces> InterfacesConfigPath = InstanceIdentifier.builder(Interfaces.class).build();
           // InstanceIdentifier<TunnelsState> tnStateOpPath = InstanceIdentifier.builder(TunnelsState.class).build();
            InstanceIdentifier<InterfacesState> ifStateOpPath = InstanceIdentifier.builder(InterfacesState.class)
                                                                .build();
            InstanceIdentifier<Nodes> frmConfigPath = InstanceIdentifier.builder(Nodes.class).build();
            List<InstanceIdentifier<T>> allConfigPaths = new ArrayList<>();
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
