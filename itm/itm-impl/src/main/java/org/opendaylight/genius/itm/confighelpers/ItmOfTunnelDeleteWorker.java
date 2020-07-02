/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.confighelpers;

import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;

public class ItmOfTunnelDeleteWorker {
    public ItmOfTunnelDeleteWorker(DataBroker dataBroker, JobCoordinator jobCoordinator, ItmConfig itmConfig,
                                   DirectTunnelUtils directTunnelUtils, OvsBridgeRefEntryCache ovsBridgeRefEntryCache,
                                   DataTreeEventCallbackRegistrar eventCallbackRegistrar) {
    }
}
