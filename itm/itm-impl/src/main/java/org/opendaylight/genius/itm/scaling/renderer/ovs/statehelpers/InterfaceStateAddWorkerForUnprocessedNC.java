/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.scaling.renderer.ovs.statehelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceStateAddWorkerForUnprocessedNC implements Callable {
    private final InstanceIdentifier<FlowCapableNodeConnector> key;
    private final FlowCapableNodeConnector fcNodeConnectorNew;
    private final String interfaceName;
    private final DataBroker dataBroker;
    private final IdManagerService idManager;
    private final IMdsalApiManager mdsalApiManager;
    private final DPNTEPsInfoCache dpntePsInfoCache;

    public InterfaceStateAddWorkerForUnprocessedNC(final DataBroker dataBroker,  final IdManagerService idManager,
                                                   final IMdsalApiManager mdsalApiManager,
                                                   final InstanceIdentifier<FlowCapableNodeConnector> key,
                                                   final FlowCapableNodeConnector fcNodeConnectorNew,
                                                   final String portName,
                                                   final DPNTEPsInfoCache dpntePsInfoCache) {
        this.key = key;
        this.fcNodeConnectorNew = fcNodeConnectorNew;
        this.interfaceName = portName;
        this.dataBroker = dataBroker;
        this.idManager = idManager;
        this.mdsalApiManager = mdsalApiManager;
        this.dpntePsInfoCache = dpntePsInfoCache;
    }

    @Override
    public Object call() throws Exception {
        // If another renderer(for eg : CSS) needs to be supported, check can be performed here
        // to call the respective helpers.
        List<ListenableFuture<Void>> futures = OvsInterfaceStateAddHelper.addState(dataBroker, idManager,
                mdsalApiManager, key, interfaceName, fcNodeConnectorNew, dpntePsInfoCache);
        return futures;
    }

    @Override
    public String toString() {
        return "InterfaceStateAddWorker{fcNodeConnectorIdentifier=" + key + ", fcNodeConnectorNew="
                + fcNodeConnectorNew + ", interfaceName='" + interfaceName + '\'' + '}';
    }
}
