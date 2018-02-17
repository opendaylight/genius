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
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;

public final class OvsTunnelConfigRemoveHelper {

    private OvsTunnelConfigRemoveHelper() {
    }

    public static List<ListenableFuture<Void>> removeConfiguration(DataBroker dataBroker,
                                                                   Interface interfaceOld,
                                                                   IdManagerService idManager,
                                                                   IMdsalApiManager mdsalApiManager,
                                                                   ParentRefs parentRefs,
                                                                   TunnelStateCache tunnelStateCache) {
        //TODO
        return null;
    }
}
