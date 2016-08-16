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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.rev160622.ResourceManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceManagerServiceProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceManagerServiceProvider.class);
    private ResourceManager resourceManager;

    private IdManagerService idManager;
    private RpcProviderRegistry rpcProviderRegistry;
    private BindingAwareBroker.RpcRegistration<ResourceManagerService> rpcRegistration;
    private DataBroker dataBroker;
    private String tablesName = "resource.tables.name";
    private String tablesStr = "resource.tables.startId";
    private String tablesEnd = "resource.tables.endId";
    private String groupsName = "resource.groups.name";
    private String groupsStr = "resource.groups.startId";
    private String groupsEnd = "resource.tables.endId";
    private String metersName = "resource.meters.name";
    private String metersStr = "resource.meters.startId";
    private String metersEnd = "resource.meters.endId";

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
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("ResourceManagerserviceProvider Session Initiated");
        dataBroker = session.getSALService(DataBroker.class);
        idManager = rpcProviderRegistry.getRpcService(IdManagerService.class);
        resourceManager = new ResourceManager(dataBroker, idManager);
        rpcRegistration = rpcProviderRegistry.addRpcImplementation(ResourceManagerService.class, resourceManager);
        createIdpools();
    }


    private void createIdpools() {
        //Create Tables Id Pool
        if (System.getProperty(tablesName) != null && System.getProperty(tablesStr) != null
                && System.getProperty(tablesEnd) != null) {
            idManager.createIdPool(new CreateIdPoolInputBuilder().setPoolName(System.getProperty(tablesName))
                    .setLow(Long.valueOf(System.getProperty(tablesStr)))
                    .setHigh(Long.valueOf(System.getProperty(tablesEnd))).build());
        } else {
            LOG.trace("Creating pool with default values");
            idManager.createIdPool(new CreateIdPoolInputBuilder().setPoolName("tables")
                    .setLow((long) 0).setHigh((long) 254).build());
        }

        //Create Groups Id Pool
        if (System.getProperty(groupsName) != null && System.getProperty(groupsStr) != null
                && System.getProperty(groupsEnd) != null) {
            idManager.createIdPool(new CreateIdPoolInputBuilder().setPoolName(System.getProperty(groupsName))
                    .setLow(Long.valueOf(System.getProperty(groupsStr)))
                    .setHigh(Long.valueOf(System.getProperty(groupsEnd))).build());
        } else {
            LOG.trace("Creating pool with default values");
            idManager.createIdPool(new CreateIdPoolInputBuilder().setPoolName("meters")
                    .setLow((long) 0).setHigh((long) 254).build());
        }

        //Create Meters Id Pool
        if (System.getProperty(metersName) != null && System.getProperty(metersStr) != null
                && System.getProperty(metersEnd) != null) {
            idManager.createIdPool(new CreateIdPoolInputBuilder().setPoolName(System.getProperty(metersName))
                    .setLow(Long.valueOf(System.getProperty(metersStr)))
                    .setHigh(Long.valueOf(System.getProperty(metersEnd))).build());
        } else {
            LOG.trace("Creating pool with default values");
            idManager.createIdPool(new CreateIdPoolInputBuilder().setPoolName("groups")
                    .setLow((long) 0).setHigh((long) 254).build());
        }
    }

    public ResourceManagerServiceProvider(RpcProviderRegistry rpcRegistry) {
        this.rpcProviderRegistry = rpcRegistry;
    }

    @Override
    public void close() throws Exception {
        rpcRegistration.close();
    }
}
