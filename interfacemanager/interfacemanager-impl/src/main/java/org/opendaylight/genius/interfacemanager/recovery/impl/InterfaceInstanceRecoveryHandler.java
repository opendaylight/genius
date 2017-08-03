/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.recovery.impl;


import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.inject.Inject;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.recovery.ServiceRecoveryInterface;
import org.opendaylight.genius.interfacemanager.recovery.registry.ServiceRecoveryRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.GeniusIfmInterface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterfaceInstanceRecoveryHandler implements ServiceRecoveryInterface {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceInstanceRecoveryHandler.class);
    private InterfacemgrProvider interfacemgrProvider;

    @Inject
    public InterfaceInstanceRecoveryHandler(InterfacemgrProvider interfacemgrProvider) {
        this.interfacemgrProvider = interfacemgrProvider;
        init();
    }

    public void init() {
        LOG.info("{} start", getClass().getSimpleName());
        ServiceRecoveryRegistry.registerServiceRecoveryRegistry(buildServiceRegistryKey(), this);
    }

    @Override
    public void recoverService(String entityId) {
        LOG.info("recover interface instance {}", entityId);
        // Fetch the interface from interface config DS first.
        Interface interfaceConfig = InterfaceManagerCommonUtils.getInterfaceFromConfigDS(entityId,
            interfacemgrProvider.getDataBroker());
        if(interfaceConfig != null) {
            // Do a delete and recreate of the interface configuration.
            InstanceIdentifier<Interface> interfaceId = InterfaceManagerCommonUtils.getInterfaceIdentifier(
                    new InterfaceKey(entityId));
            DataStoreJobCoordinator dataStoreJobCoordinator = DataStoreJobCoordinator.getInstance();
            RecoveryConfigRemoveWorker removeWorker = new RecoveryConfigRemoveWorker(interfaceId, interfaceConfig);
            dataStoreJobCoordinator.enqueueJob(entityId, removeWorker, IfmConstants.JOB_MAX_RETRIES);
            RecoveryConfigAddWroker addWorker = new RecoveryConfigAddWroker(interfaceId, interfaceConfig);
            dataStoreJobCoordinator.enqueueJob(entityId, addWorker, IfmConstants.JOB_MAX_RETRIES);
        }
    }

    private String buildServiceRegistryKey() {
        return GeniusIfmInterface.class.toString();
    }

    private class RecoveryConfigAddWroker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Interface> interfaceId;
        Interface interfaceConfig;

        RecoveryConfigAddWroker(InstanceIdentifier<Interface> interfaceId, Interface interfaceConfig) {
            this.interfaceId = interfaceId;
            this.interfaceConfig = interfaceConfig;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            WriteTransaction createTransaction = interfacemgrProvider.getDataBroker().newWriteOnlyTransaction();
            createTransaction.put(LogicalDatastoreType.CONFIGURATION, interfaceId, interfaceConfig);
            futures.add(createTransaction.submit());
            return futures;
        }

        @Override
        public String toString() {
            return "RecoveryConfigAddWroker{" + "interfaceId=" + interfaceId + ", interfaceConfig="
                    + interfaceConfig + '}';
        }
    }

    private class RecoveryConfigRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Interface> interfaceId;
        Interface interfaceConfig;

        RecoveryConfigRemoveWorker(InstanceIdentifier<Interface> interfaceId, Interface interfaceConfig) {
            this.interfaceId = interfaceId;
            this.interfaceConfig = interfaceConfig;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            WriteTransaction deleteTransaction = interfacemgrProvider.getDataBroker().newWriteOnlyTransaction();
            deleteTransaction.delete(LogicalDatastoreType.CONFIGURATION, interfaceId);
            futures.add(deleteTransaction.submit());
            return futures;
        }

        @Override
        public String toString() {
            return "RecoveryConfigRemoveWorker{" + "interfaceId=" + interfaceId + ", interfaceConfig="
                    + interfaceConfig + '}';
        }
    }
}
