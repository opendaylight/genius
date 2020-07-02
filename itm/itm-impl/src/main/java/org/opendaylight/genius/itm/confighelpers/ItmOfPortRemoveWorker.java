/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.tep.config.OfDpnTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.tep.config.OfDpnTepKey;

public class ItmOfPortRemoveWorker implements Callable<List<? extends ListenableFuture<?>>> {
    public ItmOfPortRemoveWorker(Map<OfDpnTepKey, OfDpnTep> oldDpnTepList, DataBroker dataBroker,
                                 ItmOfTunnelDeleteWorker itmOfTunnelDeleteWorker) {
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        return null;
    }
}
