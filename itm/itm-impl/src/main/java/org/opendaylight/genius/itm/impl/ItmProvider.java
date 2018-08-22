/*
 * Copyright (c) 2016, 2018 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import com.google.common.base.Optional;
import com.google.common.base.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import javax.annotation.*;
import javax.inject.*;
import org.apache.felix.service.command.*;
import org.opendaylight.controller.md.sal.binding.api.*;
import org.opendaylight.controller.md.sal.common.api.data.*;
import org.opendaylight.controller.sal.binding.api.*;
import org.opendaylight.genius.itm.api.*;
import org.opendaylight.genius.itm.cache.*;
import org.opendaylight.genius.itm.cli.*;
import org.opendaylight.genius.itm.diagstatus.*;
import org.opendaylight.genius.itm.globals.*;
import org.opendaylight.genius.itm.listeners.*;
import org.opendaylight.genius.itm.monitoring.*;
import org.opendaylight.genius.itm.rpc.*;
import org.opendaylight.genius.mdsalutil.*;
import org.opendaylight.infrautils.diagstatus.*;
import org.opendaylight.infrautils.utils.concurrent.*;
import org.opendaylight.mdsal.eos.binding.api.*;
import org.opendaylight.mdsal.eos.common.api.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.*;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.*;
import org.ops4j.pax.cdi.api.*;
import org.slf4j.*;

@Singleton
@OsgiServiceProvider
public class ItmProvider implements AutoCloseable, IITMProvider /*,ItmStateService */ {

    private static final Logger LOG = LoggerFactory.getLogger(ItmProvider.class);

    private final DataBroker dataBroker;
    private final ItmManagerRpcService itmRpcService ;
    private final IdManagerService idManager;
    private final TepCommandHelper tepCommandHelper;
    private final TransportZoneListener tzChangeListener;
    private final TunnelMonitorChangeListener tnlToggleListener;
    private final TunnelMonitorIntervalListener tnlIntervalListener;
    private final VtepConfigSchemaListener vtepConfigSchemaListener;
    private final InterfaceStateListener ifStateListener;
    private final EntityOwnershipService entityOwnershipService;
    private final ItmDiagStatusProvider itmStatusProvider;
    private RpcProviderRegistry rpcProviderRegistry;
    private final ItmTunnelEventListener itmStateListener;
    private final OvsdbNodeListener ovsdbChangeListener;
    static short flag = 0;
    private final TunnelMonitoringConfig tunnelMonitoringConfig;
    private EntityOwnershipCandidateRegistration registryCandidate;
    private final DpnTepStateCache dpnTepStateCache;
    private final TunnelStateCache tunnelStateCache;

    @Inject
    public ItmProvider(DataBroker dataBroker,
                       IdManagerService idManagerService,
                       InterfaceStateListener interfaceStateListener,
                       ItmManagerRpcService itmManagerRpcService,
                       ItmTunnelEventListener itmTunnelEventListener,
                       TepCommandHelper tepCommandHelper,
                       TunnelMonitorChangeListener tunnelMonitorChangeListener,
                       TunnelMonitorIntervalListener tunnelMonitorIntervalListener,
                       TransportZoneListener transportZoneListener,
                       VtepConfigSchemaListener vtepConfigSchemaListener,
                       OvsdbNodeListener ovsdbNodeListener,
                       TunnelMonitoringConfig tunnelMonitoringConfig,
                       EntityOwnershipService entityOwnershipService,
                       DpnTepStateCache dpnTepStateCache,
                       final ItmDiagStatusProvider itmDiagStatusProvider,
                       final TunnelStateCache tunnelStateCache) {
        LOG.info("ItmProvider Before register MBean");
        this.dataBroker = dataBroker;
        this.idManager = idManagerService;
        this.ifStateListener = interfaceStateListener;
        this.itmRpcService = itmManagerRpcService;
        this.itmStateListener = itmTunnelEventListener;
        this.tepCommandHelper = tepCommandHelper;
        this.tnlToggleListener = tunnelMonitorChangeListener;
        this.tnlIntervalListener = tunnelMonitorIntervalListener;
        this.tzChangeListener = transportZoneListener;
        this.vtepConfigSchemaListener = vtepConfigSchemaListener;
        this.ovsdbChangeListener = ovsdbNodeListener;
        this.tunnelMonitoringConfig = tunnelMonitoringConfig;
        this.entityOwnershipService = entityOwnershipService;
        this.dpnTepStateCache = dpnTepStateCache;
        this.itmStatusProvider = itmDiagStatusProvider;
        this.tunnelStateCache = tunnelStateCache;
        ITMBatchingUtils.registerWithBatchManager(this.dataBroker);
    }

