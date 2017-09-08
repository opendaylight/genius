/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.hwvtep.statehelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical._switch.attributes.Tunnels;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwVTEPInterfaceStateRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(HwVTEPInterfaceStateRemoveHelper.class);

    public static List<ListenableFuture<Void>> removeExternalTunnel(DataBroker dataBroker,
            InstanceIdentifier<Tunnels> tunnelsInstanceIdentifier) {
        LOG.debug("Removing HwVTEP tunnel entries for tunnel: {}", tunnelsInstanceIdentifier);
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.delete(LogicalDatastoreType.CONFIGURATION, tunnelsInstanceIdentifier);
        return Collections.singletonList(transaction.submit());
    }
}
