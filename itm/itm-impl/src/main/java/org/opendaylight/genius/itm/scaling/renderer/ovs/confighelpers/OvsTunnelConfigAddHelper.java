/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;

public final class OvsTunnelConfigAddHelper {

    private OvsTunnelConfigAddHelper() {
    }

    public static List<ListenableFuture<Void>> addTunnelConfiguration(DataBroker dataBroker, Interface iface) {
        //TODO
        return null;
    }
}