    @PostConstruct
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void start() {
        try {
            registerEntityForOwnership();
            itmStatusProvider.reportStatus(ServiceState.OPERATIONAL);
            LOG.info("ItmProvider Started");
        } catch (Exception ex) {
            itmStatusProvider.reportStatus(ex);
            LOG.info("ItmProvider failed to start", ex);
        }
    }

    private void registerEntityForOwnership() {
        try {
            this.registryCandidate = entityOwnershipService
                    .registerCandidate(new Entity(ITMConstants.ITM_CONFIG_ENTITY, ITMConstants.ITM_CONFIG_ENTITY));
        } catch (CandidateAlreadyRegisteredException e) {
            LOG.error("failed to register entity {} for entity-ownership-service", e.getEntity());
        }
    }

    @Override
    @PreDestroy
    public void close() {
        if (tzChangeListener != null) {
            tzChangeListener.close();
        }
        if (tnlIntervalListener != null) {
            tnlIntervalListener.close();
        }
        if (tnlToggleListener != null) {
            tnlToggleListener.close();
        }
        if (ovsdbChangeListener != null) {
            ovsdbChangeListener.close();
        }
        if (registryCandidate != null) {
            registryCandidate.close();
        }
        itmStatusProvider.reportStatus(ServiceState.UNREGISTERED);
        LOG.info("ItmProvider Closed");
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
        JdkFutures.addErrorLogging(itmRpcService.addExternalTunnelEndpoint(addExternalTunnelEndpointInput),
                LOG, "addExternalTunnelEndpoint");
    }

    @Override
    public void remExternalEndpoint(Class<? extends TunnelTypeBase> tunnelType, IpAddress dcgwIP) {
        RemoveExternalTunnelEndpointInput removeExternalTunnelEndpointInput =
                new RemoveExternalTunnelEndpointInputBuilder().setTunnelType(tunnelType)
                        .setDestinationIp(dcgwIP).build();
        JdkFutures.addErrorLogging(itmRpcService.removeExternalTunnelEndpoint(removeExternalTunnelEndpointInput),
                LOG, "removeExternalTunnelEndpoint");
    }

    @Override
    public void createLocalCache(BigInteger dpnId, String portName, Integer vlanId, String ipAddress, String subnetMask,
                                 String gatewayIp, String transportZone, CommandSession session) {
        if (tepCommandHelper != null) {
            try {
                tepCommandHelper.createLocalCache(dpnId, portName, vlanId, ipAddress, subnetMask,
                        gatewayIp, transportZone, session);
            } catch (TepException e) {
                LOG.error("Create Local Cache failed", e);
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
            tepCommandHelper.showTeps(tunnelMonitoringConfig.isTunnelMonitoringEnabled(),
                    tunnelMonitoringConfig.getMonitorInterval(), session);
        } catch (TepException e) {
            LOG.error("show teps failed", e);
        }
    }

    @Override
    public void showState(Collection<StateTunnelList> tunnels) {
        if (tunnels != null) {
            try {
                tepCommandHelper.showState(tunnels, tunnelMonitoringConfig.isTunnelMonitoringEnabled());
            } catch (TepException e) {
                LOG.error("show state failed", e);
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
            LOG.error("Delete Vteps Failed", e);
        }
    }

    @Override
    public void configureTunnelType(String transportZone, String tunnelType) {
        LOG.debug("ItmProvider: configureTunnelType {} for transportZone {}", tunnelType, transportZone);
        try {
            tepCommandHelper.configureTunnelType(transportZone,tunnelType);
        } catch (ExecutionException | InterruptedException e) {
            LOG.error("configureTunnelType {} failed for transportZone {}", tunnelType, transportZone, e);
        }
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
        if (ip == null || ip.isEmpty() || "null".equals(ip) || "0.0.0.0".equals(ip)) {
            return false;
        }
        final String PTRN =
                "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
        Pattern pattern = Pattern.compile(PTRN);
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }

    @Override
    public Interface getInterface(String tunnelName) {
        return dpnTepStateCache.getInterfaceFromCache(tunnelName);
    }

    @Override
    public Optional<StateTunnelList> getTunnelState(String interfaceName) throws ReadFailedException {
        return tunnelStateCache.get(tunnelStateCache.getStateTunnelListIdentifier(interfaceName));
    }
}
