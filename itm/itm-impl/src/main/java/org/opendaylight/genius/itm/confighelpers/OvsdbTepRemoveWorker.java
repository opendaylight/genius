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
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbTepRemoveWorker implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbTepRemoveWorker.class) ;

    private final String tepIp;
    private final String strDpid;
    private final String tzName;
    private final DataBroker dataBroker;
    private final OvsdbTepRemoveConfigHelper ovsdbTepRemoveConfigHelper;

    public OvsdbTepRemoveWorker(String tepIp, String strDpid, String tzName, DataBroker broker,
                                OvsdbTepRemoveConfigHelper ovsdbTepRemoveConfigHelper) {
        this.tepIp = tepIp;
        this.strDpid = strDpid;
        this.tzName = tzName;
        this.dataBroker = broker ;
        this.ovsdbTepRemoveConfigHelper = ovsdbTepRemoveConfigHelper;
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        WriteTransaction wrTx = dataBroker.newWriteOnlyTransaction();

        LOG.trace("Remove TEP task is picked from DataStoreJobCoordinator for execution.");

        // remove TEP received from southbound OVSDB from ITM config DS.
        ovsdbTepRemoveConfigHelper.removeTepReceivedFromOvsdb(tepIp, strDpid, tzName, dataBroker, wrTx);

        return Collections.singletonList(wrTx.submit());
    }
}
