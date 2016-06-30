/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.resourcemanager;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.ResourceManagerService;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.data.api.schema.tree.SynchronizedDataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;


public class ResourceManagerServiceProvider implements BindingAwareProvider,
            AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceManagerServiceProvider.class);
    private ResourceManager resourceManager;

    private IdManagerService idManager;
    private RpcProviderRegistry rpcProviderRegistry;
    private BindingAwareBroker.RpcRegistration<ResourceManagerService> rpcRegistration;
    private DataBroker dataBroker;

    public RpcProviderRegistry getRpcProviderRegistry() {
        return rpcProviderRegistry;
    }

    public void setRpcProviderRegistry(RpcProviderRegistry rpcProviderRegistry) {
        this.rpcProviderRegistry = rpcProviderRegistry;
    }

    public void setIdManager(IdManagerService idManager) {
        this.idManager = idManager;
    }

    @Override
    public void onSessionInitiated(ProviderContext session){
        LOG.info("ResourceManagerserviceProvider Session Initiated");
        try {
            dataBroker = session.getSALService(DataBroker.class);
            idManager = rpcProviderRegistry.getRpcService(IdManagerService.class);
            resourceManager = new ResourceManager(dataBroker, idManager);
            rpcRegistration = getRpcProviderRegistry().addRpcImplementation(ResourceManagerService.class, resourceManager);
            createIdpools();
        } catch (Exception e) {
            LOG.error("Error initializing services", e);
        }
    }

    private void createIdpools() {
        //TODO Create pools for tables, groups and meters

    }

    public ResourceManagerServiceProvider(RpcProviderRegistry rpcRegistry) {
        this.rpcProviderRegistry = rpcRegistry;
    }

    @Override
    public void close() throws Exception {
        rpcRegistration.close();

        }
    }





