/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.itm.cache.TransportZonesCache;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbTepRemoveWorker implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbTepRemoveWorker.class) ;

    private final String tepIp;
    private final String strDpid;
    private final String tzName;
    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;
    private final TransportZonesCache transportZonesCache;

    public OvsdbTepRemoveWorker(String tepIp, String strDpid, String tzName, DataBroker broker,
                                final TransportZonesCache transportZonesCache) {
        this.tepIp = tepIp;
        this.strDpid = strDpid;
        this.tzName = tzName;
        this.dataBroker = broker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.transportZonesCache = transportZonesCache;
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {

        LOG.trace("Remove TEP task is picked from DataStoreJobCoordinator for execution.");
        TransportZone transportZone;
        // Case: TZ name is not given from OVS's other_config parameters.
        if (tzName == null) {
            // remove TEP into default-TZ
            transportZone = transportZonesCache.getTransportZoneFromCache(ITMConstants.DEFAULT_TRANSPORT_ZONE);
            if (transportZone == null) {
                LOG.error("Error: default-transport-zone is not yet created.");
                return Collections.emptyList();
            }
            LOG.trace("Remove TEP from default-transport-zone.");
        } else {
            // Case: Remove TEP from corresponding TZ created from Northbound.
            transportZone = transportZonesCache.getTransportZoneFromCache(tzName);
            if (transportZone == null) {
                // Case: TZ is not configured from Northbound, then remove TEP from "teps-in-not-hosted-transport-zone"
                LOG.trace("Removing TEP from teps-in-not-hosted-transport-zone list.");
            } else {
                LOG.trace("Remove TEP from transport-zone already configured by Northbound.");
            }
        }
        // remove TEP received from southbound OVSDB from ITM config DS.
        return OvsdbTepRemoveConfigHelper.removeTepReceivedFromOvsdb(tepIp, strDpid, tzName, transportZone, dataBroker,
                txRunner);
    }
}
