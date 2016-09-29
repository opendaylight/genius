/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.idmanager;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IdManagerServiceProvider implements BindingAwareProvider,
            AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(IdManagerServiceProvider.class);
    private IdManager idManager;
    private RpcProviderRegistry rpcProviderRegistry;
    private LockManagerService lockManager;
    private IdPoolListener idPoolListener;

    public RpcProviderRegistry getRpcProviderRegistry() {
        return rpcProviderRegistry;
    }

    public void setRpcProviderRegistry(RpcProviderRegistry rpcProviderRegistry) {
        this.rpcProviderRegistry = rpcProviderRegistry;
    }

    public void setLockManager(LockManagerService lockManager) {
        this.lockManager = lockManager;
    }

    @Override
    public void onSessionInitiated(ProviderContext session){
        LOG.info("IDManagerserviceProvider Session Initiated");
        try {
            final  DataBroker dataBroker = session.getSALService(DataBroker.class);
            idManager = new IdManager(dataBroker, lockManager);
            idPoolListener = new IdPoolListener(dataBroker, idManager);
            idPoolListener.registerListener(LogicalDatastoreType.CONFIGURATION, dataBroker);
            final BindingAwareBroker.RpcRegistration<IdManagerService> rpcRegistration = getRpcProviderRegistry().addRpcImplementation(IdManagerService.class, idManager);
        } catch (Exception e) {
            LOG.error("Error initializing services", e);
        }
    }

    public IdManagerServiceProvider(RpcProviderRegistry rpcRegistry) {
        this.rpcProviderRegistry = rpcRegistry;
    }

    @Override
    public void close() throws Exception {
        idManager.close();
    }
}