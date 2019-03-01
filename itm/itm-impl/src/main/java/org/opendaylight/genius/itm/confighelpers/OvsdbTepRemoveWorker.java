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
import org.opendaylight.genius.itm.cache.TepsInNotHostedTransportZoneCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbTepRemoveWorker implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbTepRemoveWorker.class) ;

    private final String tepIp;
    private final String strDpid;
    private final String tzName;
    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;
    private final TepsInNotHostedTransportZoneCache tepsInNotHostedTransportZoneCache;

    public OvsdbTepRemoveWorker(String tepIp, String strDpid, String tzName,
                                TepsInNotHostedTransportZoneCache tepsInNotHostedTransportZoneCache,
                                DataBroker broker) {
        this.tepIp = tepIp;
        this.strDpid = strDpid;
        this.tzName = tzName;
        this.dataBroker = broker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.tepsInNotHostedTransportZoneCache = tepsInNotHostedTransportZoneCache;
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {

        LOG.trace("Remove TEP task is picked from DataStoreJobCoordinator for execution.");

        // remove TEP received from southbound OVSDB from ITM config DS.
        return OvsdbTepRemoveConfigHelper.removeTepReceivedFromOvsdb(tepIp, strDpid, tzName,
                tepsInNotHostedTransportZoneCache, dataBroker, txRunner);
    }
}
