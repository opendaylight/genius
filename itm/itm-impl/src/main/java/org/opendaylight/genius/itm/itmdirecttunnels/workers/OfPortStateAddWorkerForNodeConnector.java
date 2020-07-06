/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.workers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;

public class OfPortStateAddWorkerForNodeConnector implements Callable<List<? extends ListenableFuture<?>>> {
    private final OfPortStateAddWorker ofPortStateAddWorker;
    private final NodeConnectorInfo nodeConnectorInfo;
    private final ConcurrentMap<String, NodeConnectorInfo> ofPortStateInfo = new ConcurrentHashMap<>();

    public OfPortStateAddWorkerForNodeConnector(OfPortStateAddWorker ofPortStateAddWorker,
                                                NodeConnectorInfo nodeConnectorInfo) {
        this.ofPortStateAddWorker = ofPortStateAddWorker;
        this.nodeConnectorInfo = nodeConnectorInfo;
    }

    @Override
    public List<? extends ListenableFuture<?>> call() throws Exception {
        // If another renderer(for eg : OVS) needs to be supported, check can be performed here
        // to call the respective helpers.
        return ofPortStateAddWorker.addState(nodeConnectorInfo);
    }

    @Override
    public String toString() {
        return "OfPortStateAddWorkerForNodeConnector{"
                + "ofStateInfo=" + ofPortStateInfo
                + '}';
    }
}
