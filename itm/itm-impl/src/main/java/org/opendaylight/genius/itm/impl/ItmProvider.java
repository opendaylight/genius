/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.felix.service.command.CommandSession;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.cli.TepCommandHelper;
import org.opendaylight.genius.itm.listeners.cache.DpnTepsInfoListener;
import org.opendaylight.genius.itm.cli.TepException;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.listeners.InterfaceStateListener;
import org.opendaylight.genius.itm.listeners.TransportZoneListener;
import org.opendaylight.genius.itm.listeners.cache.StateTunnelListListener;
import org.opendaylight.genius.itm.listeners.TunnelMonitorChangeListener;
import org.opendaylight.genius.itm.listeners.TunnelMonitorIntervalListener;
import org.opendaylight.genius.itm.listeners.VtepConfigSchemaListener;
import org.opendaylight.genius.itm.listeners.cache.ItmMonitoringIntervalListener;
import org.opendaylight.genius.itm.listeners.cache.ItmMonitoringListener;
import org.opendaylight.genius.itm.monitoring.ItmTunnelEventListener;
import org.opendaylight.genius.itm.rpc.ItmManagerRpcService;
import org.opendaylight.genius.itm.snd.ITMStatusMonitor;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.utils.cache.DataStoreCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.VtepConfigSchemas;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.VtepConfigSchema;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.VtepConfigSchemaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.ItmRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddExternalTunnelEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddExternalTunnelEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.*;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmProvider implements BindingAwareProvider, AutoCloseable, IITMProvider /*,ItmStateService */{

    private static final Logger LOG = LoggerFactory.getLogger(ItmProvider.class);
    private IInterfaceManager interfaceManager;
    private ITMManager itmManager;
    private IMdsalApiManager mdsalManager;
    private DataBroker dataBroker;
    private NotificationPublishService notificationPublishService;
    private ItmManagerRpcService itmRpcService ;
    private IdManagerService idManager;
    private NotificationService notificationService;
    private TepCommandHelper tepCommandHelper;
    private TransportZoneListener tzChangeListener;
    private TunnelMonitorChangeListener tnlToggleListener;
    private TunnelMonitorIntervalListener tnlIntervalListener;
    private VtepConfigSchemaListener vtepConfigSchemaListener;
    private InterfaceStateListener ifStateListener;
    private RpcProviderRegistry rpcProviderRegistry;
    private static final ITMStatusMonitor itmStatusMonitor = ITMStatusMonitor.getInstance();
    private ItmTunnelEventListener itmStateListener;
    private ItmMonitoringListener itmMonitoringListener;
    private ItmMonitoringIntervalListener itmMonitoringIntervalListener;
    static short flag = 0;
    private StateTunnelListListener tunnelStateListener ;
    private DpnTepsInfoListener dpnTepsInfoListener ;
    public ItmProvider() {
        LOG.info("ItmProvider Before register MBean");
        itmStatusMonitor.registerMbean();
    }

    public void setRpcProviderRegistry(RpcProviderRegistry rpcProviderRegistry) {
        this.rpcProviderRegistry = rpcProviderRegistry;
    }

    public RpcProviderRegistry getRpcProviderRegistry() {
        return this.rpcProviderRegistry;
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("ItmProvider Session Initiated");
        itmStatusMonitor.reportStatus("STARTING");
        try {
            dataBroker = session.getSALService(DataBroker.class);
            idManager = getRpcProviderRegistry().getRpcService(IdManagerService.class);

            itmManager = new ITMManager(dataBroker);
            tzChangeListener = new TransportZoneListener(dataBroker, idManager) ;
            itmRpcService = new ItmManagerRpcService(dataBroker, idManager);
            vtepConfigSchemaListener = new VtepConfigSchemaListener(dataBroker);
            this.ifStateListener = new InterfaceStateListener(dataBroker);
            tnlToggleListener = new TunnelMonitorChangeListener(dataBroker);
            tnlIntervalListener = new TunnelMonitorIntervalListener(dataBroker);
            tepCommandHelper = new TepCommandHelper(dataBroker);
            getRpcProviderRegistry().addRpcImplementation(ItmRpcService.class, itmRpcService);
            itmRpcService.setMdsalManager(mdsalManager);
            itmManager.setMdsalManager(mdsalManager);
            itmManager.setNotificationPublishService(notificationPublishService);
            itmManager.setMdsalManager(mdsalManager);
            tzChangeListener.setMdsalManager(mdsalManager);
            tzChangeListener.setItmManager(itmManager);
            tzChangeListener.registerListener(LogicalDatastoreType.CONFIGURATION, dataBroker);
            tnlIntervalListener.registerListener(LogicalDatastoreType.CONFIGURATION, dataBroker);
            tnlToggleListener.registerListener(LogicalDatastoreType.CONFIGURATION, dataBroker);
            tepCommandHelper = new TepCommandHelper(dataBroker);
            tepCommandHelper.setInterfaceManager(interfaceManager);
            tepCommandHelper.configureTunnelType(ITMConstants.DEFAULT_TRANSPORT_ZONE,ITMConstants.TUNNEL_TYPE_VXLAN);
            itmStateListener =new ItmTunnelEventListener(dataBroker);
            createIdPool();
            itmStatusMonitor.reportStatus("OPERATIONAL");
            DataStoreCache.create(ITMConstants.ITM_MONIRORING_PARAMS_CACHE_NAME);
            itmMonitoringListener = new ItmMonitoringListener(dataBroker);
            itmMonitoringIntervalListener = new ItmMonitoringIntervalListener(dataBroker);
            DataStoreCache.create(ITMConstants.TUNNEL_STATE_CACHE_NAME) ;
            tunnelStateListener = new StateTunnelListListener(dataBroker);
            DataStoreCache.create(ITMConstants.DPN_TEPs_Info_CACHE_NAME) ;
            dpnTepsInfoListener = new DpnTepsInfoListener(dataBroker);
        } catch (Exception e) {
            LOG.error("Error initializing services", e);
            itmStatusMonitor.reportStatus("ERROR");
        }
    }

    public void setInterfaceManager(IInterfaceManager interfaceManager) {
        this.interfaceManager = interfaceManager;
    }

    public void setNotificationPublishService(NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
    }

    public void setMdsalApiManager(IMdsalApiManager mdsalMgr) {
        this.mdsalManager = mdsalMgr;
    }
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public void close() throws Exception {
        if (itmManager != null) {
            itmManager.close();
        }
        if (tzChangeListener != null) {
            tzChangeListener.close();
        }
        if (tnlIntervalListener != null) {
            tnlIntervalListener.close();
        }
        if(tnlToggleListener!= null){
            tnlToggleListener.close();
        }
        if(tunnelStateListener!= null){
            tunnelStateListener.close();
        }
        if(dpnTepsInfoListener!= null){
            dpnTepsInfoListener.close();
        }
        LOG.info("ItmProvider Closed");
    }

    private void createIdPool() {
        CreateIdPoolInput createPool = new CreateIdPoolInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setLow(ITMConstants.ITM_IDPOOL_START)
                .setHigh(new BigInteger(ITMConstants.ITM_IDPOOL_SIZE).longValue())
                .build();
        try {
            Future<RpcResult<Void>> result = idManager.createIdPool(createPool);
            if ((result != null) && (result.get().isSuccessful())) {
                LOG.debug("Created IdPool for ITM Service");
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to create idPool for ITM Service",e);
        }
    }

    @Override
    public DataBroker getDataBroker() {
        return dataBroker;
    }

    public void addExternalEndpoint(Class<? extends TunnelTypeBase> tunnelType, IpAddress dcgwIP) {
        AddExternalTunnelEndpointInput addExternalTunnelEndpointInput =
                new AddExternalTunnelEndpointInputBuilder().setTunnelType(tunnelType)
                        .setDestinationIp(dcgwIP).build();
        itmRpcService.addExternalTunnelEndpoint(addExternalTunnelEndpointInput);
    }

    public void remExternalEndpoint(Class<? extends TunnelTypeBase> tunnelType, IpAddress dcgwIP) {
        RemoveExternalTunnelEndpointInput removeExternalTunnelEndpointInput =
                new RemoveExternalTunnelEndpointInputBuilder().setTunnelType(tunnelType)
                        .setDestinationIp(dcgwIP).build();
        itmRpcService.removeExternalTunnelEndpoint(removeExternalTunnelEndpointInput);
    }
    @Override
    public void createLocalCache(BigInteger dpnId, String portName, Integer vlanId, String ipAddress, String subnetMask,
                                 String gatewayIp, String transportZone, CommandSession session) {
        if (tepCommandHelper != null) {
            try {
                tepCommandHelper.createLocalCache(dpnId, portName, vlanId, ipAddress, subnetMask, gatewayIp, transportZone, session);
            } catch (TepException e) {
                LOG.error(e.getMessage());
            }
        } else {
            LOG.trace("tepCommandHelper doesnt exist");
        }
    }

    @Override
    public void commitTeps() {
        try {
            tepCommandHelper.deleteOnCommit();
            tepCommandHelper.buildTeps();
        } catch (Exception e) {
            LOG.debug("unable to configure teps" + e.toString());
        }
    }

    @Override
    public void showTeps(CommandSession session) {
        try {
            tepCommandHelper.showTeps(itmManager.getTunnelMonitorEnabledFromConfigDS(), ItmUtils.determineMonitorInterval(this.dataBroker), session);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }
    }

    public void showState(List<StateTunnelList> tunnels,CommandSession session) {
        if (tunnels != null) {
            try {
                tepCommandHelper.showState(tunnels, itmManager.getTunnelMonitorEnabledFromConfigDS(), session);
            }catch(TepException e) {
                LOG.error(e.getMessage());
            }
        }else
            LOG.debug("No tunnels available");
    }

    @Override
    public void showCache( String cacheName) {
        tepCommandHelper.showCache(cacheName);
    }

    public void deleteVtep(BigInteger dpnId, String portName, Integer vlanId, String ipAddress, String subnetMask,
                           String gatewayIp, String transportZone, CommandSession session) {
        try {
            tepCommandHelper.deleteVtep(dpnId,  portName, vlanId, ipAddress, subnetMask, gatewayIp, transportZone, session);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    @Override
    public void configureTunnelType(String transportZone, String tunnelType) {
        LOG .debug("ItmProvider: configureTunnelType {} for transportZone {}", tunnelType, transportZone);
        tepCommandHelper.configureTunnelType(transportZone,tunnelType);
    }

    @Override
    public void addVtepConfigSchema(VtepConfigSchema vtepConfigSchema) {
        VtepConfigSchema validatedSchema = ItmUtils.validateForAddVtepConfigSchema(vtepConfigSchema,
                getAllVtepConfigSchemas());

        String schemaName = validatedSchema.getSchemaName();
        VtepConfigSchema existingSchema = getVtepConfigSchema(schemaName);
        if (existingSchema != null) {
            Preconditions.checkArgument(false, String.format("VtepConfigSchema [%s] already exists!", schemaName));
        }
        MDSALUtil.syncWrite(this.dataBroker, LogicalDatastoreType.CONFIGURATION,
                ItmUtils.getVtepConfigSchemaIdentifier(schemaName), validatedSchema);
        LOG.debug("Vtep config schema {} added to config DS", schemaName);
    }

    @Override
    public VtepConfigSchema getVtepConfigSchema(String schemaName) {
        Optional<VtepConfigSchema> schema = ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                ItmUtils.getVtepConfigSchemaIdentifier(schemaName), this.dataBroker);
        if (schema.isPresent()) {
            return schema.get();
        }
        return null;
    }

    @Override
    public List<VtepConfigSchema> getAllVtepConfigSchemas() {
        Optional<VtepConfigSchemas> schemas = ItmUtils.read(LogicalDatastoreType.CONFIGURATION,
                ItmUtils.getVtepConfigSchemasIdentifier(), this.dataBroker);
        if (schemas.isPresent()) {
            return schemas.get().getVtepConfigSchema();
        }
        return null;
    }

    @Override
    public void updateVtepSchema(String schemaName, List<BigInteger> lstDpnsForAdd, List<BigInteger> lstDpnsForDelete) {
        LOG.trace("Updating VTEP schema {} by adding DPN's {} and deleting DPN's {}.", schemaName, lstDpnsForAdd,
                lstDpnsForDelete);

        VtepConfigSchema schema = ItmUtils.validateForUpdateVtepSchema(schemaName, lstDpnsForAdd, lstDpnsForDelete,
                this);
        VtepConfigSchemaBuilder builder = new VtepConfigSchemaBuilder(schema);
       /* if (ItmUtils.getDpnIdList(schema.getDpnIds()).isEmpty()) {
            builder.setDpnIds(schema.getDpnIds());
        } else {*/
            if (lstDpnsForAdd != null && !lstDpnsForAdd.isEmpty()) {
                List<BigInteger> originalDpnList = ItmUtils.getDpnIdList(schema.getDpnIds()) ;
                originalDpnList.addAll(lstDpnsForAdd) ;
                builder.setDpnIds(ItmUtils.getDpnIdsListFromBigInt(originalDpnList));
            }
            if (lstDpnsForDelete != null && !lstDpnsForDelete.isEmpty()) {
                List<BigInteger> originalDpnList = ItmUtils.getDpnIdList(schema.getDpnIds()) ;
                originalDpnList.removeAll(lstDpnsForDelete) ;
                builder.setDpnIds(ItmUtils.getDpnIdsListFromBigInt(originalDpnList)) ;
                // schema.setDpnIds(ItmUtils.getDpnIdsListFromBigInt(ItmUtils.getDpnIdList(schema.getDpnIds()).removeAll(lstDpnsForAdd)));
            }
       // }
        schema = builder.build();
        MDSALUtil.syncWrite(this.dataBroker, LogicalDatastoreType.CONFIGURATION,
                ItmUtils.getVtepConfigSchemaIdentifier(schemaName), schema);
        LOG.debug("Vtep config schema {} updated to config DS with DPN's {}", schemaName, ItmUtils.getDpnIdList(schema.getDpnIds()));
    }

    @Override
    public void deleteAllVtepSchemas() {
        List<VtepConfigSchema> lstSchemas = getAllVtepConfigSchemas();
        if (lstSchemas != null && !lstSchemas.isEmpty()) {
            for (VtepConfigSchema schema : lstSchemas) {
                MDSALUtil.syncDelete(this.dataBroker, LogicalDatastoreType.CONFIGURATION,
                        ItmUtils.getVtepConfigSchemaIdentifier(schema.getSchemaName()));
            }
        }
        LOG.debug("Deleted all Vtep schemas from config DS");
    }

    public void configureTunnelMonitorParams(boolean monitorEnabled, String monitorProtocol) {
        tepCommandHelper.configureTunnelMonitorParams(monitorEnabled, monitorProtocol);
    }

    public void configureTunnelMonitorInterval(int interval) {
        tepCommandHelper.configureTunnelMonitorInterval(interval);
    }
    
    public boolean validateIP (final String ip){
        if (ip == null || ip.equals("")) {
            return false;
        }
        final String PATTERN =
                "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }
}
