/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
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

public class OvsdbTepAddWorker implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbTepAddWorker.class) ;

    private final String tepIp;
    private final String strDpid;
    private final String tzName;
    private final boolean ofTunnel;
    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;
    private final TransportZonesCache transportZonesCache;

    public OvsdbTepAddWorker(String tepIp, String strDpnId, String tzName,  boolean ofTunnel, DataBroker broker,
                             final TransportZonesCache transportZonesCache) {
        this.tepIp = tepIp;
        this.strDpid = strDpnId;
        this.tzName = tzName;
        this.ofTunnel = ofTunnel;
        this.dataBroker = broker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.transportZonesCache = transportZonesCache;
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        LOG.trace("Add TEP task is picked from DataStoreJobCoordinator for execution.");
        TransportZone transportZone;
        // Case: TZ name is not given with OVS TEP.
        if (tzName == null) {
            // add TEP into default-TZ
            transportZone = transportZonesCache.getTransportZoneFromCache(ITMConstants.DEFAULT_TRANSPORT_ZONE);
            if (transportZone == null) {
                // Case: default-TZ is not yet created, then add TEP into "teps-in-not-hosted-transport-zone"
                LOG.trace("Adding TEP with default TZ into teps-in-not-hosted-transport-zone.");
            }
            LOG.trace("Add TEP into default-transport-zone.");
        } else {
            // Case: Add TEP into corresponding TZ created from Northbound.
            transportZone = transportZonesCache.getTransportZoneFromCache(tzName);
            if (transportZone == null) {
                // Case: TZ is not configured from Northbound, then add TEP into "teps-in-not-hosted-transport-zone"
                LOG.trace("Adding TEP with unknown TZ into teps-in-not-hosted-transport-zone.");

            } else {
                LOG.trace("Add TEP into transport-zone already configured by Northbound.");
            }
        }
        // add TEP received from southbound OVSDB into ITM config DS.
        return OvsdbTepAddConfigHelper.addTepReceivedFromOvsdb(tepIp, strDpid, tzName, transportZone, ofTunnel,
                dataBroker, txRunner);
    }
}
