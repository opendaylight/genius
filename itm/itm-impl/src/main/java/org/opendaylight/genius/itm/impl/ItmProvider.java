/*
 * Copyright (c) 2016, 2018 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Service;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.cli.TepCommandHelper;
import org.opendaylight.genius.itm.cli.TepException;
import org.opendaylight.genius.itm.diagstatus.ItmDiagStatusProvider;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.listeners.InterfaceStateListener;
import org.opendaylight.genius.itm.listeners.OvsdbNodeListener;
import org.opendaylight.genius.itm.listeners.TransportZoneListener;
import org.opendaylight.genius.itm.listeners.TunnelMonitorChangeListener;
import org.opendaylight.genius.itm.listeners.TunnelMonitorIntervalListener;
import org.opendaylight.genius.itm.monitoring.ItmTunnelEventListener;
import org.opendaylight.genius.itm.recovery.impl.EosChangeEventHandler;
import org.opendaylight.genius.itm.rpc.ItmManagerRpcService;
import org.opendaylight.infrautils.diagstatus.ServiceState;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.infrautils.utils.concurrent.LoggingFutures;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.eos.binding.api.Entity;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipChange;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipListener;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipListenerRegistration;
import org.opendaylight.mdsal.eos.binding.api.EntityOwnershipService;
import org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.CreateIdPoolOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.of.teps.state.OfTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddExternalTunnelEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.AddExternalTunnelEndpointInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rpcs.rev160406.RemoveExternalTunnelEndpointInputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Service
public class ItmProvider implements AutoCloseable, IITMProvider /*,ItmStateService */ {

    private static final Logger LOG = LoggerFactory.getLogger(ItmProvider.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private final DataBroker dataBroker;
    private final ItmManagerRpcService itmRpcService ;
    private final IdManagerService idManager;
    private final TepCommandHelper tepCommandHelper;
    private final TransportZoneListener tzChangeListener;
    private final TunnelMonitorChangeListener tnlToggleListener;
    private final TunnelMonitorIntervalListener tnlIntervalListener;
    private final InterfaceStateListener ifStateListener;
    private final EntityOwnershipService entityOwnershipService;
    private final ItmDiagStatusProvider itmStatusProvider;
    private RpcProviderService rpcProviderService;
    private final ItmTunnelEventListener itmStateListener;
    private final OvsdbNodeListener ovsdbChangeListener;
    static short flag = 0;
    private final TunnelMonitoringConfig tunnelMonitoringConfig;
    private EntityOwnershipCandidateRegistration registryCandidate;
    private final DpnTepStateCache dpnTepStateCache;
    private final TunnelStateCache tunnelStateCache;
    private final ItmConfig itmConfig;
    private final JobCoordinator jobCoordinator;
    private final ItmProvider.ItmProviderEOSListener itmProviderEOSListener;
    public  Integer batchSize;
    public  Integer batchInterval;
    private EosChangeEventHandler eosChangeEventHandler;

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
                       OvsdbNodeListener ovsdbNodeListener,
                       TunnelMonitoringConfig tunnelMonitoringConfig,
                       EntityOwnershipService entityOwnershipService,
                       DpnTepStateCache dpnTepStateCache,
                       final ItmDiagStatusProvider itmDiagStatusProvider,
                       final TunnelStateCache tunnelStateCache,
                       final ItmConfig itmConfig,
                       final JobCoordinator jobCoordinator,
                       final EosChangeEventHandler eosChangeEventHandler) {
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
        this.ovsdbChangeListener = ovsdbNodeListener;
        this.tunnelMonitoringConfig = tunnelMonitoringConfig;
        this.entityOwnershipService = entityOwnershipService;
        this.dpnTepStateCache = dpnTepStateCache;
        this.itmStatusProvider = itmDiagStatusProvider;
        this.tunnelStateCache = tunnelStateCache;
        this.eosChangeEventHandler = eosChangeEventHandler;

        this.itmConfig = itmConfig;
        this.jobCoordinator = jobCoordinator;
        this.itmProviderEOSListener = new ItmProvider.ItmProviderEOSListener(this, this.entityOwnershipService);
    }

    @PostConstruct
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void start() {
        try {
            createIdPool();
            registerEntityForOwnership();
            initialiseBatchingManager();
            LOG.info("ItmProvider Started");
        } catch (Exception ex) {
            itmStatusProvider.reportStatus(ex);
            LOG.info("ItmProvider failed to start", ex);
            return;
        }
    }

    public void initialiseBatchingManager() {
        batchSize = ITMConstants.BATCH_SIZE;
        LOG.info("entered initialse");
        if (itmConfig.getBatchSize() != null) {
            batchSize = itmConfig.getBatchSize().toJava();
        }
        batchInterval = ITMConstants.PERIODICITY;
        if (itmConfig.getBatchInterval() != null) {
            batchInterval = itmConfig.getBatchInterval().toJava();
        }
        ITMBatchingUtils.registerWithBatchManager(this.dataBroker,this.batchSize,this.batchInterval);
    }

