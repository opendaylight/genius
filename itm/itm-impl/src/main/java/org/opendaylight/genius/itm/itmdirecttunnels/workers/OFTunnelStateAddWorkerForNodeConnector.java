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

import org.opendaylight.genius.itm.utils.OfTunnelChildInfo;
import org.opendaylight.genius.itm.utils.TunnelStateInfo;


public class OFTunnelStateAddWorkerForNodeConnector implements Callable<List<ListenableFuture<Void>>> {
    private final OFTunnelStateAddWorker ofTunnelStateAddWorker;
    private final TunnelStateInfo tunnelStateInfo;
    private final OfTunnelChildInfo ofTunnelChildInfo;

    public OFTunnelStateAddWorkerForNodeConnector(OFTunnelStateAddWorker ofTunnelStateAddWorker,
                                                  TunnelStateInfo tunnelStateInfo,
                                                  OfTunnelChildInfo ofTunnelChildInfo) {
        this.ofTunnelStateAddWorker = ofTunnelStateAddWorker;
        this.tunnelStateInfo = tunnelStateInfo;
        this.ofTunnelChildInfo = ofTunnelChildInfo;
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        // If another renderer(for eg : OVS) needs to be supported, check can be performed here
        // to call the respective helpers.
        return ofTunnelStateAddWorker.addState(tunnelStateInfo, ofTunnelChildInfo);
    }

    @Override
    public String toString() {
        return "TunnelStateAddWorkerForNodeConnector{tunnelStateInfo=" + tunnelStateInfo + '}';
    }
}