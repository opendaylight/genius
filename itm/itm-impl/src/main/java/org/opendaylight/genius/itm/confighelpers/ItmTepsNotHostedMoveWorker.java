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
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.listeners.ItmListenerUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ItmTepsNotHostedMoveWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger LOG = LoggerFactory.getLogger(ItmTepsNotHostedMoveWorker.class );
    private  List<Vteps> vTepList;
    private  String tzName;

    private  DataBroker dataBroker;


    public ItmTepsNotHostedMoveWorker(List<Vteps> vTepList, String tzName, DataBroker broker) {
        this.vTepList = vTepList;
        this.tzName = tzName;
        this.dataBroker = broker ;
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction wrTx = dataBroker.newWriteOnlyTransaction();
        List<Subnets> subnetList=new ArrayList<Subnets>();
        IpPrefix subnetMaskObj = ItmListenerUtils.getDummySubnet();
        IpAddress tepIpAddress = null;
        BigInteger dpnId = BigInteger.valueOf(0);

        LOG.trace("Move TEP from TepsNotHosted list to NBI configured TZ task is picked from DataStoreJobCoordinator for execution.");

        // Move TEP from TepsNotHosted list to NBI configured TZ.
        ItmListenerUtils.addVtepInITMConfigDS(subnetList, subnetMaskObj, vTepList, tepIpAddress, tzName, dpnId,
                ITMConstants.DUMMY_PORT, wrTx);

        futures.add(wrTx.submit());
        return futures;
    }
}