    public void createDefaultTransportZone(ItmConfig itmConfigObj) {
        jobCoordinator.enqueueJob(ITMConstants.DEFAULT_TRANSPORT_ZONE, () -> {
            boolean defTzEnabled = itmConfigObj.isDefTzEnabled();
            if (defTzEnabled) {
                String tunnelType = itmConfigObj.getDefTzTunnelType();
                if (tunnelType == null || tunnelType.isEmpty()) {
                    tunnelType = ITMConstants.TUNNEL_TYPE_VXLAN;
                }
                // validate tunnel-type and based on tunnel-type,
                // create default-TZ or update default-TZ if tunnel-type is changed during ODL restart
                configureTunnelType(ITMConstants.DEFAULT_TRANSPORT_ZONE, tunnelType);
                LOG.info("{} is created with {} tunnel-type.", ITMConstants.DEFAULT_TRANSPORT_ZONE, tunnelType);
            } else {
                LOG.info("Removing {} on start-up, def-tz-enabled is false.", ITMConstants.DEFAULT_TRANSPORT_ZONE);
                // check if default-TZ already exists, then delete it because flag is OFF now.
                ItmUtils.deleteTransportZoneFromConfigDS(ITMConstants.DEFAULT_TRANSPORT_ZONE, dataBroker);
            }
            return Collections.emptyList();
        });
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
        this.itmProviderEOSListener.close();
        LOG.info("ItmProvider Closed");
    }

    private void createIdPool() {
        CreateIdPoolInput createPool = new CreateIdPoolInputBuilder()
                .setPoolName(ITMConstants.ITM_IDPOOL_NAME)
                .setLow(ITMConstants.ITM_IDPOOL_START)
                .setHigh(Uint32.valueOf(ITMConstants.ITM_IDPOOL_SIZE))
                .build();
        try {
            ListenableFuture<RpcResult<CreateIdPoolOutput>> result = idManager.createIdPool(createPool);
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
        LoggingFutures.addErrorLogging(itmRpcService.addExternalTunnelEndpoint(addExternalTunnelEndpointInput),
                LOG, "addExternalTunnelEndpoint");
    }

    @Override
    public void remExternalEndpoint(Class<? extends TunnelTypeBase> tunnelType, IpAddress dcgwIP) {
        RemoveExternalTunnelEndpointInput removeExternalTunnelEndpointInput =
                new RemoveExternalTunnelEndpointInputBuilder().setTunnelType(tunnelType)
                        .setDestinationIp(dcgwIP).build();
        LoggingFutures.addErrorLogging(itmRpcService.removeExternalTunnelEndpoint(removeExternalTunnelEndpointInput),
                LOG, "removeExternalTunnelEndpoint");
    }

    @Override
    public void createLocalCache(Uint64 dpnId, String portName, Integer vlanId, String ipAddress, String subnetMask,
            String gatewayIp, String transportZone) {
        if (tepCommandHelper != null) {
            try {
                tepCommandHelper.createLocalCache(dpnId, ipAddress, transportZone);
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
    public List<String> showTeps() {
        try {
            return tepCommandHelper.showTeps(tunnelMonitoringConfig.isTunnelMonitoringEnabled(),
                    tunnelMonitoringConfig.getMonitorInterval());
        } catch (TepException e) {
            LOG.error("show teps failed", e);
            return Collections.singletonList(e.getMessage());
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
    public void showBridges(Map dpnIdBridgeRefsMap) {
        if (!dpnIdBridgeRefsMap.isEmpty()) {
            tepCommandHelper.showBridges(dpnIdBridgeRefsMap);
        } else {
            LOG.warn("tep:show-bridges => No bridges available");
        }
    }

    @Override
    public void showCache(String cacheName) {
        tepCommandHelper.showCache(cacheName);
    }

    @Override
    public void deleteVtep(Uint64 dpnId, String portName, Integer vlanId, String ipAddress, String subnetMask,
            String gatewayIp, String transportZone) {
        try {
            tepCommandHelper.deleteVtep(dpnId, ipAddress, transportZone);
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

    @Override
    public void showOfPorts(Collection<OfTep> ofTeps) {
        if (ofTeps != null) {
            tepCommandHelper.showOfPorts(ofTeps); }
        else {
            LOG.debug("No ofports available"); }
    }

    public void handleOwnershipChange(EntityOwnershipChange ownershipChange,
                                      EntityOwnershipListenerRegistration listenerRegistration) {
        if (ownershipChange.getState().isOwner()) {
            LOG.info("*This* instance of provider is set as a MASTER instance");
            EVENT_LOGGER.debug("****MASTER**** instance");
            createDefaultTransportZone(itmConfig);
            eosChangeEventHandler.recoverUnknownTunnelsOnEosSwitch();
        } else {
            LOG.info("*This* instance of provider is set as a SLAVE instance");
            EVENT_LOGGER.debug("****SLAVE**** instance");
        }
        itmStatusProvider.reportStatus(ServiceState.OPERATIONAL);
    }

    private static class ItmProviderEOSListener implements EntityOwnershipListener {
        private final ItmProvider itmProviderObj;
        private final EntityOwnershipListenerRegistration listenerRegistration;

        ItmProviderEOSListener(ItmProvider itmProviderObj, EntityOwnershipService entityOwnershipService) {
            this.itmProviderObj = itmProviderObj;
            this.listenerRegistration = entityOwnershipService.registerListener(ITMConstants.ITM_CONFIG_ENTITY, this);
            EVENT_LOGGER.debug("ItmProviderEOSListener: registered");
        }

        public void close() {
            listenerRegistration.close();
        }

        @Override
        public void ownershipChanged(EntityOwnershipChange ownershipChange) {
            EVENT_LOGGER.debug("****ownershipChanged****");
            itmProviderObj.handleOwnershipChange(ownershipChange, listenerRegistration);
        }
    }
}
