/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.workers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.genius.itm.utils.TunnelStateInfo;

public class TunnelStateAddWorkerForNodeConnector implements Callable<List<ListenableFuture<Void>>> {
    private final TunnelStateAddWorker tunnelStateAddWorker;
    private final TunnelStateInfo tunnelStateInfo;

    public TunnelStateAddWorkerForNodeConnector(TunnelStateAddWorker tunnelStateAddWorker,
                                                TunnelStateInfo tunnelStateInfo) {
        this.tunnelStateAddWorker = tunnelStateAddWorker;
        this.tunnelStateInfo = tunnelStateInfo;
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        // If another renderer(for eg : OVS) needs to be supported, check can be performed here
        // to call the respective helpers.
        return tunnelStateAddWorker.addState(tunnelStateInfo);
    }

    @Override
    public String toString() {
        return "TunnelStateAddWorkerForNodeConnector{tunnelStateInfo=" + tunnelStateInfo + '}';
    }
}