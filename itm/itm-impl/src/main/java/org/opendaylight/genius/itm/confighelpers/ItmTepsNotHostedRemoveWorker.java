/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmTepsNotHostedRemoveWorker implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTepsNotHostedRemoveWorker.class);

    private final IpAddress tepIpAddress;
    private final String tzName;
    private final BigInteger dpnId;
    private final DataBroker dataBroker;
    private final OvsdbTepRemoveConfigHelper ovsdbTepRemoveConfigHelper;

    public ItmTepsNotHostedRemoveWorker(String tzName, IpAddress tepIpAddress, BigInteger dpnId, DataBroker broker,
                                        OvsdbTepRemoveConfigHelper ovsdbTepRemoveConfigHelper) {
        this.tepIpAddress = tepIpAddress;
        this.tzName = tzName;
        this.dpnId = dpnId;
        this.dataBroker = broker ;
        this.ovsdbTepRemoveConfigHelper = ovsdbTepRemoveConfigHelper;
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        WriteTransaction wrTx = dataBroker.newWriteOnlyTransaction();

        LOG.trace("Remove TEP from TepsNotHosted list task is picked from DataStoreJobCoordinator for execution.");

        // Remove TEP from TepsNotHosted list.
        ovsdbTepRemoveConfigHelper.removeUnknownTzTepFromTepsNotHosted(tzName, tepIpAddress, dpnId, dataBroker, wrTx);

        return Collections.singletonList(wrTx.submit());
    }
}
