/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import com.google.common.util.concurrent.Futures;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Command(scope = "tep", name = "deleteDatastore", description = "deleting all tep data from datastore")
public class TepDeleteDatastore<T extends DataObject>  extends OsgiCommandSupport {
    private IITMProvider itmProvider;

    public void setItmProvider(IITMProvider itmProvider) {
        this.itmProvider = itmProvider;
    }

    @Override
    @SuppressFBWarnings("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
    protected Object doExecute() {
        InstanceIdentifier<TransportZones> itmConfigPath =
                InstanceIdentifier.builder(TransportZones.class).build();
        InstanceIdentifier<Interfaces> interfacesConfigPath =
                InstanceIdentifier.builder(Interfaces.class).build();
        final InstanceIdentifier<InterfacesState> ifStateOpPath =
                InstanceIdentifier.builder(InterfacesState.class).build();
        InstanceIdentifier<Nodes> frmConfigPath =
                InstanceIdentifier.builder(Nodes.class).build();
        List<InstanceIdentifier<T>> allConfigPaths = new ArrayList<>();
        allConfigPaths.add((InstanceIdentifier<T>) itmConfigPath);
        allConfigPaths.add((InstanceIdentifier<T>) interfacesConfigPath);
        allConfigPaths.add((InstanceIdentifier<T>) frmConfigPath);
        //allConfigPaths.add((InstanceIdentifier<T>) tunnelsConfigPath);
        final ManagedNewTransactionRunner txrunner = new ManagedNewTransactionRunnerImpl(itmProvider.getDataBroker());
        Futures.addCallback(txrunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.CONFIGURATION,
            tx -> allConfigPaths.forEach(tx::delete)), ItmUtils.DEFAULT_WRITE_CALLBACK);
        List<InstanceIdentifier<T>> allOperationalPaths = new ArrayList<>();
        // allOperationalPaths.add((InstanceIdentifier<T>) tnStateOpPath);
        allOperationalPaths.add((InstanceIdentifier<T>) ifStateOpPath);
        Futures.addCallback(txrunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.OPERATIONAL,
            tx -> allOperationalPaths.forEach(tx::delete)), ItmUtils.DEFAULT_WRITE_CALLBACK);
        return null;
    }
}
