/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import com.google.common.base.Preconditions;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.felix.service.command.CommandSession;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.cli.TepCommandHelper;
import org.opendaylight.genius.itm.cli.TepException;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.listeners.InterfaceStateListener;
import org.opendaylight.genius.itm.listeners.OvsdbNodeListener;
import org.opendaylight.genius.itm.listeners.TransportZoneListener;
import org.opendaylight.genius.itm.listeners.TunnelMonitorChangeListener;
import org.opendaylight.genius.itm.listeners.TunnelMonitorIntervalListener;
import org.opendaylight.genius.itm.listeners.VtepConfigSchemaListener;
import org.opendaylight.genius.itm.listeners.cache.DpnTepsInfoListener;
import org.opendaylight.genius.itm.listeners.cache.ItmMonitoringIntervalListener;
import org.opendaylight.genius.itm.listeners.cache.ItmMonitoringListener;
import org.opendaylight.genius.itm.listeners.cache.StateTunnelListListener;
import org.opendaylight.genius.itm.monitoring.ItmTunnelEventListener;
import org.opendaylight.genius.itm.rpc.ItmManagerRpcService;
import org.opendaylight.genius.itm.snd.ITMStatusMonitor;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.VtepConfigSchemas;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.VtepConfigSchema;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.VtepConfigSchemaBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddExternalTunnelEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddExternalTunnelEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointInputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmProvider implements AutoCloseable, IITMProvider /*,ItmStateService */ {

    private static final Logger LOG = LoggerFactory.getLogger(ItmProvider.class);

    private final ITMManager itmManager;
    private final DataBroker dataBroker;
    private final ItmManagerRpcService itmRpcService ;
    private final IdManagerService idManager;
    private final TepCommandHelper tepCommandHelper;
    private final TransportZoneListener tzChangeListener;
    private final TunnelMonitorChangeListener tnlToggleListener;
    private final TunnelMonitorIntervalListener tnlIntervalListener;
    private final VtepConfigSchemaListener vtepConfigSchemaListener;
    private final InterfaceStateListener ifStateListener;
    private RpcProviderRegistry rpcProviderRegistry;
    private static final ITMStatusMonitor ITM_STAT_MON = ITMStatusMonitor.getInstance();
    private final ItmTunnelEventListener itmStateListener;
    private final ItmMonitoringListener itmMonitoringListener;
    private final ItmMonitoringIntervalListener itmMonitoringIntervalListener;
    private final OvsdbNodeListener ovsdbChangeListener;
    static short flag = 0;
    private final StateTunnelListListener tunnelStateListener ;
    private final DpnTepsInfoListener dpnTepsInfoListener ;

    @Inject
    public ItmProvider(DataBroker dataBroker,
                       DpnTepsInfoListener dpnTepsInfoListener,
                       IdManagerService idManagerService,
                       InterfaceStateListener interfaceStateListener,
                       ITMManager itmManager,
                       ItmManagerRpcService itmManagerRpcService,
                       ItmMonitoringListener itmMonitoringListener,
                       ItmMonitoringIntervalListener itmMonitoringIntervalListener,
                       ItmTunnelEventListener itmTunnelEventListener,
                       StateTunnelListListener stateTunnelListListener,
                       TepCommandHelper tepCommandHelper,
                       TunnelMonitorChangeListener tunnelMonitorChangeListener,
                       TunnelMonitorIntervalListener tunnelMonitorIntervalListener,
                       TransportZoneListener transportZoneListener,
                       VtepConfigSchemaListener vtepConfigSchemaListener,
                       OvsdbNodeListener ovsdbNodeListener) {
        LOG.info("ItmProvider Before register MBean");
        ITM_STAT_MON.registerMbean();
        this.dataBroker = dataBroker;
        this.dpnTepsInfoListener = dpnTepsInfoListener;
        this.idManager = idManagerService;
        this.ifStateListener = interfaceStateListener;
        this.itmManager = itmManager;
        this.itmRpcService = itmManagerRpcService;
        this.itmMonitoringListener = itmMonitoringListener;
        this.itmMonitoringIntervalListener = itmMonitoringIntervalListener;
        this.itmStateListener = itmTunnelEventListener;
        this.tunnelStateListener = stateTunnelListListener;
        this.tepCommandHelper = tepCommandHelper;
        this.tnlToggleListener = tunnelMonitorChangeListener;
        this.tnlIntervalListener = tunnelMonitorIntervalListener;
        this.tzChangeListener = transportZoneListener;
        this.vtepConfigSchemaListener = vtepConfigSchemaListener;
        this.ovsdbChangeListener = ovsdbNodeListener;
        ITMBatchingUtils.registerWithBatchManager(this.dataBroker);
    }

    @PostConstruct
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void start() {
        try {
            ITM_STAT_MON.reportStatus("STARTING");
            createIdPool();
            LOG.info("ItmProvider Started");
            ITM_STAT_MON.reportStatus("OPERATIONAL");
        } catch (Exception ex) {
            ITM_STAT_MON.reportStatus("ERROR");
        }
    }

    @Override
    @PreDestroy
    public void close() {
        ITM_STAT_MON.unregisterMbean();
        if (itmManager != null) {
            itmManager.close();
        }
        if (tzChangeListener != null) {
            tzChangeListener.close();
        }
        if (tnlIntervalListener != null) {
            tnlIntervalListener.close();
        }
        if (tnlToggleListener != null) {
            tnlToggleListener.close();
        }
        if (tunnelStateListener != null) {
            tunnelStateListener.close();
        }
        if (dpnTepsInfoListener != null) {
            dpnTepsInfoListener.close();
        }
        if (ovsdbChangeListener != null) {
            ovsdbChangeListener.close();
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
            if (result != null && result.get().isSuccessful()) {
                LOG.debug("Created IdPool for ITM Service");
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to create idPool for ITM Service ", e);
        }
    }

    @Override
    public DataBroker getDataBroker() {
        return dataBroker;
    }

    @Override
    public void addExternalEndpoint(Class<? extends TunnelTypeBase> tunnelType, IpAddress dcgwIP) {
        AddExternalTunnelEndpointInput addExternalTunnelEndpointInput =
                new AddExternalTunnelEndpointInputBuilder().setTunnelType(tunnelType)
                        .setDestinationIp(dcgwIP).build();
        itmRpcService.addExternalTunnelEndpoint(addExternalTunnelEndpointInput);
    }

    @Override
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
                tepCommandHelper.createLocalCache(dpnId, portName, vlanId, ipAddress, subnetMask,
                        gatewayIp, transportZone, session);
            } catch (TepException e) {
                LOG.error(e.getMessage());
            }
        } else {
            LOG.trace("tepCommandHelper doesnt exist");
        }
    }

    @Override
    public void commitTeps() {
        tepCommandHelper.deleteOnCommit();
        tepCommandHelper.buildTeps();
    }

    @Override
    public void showTeps(CommandSession session) {
        try {
            tepCommandHelper.showTeps(itmManager.getTunnelMonitorEnabledFromConfigDS(),
                    ItmUtils.determineMonitorInterval(this.dataBroker), session);
        } catch (TepException e) {
            LOG.error(e.getMessage());
        }
    }

    @Override
    public void showState(List<StateTunnelList> tunnels,CommandSession session) {
        if (tunnels != null) {
            try {
                tepCommandHelper.showState(tunnels, itmManager.getTunnelMonitorEnabledFromConfigDS(), session);
            } catch (TepException e) {
                LOG.error(e.getMessage());
            }
        } else {
            LOG.debug("No tunnels available");
        }
    }

    @Override
    public void showCache(String cacheName) {
        tepCommandHelper.showCache(cacheName);
    }

    @Override
    public void deleteVtep(BigInteger dpnId, String portName, Integer vlanId, String ipAddress, String subnetMask,
                           String gatewayIp, String transportZone, CommandSession session) {
        try {
            tepCommandHelper.deleteVtep(dpnId,  portName, vlanId, ipAddress,
                    subnetMask, gatewayIp, transportZone, session);
        } catch (TepException e) {
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
        return ItmUtils.read(LogicalDatastoreType.CONFIGURATION, ItmUtils.getVtepConfigSchemaIdentifier(schemaName),
                this.dataBroker).orNull();
    }

    @Override
    public List<VtepConfigSchema> getAllVtepConfigSchemas() {
        return ItmUtils.read(LogicalDatastoreType.CONFIGURATION, ItmUtils.getVtepConfigSchemasIdentifier(),
                this.dataBroker).toJavaUtil().map(VtepConfigSchemas::getVtepConfigSchema).orElse(null);
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
            List<BigInteger> originalDpnList = ItmUtils.getDpnIdList(schema.getDpnIds());
            originalDpnList.addAll(lstDpnsForAdd) ;
            builder.setDpnIds(ItmUtils.getDpnIdsListFromBigInt(originalDpnList));
        }
        if (lstDpnsForDelete != null && !lstDpnsForDelete.isEmpty()) {
            List<BigInteger> originalDpnList = ItmUtils.getDpnIdList(schema.getDpnIds());
            originalDpnList.removeAll(lstDpnsForDelete) ;
            builder.setDpnIds(ItmUtils.getDpnIdsListFromBigInt(originalDpnList));
            // schema.setDpnIds(ItmUtils.getDpnIdsListFromBigInt(ItmUtils.getDpnIdList(schema.getDpnIds())
            // .removeAll(lstDpnsForAdd)));
        }
        // }
        schema = builder.build();
        MDSALUtil.syncWrite(this.dataBroker, LogicalDatastoreType.CONFIGURATION,
                ItmUtils.getVtepConfigSchemaIdentifier(schemaName), schema);
        LOG.debug("Vtep config schema {} updated to config DS with DPN's {}",
                schemaName, ItmUtils.getDpnIdList(schema.getDpnIds()));
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

    @Override
    public void configureTunnelMonitorParams(boolean monitorEnabled, String monitorProtocol) {
        tepCommandHelper.configureTunnelMonitorParams(monitorEnabled, monitorProtocol);
    }

    @Override
    public void configureTunnelMonitorInterval(int interval) {
        tepCommandHelper.configureTunnelMonitorInterval(interval);
    }

    @Override
    public boolean validateIP(final String ip) {
        if (ip == null || ip.equals("")) {
            return false;
        }
        final String PTRN =
                "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
        Pattern pattern = Pattern.compile(PTRN);
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }
}
