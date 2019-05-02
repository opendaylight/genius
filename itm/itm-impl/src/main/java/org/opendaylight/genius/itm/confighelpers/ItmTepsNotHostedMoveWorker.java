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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Vteps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmTepsNotHostedMoveWorker implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTepsNotHostedMoveWorker.class);

    private final  List<Vteps> vtepsList;
    private final  String tzName;
    private final ManagedNewTransactionRunner txRunner;

    public ItmTepsNotHostedMoveWorker(List<Vteps> vtepsList, String tzName, ManagedNewTransactionRunner txRunner) {
        this.vtepsList = vtepsList;
        this.tzName = tzName;
        this.txRunner = txRunner;
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        LOG.trace("Move TEP from TepsNotHosted list to NBI configured TZ task is picked from "
                + "DataStoreJobCoordinator for execution.");

        // Move TEP from TepsNotHosted list to NBI configured TZ.
        return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore
            .CONFIGURATION, tx -> OvsdbTepAddConfigHelper.addVtepInITMConfigDS(
            vtepsList, null /*tepIpAddress*/, tzName, BigInteger.ZERO,
            false, tx)));
    }
}
