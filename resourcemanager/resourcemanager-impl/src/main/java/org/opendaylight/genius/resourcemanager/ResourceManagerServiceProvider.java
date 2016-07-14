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
    private String TABLESNAME = "resource.tables.name";
    private String TABLESSTART = "resource.tables.startId";
    private String TABLESEND = "resource.tables.endId";
    private String GROUPSNAME = "resource.groups.name";
    private String GROUPSSTART = "resource.groups.startId";
    private String GROUPSEND = "resource.tables.endId";
    private String METERSNAME = "resource.meters.name";
    private String METERSSTART = "resource.meters.startId";
    private String METERSEND = "resource.meters.endId";

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
        if(System.getProperty(TABLESNAME)!=null && System.getProperty(TABLESSTART)!=null
                && System.getProperty(TABLESEND)!=null){
            String tName = System.getProperty(TABLESNAME);
            long tStart = Long.valueOf(System.getProperty(TABLESSTART));
            long tEnd = Long.valueOf(System.getProperty(TABLESEND));
            idManager.createIdPool(new CreateIdPoolInputBuilder().setPoolName(tName)
                    .setLow(tStart).setHigh(tEnd).build());
        } else{
            LOG.error("Tables Id Pool cannot be created due to null parameters");
            LOG.trace("Creating pool with default values");
            idManager.createIdPool(new CreateIdPoolInputBuilder().setPoolName("tables")
                    .setLow((long) 0).setHigh((long) 254).build());
        }

        //Create Groups Id Pool
        if(System.getProperty(GROUPSNAME)!=null && System.getProperty(GROUPSSTART)!=null
                && System.getProperty(GROUPSEND)!=null){
            String gName = System.getProperty(GROUPSNAME);
            long gStart = Long.valueOf(System.getProperty(GROUPSSTART));
            long gEnd = Long.valueOf(System.getProperty(GROUPSEND));
            idManager.createIdPool(new CreateIdPoolInputBuilder().setPoolName(gName)
                    .setLow(gStart).setHigh(gEnd).build());
        } else{
            LOG.error("Groups Id Pool cannot be created due to null parameters");
            LOG.trace("Creating pool with default values");
            idManager.createIdPool(new CreateIdPoolInputBuilder().setPoolName("meters")
                    .setLow((long) 0).setHigh((long) 254).build());
        }

        //Create Meters Id Pool
        if(System.getProperty(METERSNAME)!=null && System.getProperty(METERSSTART)!=null
                && System.getProperty(METERSEND)!=null){
            String mName = System.getProperty(METERSNAME);
            long mStart = Long.valueOf(System.getProperty(METERSSTART));
            long mEnd = Long.valueOf(System.getProperty(METERSEND));
            idManager.createIdPool(new CreateIdPoolInputBuilder().setPoolName(mName)
                    .setLow(mStart).setHigh(mEnd).build());
        } else{
            LOG.error("Meters Id Pool cannot be created due to null parameters");
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
