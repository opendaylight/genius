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
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.mdsal.binding.api.DataBroker;
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

    public OvsdbTepAddWorker(String tepIp, String strDpnId, String tzName,  boolean ofTunnel, DataBroker broker) {
        this.tepIp = tepIp;
        this.strDpid = strDpnId;
        this.tzName = tzName;
        this.ofTunnel = ofTunnel;
        this.dataBroker = broker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        LOG.trace("Add TEP task is picked from DataStoreJobCoordinator for execution.");
        // add TEP received from southbound OVSDB into ITM config DS.
        return OvsdbTepAddConfigHelper.addTepReceivedFromOvsdb(tepIp, strDpid, tzName, ofTunnel, dataBroker, txRunner);
    }
}
