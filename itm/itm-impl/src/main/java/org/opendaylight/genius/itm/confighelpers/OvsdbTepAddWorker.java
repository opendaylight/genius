/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.itm.listeners.ItmListenerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class OvsdbTepAddWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbTepAddWorker.class ) ;
    private String tepIp;
    private String strDpnId;
    private String tzName;
    private DataBroker dataBroker;

    public OvsdbTepAddWorker(String tepIp, String strDpnId, String tzName, DataBroker broker) {
        this.tepIp = tepIp;
        this.strDpnId = strDpnId;
        this.tzName = tzName;
        this.dataBroker = broker;
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction wrTx = dataBroker.newWriteOnlyTransaction();

        LOG.trace("Add TEP task is picked from DataStoreJobCoordinator for execution.");

        // add TEP received from southbound OVSDB into ITM config DS.
        ItmListenerUtils.addTepReceivedFromOvsdb(tepIp, strDpnId, tzName, dataBroker, wrTx);

        futures.add(wrTx.submit());
        return futures;
    }
}
