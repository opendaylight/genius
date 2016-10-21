/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.it;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel.DEBUG;
import static org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel.ERROR;
import static org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel.INFO;
import static org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel.WARN;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.genius.interfacemanager.statusanddiag.InterfaceStatusMonitor;
import org.opendaylight.genius.it.statemanager.api.StateManager;
import org.opendaylight.genius.it.statemanager.impl.ConfigStateManager;
import org.opendaylight.genius.itm.snd.ITMStatusMonitor;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.mdsal.utils.NotifyingDataChangeListener;
import org.opendaylight.ovsdb.utils.ovsdb.it.utils.ItConstants;
import org.opendaylight.ovsdb.utils.ovsdb.it.utils.OvsdbItUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdPools;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.IdEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.id.pool.IdEntriesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class GeniusIT extends AbstractMdsalTestBase implements StateManager {
    private static final Logger LOG = LoggerFactory.getLogger(GeniusIT.class);
    private static AtomicBoolean setup = new AtomicBoolean(false);
    private static AtomicBoolean ready = new AtomicBoolean(false);
    private static String userSpaceEnabled;
    private static InterfaceStatusMonitor interfaceStatusMonitor = InterfaceStatusMonitor.getInstance();
    private static ITMStatusMonitor itmStatusMonitor = ITMStatusMonitor.getInstance();
    private static final String OVSDB_TOPOLOGY_ID = "ovsdb:1";
    private static final String IDPOOL_NAME = "genius-it";
    private static final long IDPOOL_START = 1;
    private static final long IDPOOL_SIZE = 65535;
    private static final String IDPOOL_ID_1 = "genius-1";
    private static final String IDPOOL_ID_2 = "genius-2";
    private static MdsalUtils mdsalUtils = null;
    private static OvsdbItUtils itUtils;
    @Inject @Filter(timeout = 60000)
    private static DataBroker dataBroker = null;
    @Inject @Filter(timeout = 60000)
    private static RpcProviderRegistry rpcProviderRegistry;
    private static IdManagerService idManager;

    @Override
    public MavenUrlReference getFeatureRepo() {
        return maven()
                .groupId("org.opendaylight.genius")
                .artifactId("it-features")
                .classifier("features")
                .type("xml")
                .versionAsInProject();
    }

    @Override
    public String getFeatureName() {
        return "odl-genius-it";
    }

    @Override
    public Option getLoggingOption() {
        return composite(
                editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        logConfiguration(GeniusIT.class),
                        INFO.name()),
                editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.genius",
                        DEBUG.name()),
                editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.controller.configpusherfeature.internal.FeatureConfigPusher",
                        ERROR.name()),
                editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        "log4j.logger.org.opendaylight.yangtools.yang.parser.repo.YangTextSchemaContextResolver",
                        WARN.name()),
                super.getLoggingOption());
    }

    @Before
    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void setup() throws Exception {
        if (setup.get()) {
            LOG.info("Skipping setUp, already initialized");
            return;
        }

        try {
            super.setup();
        } catch (Exception e) {
            LOG.warn("Failed to setup test", e);
            fail("Failed to setup test: " + e);
        }

        getProperties();
        assertNotNull("dataBroker should not be null", dataBroker);
        assertNotNull("rpcProviderRegistry should not be null", rpcProviderRegistry);
        itUtils = new OvsdbItUtils(dataBroker);
        mdsalUtils = new MdsalUtils(dataBroker);
        assertNotNull("mdsalUtils should not be null", mdsalUtils);
        getReady();
        assertTrue("InterfaceManager is not ready", isServiceReady("interfaceManager"));
        assertTrue("ITM is not ready", isServiceReady("itm"));
        idManager = rpcProviderRegistry.getRpcService(IdManagerService.class);
        assertNotNull("idManager should not be null", idManager);
        assertTrue("Did not find " + OVSDB_TOPOLOGY_ID, getTopology());
        setup.set(true);
    }

    private void getProperties() {
        Properties props = System.getProperties();
        String addressStr = props.getProperty(ItConstants.SERVER_IPADDRESS);
        String portStr = props.getProperty(ItConstants.SERVER_PORT, ItConstants.DEFAULT_SERVER_PORT);
        String connectionType = props.getProperty(ItConstants.CONNECTION_TYPE, "active");
        String controllerStr = props.getProperty(ItConstants.CONTROLLER_IPADDRESS, "0.0.0.0");
        userSpaceEnabled = props.getProperty(ItConstants.USERSPACE_ENABLED, "no");
        LOG.info("Using the following properties: connection= {}, server ip:port= {}:{}, controller ip: {}, "
                + "userspace.enabled: {}",
                connectionType, addressStr, portStr, controllerStr, userSpaceEnabled);
    }

    private boolean isServiceReady(String service) {
        LOG.info("isServiceReady: waiting for {} status to be Operational...", service);
        Boolean ready = false;
        for (int i = 0; i < 60; i++) {
            String status = getStatus(service);
            if (status != null && status.equals("OPERATIONAL")) {
                LOG.info("isServiceReady: {} status is Operational...", service);
                ready = true;
                break;
            } else {
                LOG.info("isServiceReady: still waiting for {} ({}s), status is {}", service, i, status);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOG.warn("isServiceReady: interrupted while waiting for {} status to be Operational", service);
                }
            }
        }
        return ready;
    }

    private String getStatus(String service) {
        return (service.equals("itm")
                ? itmStatusMonitor.acquireServiceStatus() : interfaceStatusMonitor.acquireServiceStatus());
    }

    private void getReady() {
        new ConfigStateManager(this).start();
        synchronized (ready) {
            while (!ready.get()) {
                try {
                    LOG.info("Waiting for services");
                    ready.wait();
                } catch (InterruptedException e) {
                    LOG.warn("Problem waiting for services", e);
                }
            }
        }
    }

    @Override
    public void setReady(boolean ready2) {
        synchronized (ready) {
            if (ready.compareAndSet(false, true)) {
                ready.notify();
            }
        }
    }

    @Override
    public void setFail() {
        fail();
    }

    private Boolean getTopology() throws Exception {
        LOG.info("getNetvirtTopology: looking for {}...", OVSDB_TOPOLOGY_ID);
        Boolean found = false;
        TopologyId topologyId = new TopologyId(OVSDB_TOPOLOGY_ID);
        InstanceIdentifier<Topology> path =
                InstanceIdentifier.create(NetworkTopology.class).child(Topology.class, new TopologyKey(topologyId));
        final NotifyingDataChangeListener netvirtTopologyListener =
                new NotifyingDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                        NotifyingDataChangeListener.BIT_CREATE, path, null);
        netvirtTopologyListener.registerDataChangeListener(dataBroker);
        netvirtTopologyListener.waitForCreation(60000);
        Topology topology = mdsalUtils.read(LogicalDatastoreType.OPERATIONAL, path);
        if (topology != null) {
            LOG.info("getNetvirtTopology: found {}...", OVSDB_TOPOLOGY_ID);
            found = true;
        }
        netvirtTopologyListener.close();

        return found;
    }

    @Ignore
    @Test
    public void testGeniusFeatureLoad() {
        assertTrue(true);
    }

    @Test
    public void testIdManager() throws Exception {
        IdManagerUtils idManagerUtils = new IdManagerUtils(idManager, IDPOOL_NAME, IDPOOL_START, IDPOOL_SIZE);
        assertTrue("Failed to create Id Pool", idManagerUtils.createIdPool());

        // verify the idPool is written to mdsal
        InstanceIdentifier<IdPool> idPoolIId = InstanceIdentifier.create(IdPools.class)
                .child(IdPool.class, new IdPoolKey(IDPOOL_NAME));
        final NotifyingDataChangeListener idPoolListener =
                new NotifyingDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                        NotifyingDataChangeListener.BIT_ALL, idPoolIId, null);
        idPoolListener.registerDataChangeListener(dataBroker);
        idPoolListener.waitForCreation(1000);
        IdPool idPool = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, idPoolIId);
        assertNotNull("Failed to find Id Pool", idPool);

        assertNotEquals("Failed to allocate Unique Id " + IDPOOL_ID_1,
                idManagerUtils.allocateId(IDPOOL_ID_1), IdManagerUtils.INVALID_ID);
        // verify the entries were written to mdsal
        InstanceIdentifier<IdEntries> idEntries1IId = InstanceIdentifier.create(IdPools.class)
                .child(IdPool.class, new IdPoolKey(IDPOOL_NAME))
                .child(IdEntries.class, new IdEntriesKey(IDPOOL_ID_1));
        final NotifyingDataChangeListener idEntriesListener =
                new NotifyingDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                        NotifyingDataChangeListener.BIT_ALL, idEntries1IId, null);
        idEntriesListener.registerDataChangeListener(dataBroker);
        idEntriesListener.waitForCreation(1000);
        IdEntries idEntries = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, idEntries1IId);
        assertNotNull("Failed to find Id Entry " + IDPOOL_ID_1, idEntries);

        assertNotEquals("Failed to allocate Unique Id " + IDPOOL_ID_2,
                idManagerUtils.allocateId(IDPOOL_ID_2), IdManagerUtils.INVALID_ID);
        InstanceIdentifier<IdEntries> idEntries2IId = InstanceIdentifier.create(IdPools.class)
                .child(IdPool.class, new IdPoolKey(IDPOOL_NAME))
                .child(IdEntries.class, new IdEntriesKey(IDPOOL_ID_2));
        idEntriesListener.modify(LogicalDatastoreType.CONFIGURATION, idEntries2IId);
        idEntriesListener.registerDataChangeListener(dataBroker);
        idEntriesListener.waitForCreation(1000);
        idEntries = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, idEntries2IId);
        assertNotNull("Failed to find Id Entry " + IDPOOL_ID_2, idEntries);

        assertTrue("Failed to release Unique Id " + IDPOOL_ID_2, idManagerUtils.releaseId(IDPOOL_ID_2));
        idEntriesListener.waitForDeletion(1000);
        idEntries = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, idEntries2IId);
        assertNull("Should not find Id Entry " + IDPOOL_ID_2, idEntries);

        assertTrue("Failed to release Unique Id " + IDPOOL_ID_1, idManagerUtils.releaseId(IDPOOL_ID_1));
        idEntriesListener.modify(LogicalDatastoreType.CONFIGURATION, idEntries1IId);
        idEntriesListener.registerDataChangeListener(dataBroker);
        idEntriesListener.waitForDeletion(1000);
        idEntries = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, idEntries1IId);
        assertNull("Should not find Id Entry " + IDPOOL_ID_1, idEntries);

        assertTrue("Failed to delete Id Pool", idManagerUtils.deleteIdPool());
        idPoolListener.waitForDeletion(1000);
        idPool = mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, idPoolIId);
        assertNull("Id Pool should not be found", idPool);

        idPoolListener.close();
        idEntriesListener.close();
    }
}
