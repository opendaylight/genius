package org.opendaylight.genius.itm.recovery.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import javax.inject.Singleton;
import javax.inject.Inject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.recovery.ItmServiceRecoveryInterface;
import org.opendaylight.genius.itm.recovery.registry.ItmServiceRecoveryRegistry;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.GeniusItmTz;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmTzInstanceRecoveryHandler implements ItmServiceRecoveryInterface {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTzInstanceRecoveryHandler.class);
    private final JobCoordinator jobCoordinator;
    private final DataBroker dataBroker;

    @Inject
    public ItmTzInstanceRecoveryHandler(DataBroker dataBroker,
                                        JobCoordinator jobCoordinator,
                                        ItmServiceRecoveryRegistry itmServiceRecoveryRegistry){
        this.dataBroker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        itmServiceRecoveryRegistry.registerServiceRecoveryRegistry(buildServiceRegistryKey(), this);
    }

    private String buildServiceRegistryKey() {
        return GeniusItmTz.class.toString();
    }

    @Override
    public void recoverService(String entityId) {
        LOG.info("Trigerred recovery of ITM Instance - TZ Name {}", entityId);
        recoverTransportZone(entityId);
    }

    public void recoverTransportZone(String entityId) {
        // Get the transport Zone from the transport Zone Name
        InstanceIdentifier<TransportZone> tzII = ItmUtils.getTransportZoneIdentifierFromName(entityId);
        TransportZone tz = ItmUtils.getTransportZoneFromConfigDS(tzII, dataBroker);
        if (tz != null) {
            // Delete the transportZone and re create it
            ItmTransportZoneRemoveWorker removeWorker = new ItmTransportZoneRemoveWorker(tzII, tz);
            jobCoordinator.enqueueJob(entityId, removeWorker);
            ItmTransportZoneAddWorker addWorker = new ItmTransportZoneAddWorker(tzII, tz);
            jobCoordinator.enqueueJob(entityId, addWorker);
        }
    }

    private class ItmTransportZoneAddWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<TransportZone> tzII;
        TransportZone tz;

        public ItmTransportZoneAddWorker(InstanceIdentifier<TransportZone> tzII, TransportZone tz) {
            this.tzII = tzII;
            this.tz = tz;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            WriteTransaction deleteTransaction = dataBroker.newWriteOnlyTransaction();
            deleteTransaction.delete(LogicalDatastoreType.CONFIGURATION, tzII);
            futures.add(deleteTransaction.submit());
            return futures;
        }
    }

        private class ItmTransportZoneRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
            InstanceIdentifier<TransportZone> tzII;
            TransportZone tz;

            public ItmTransportZoneRemoveWorker(InstanceIdentifier<TransportZone> tzII, TransportZone tz) {
                this.tzII = tzII;
                this.tz = tz;
            }

            @Override
            public String toString() {
                return "ItmTransportZoneRemoveWorker{" + "transportZoneId=" + tzII + ", transportZone=" + tz + '}';
            }

            @Override
            public List<ListenableFuture<Void>> call() throws Exception {
                List<ListenableFuture<Void>> futures = new ArrayList<>();
                WriteTransaction deleteTransaction = dataBroker.newWriteOnlyTransaction();
                deleteTransaction.delete(LogicalDatastoreType.CONFIGURATION, tzII);
                futures.add(deleteTransaction.submit());
                return futures;
            }
        }
}