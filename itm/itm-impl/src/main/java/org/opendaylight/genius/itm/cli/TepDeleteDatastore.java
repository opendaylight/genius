/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "tep", name = "deleteDatastore", description = "deleting all tep data from datastore")
public class TepDeleteDatastore<T extends DataObject>  extends OsgiCommandSupport {
    private static final Logger LOG = LoggerFactory.getLogger(TepDeleteDatastore.class);
    private IITMProvider itmProvider;

    public void setItmProvider(IITMProvider itmProvider) {
        this.itmProvider = itmProvider;
    }

    @Override
    protected Object doExecute() {
        final DataBroker dataBroker =  itmProvider.getDataBroker();
        InstanceIdentifier<TransportZones> itmConfigPath =
                InstanceIdentifier.builder(TransportZones.class).build();
        InstanceIdentifier<Interfaces> interfacesConfigPath =
                InstanceIdentifier.builder(Interfaces.class).build();
        final InstanceIdentifier<InterfacesState> ifStateOpPath =
                InstanceIdentifier.builder(InterfacesState.class).build();
        InstanceIdentifier<Nodes> frmConfigPath =
                InstanceIdentifier.builder(Nodes.class).build();
        List<InstanceIdentifier<T>> allConfigPaths =
                        new ArrayList<>();
        allConfigPaths.add((InstanceIdentifier<T>) itmConfigPath);
        allConfigPaths.add((InstanceIdentifier<T>) interfacesConfigPath);
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
