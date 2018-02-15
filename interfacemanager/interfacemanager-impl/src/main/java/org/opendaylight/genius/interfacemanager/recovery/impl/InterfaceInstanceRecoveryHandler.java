/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.recovery.impl;

import java.util.Collections;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.recovery.ServiceRecoveryInterface;
import org.opendaylight.genius.interfacemanager.recovery.registry.ServiceRecoveryRegistry;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.GeniusIfmInterface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfaceInstanceRecoveryHandler implements ServiceRecoveryInterface {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceInstanceRecoveryHandler.class);

    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final JobCoordinator jobCoordinator;
    private final ManagedNewTransactionRunner txRunner;

    @Inject
    public InterfaceInstanceRecoveryHandler(DataBroker dataBroker,
                                            InterfaceManagerCommonUtils interfaceManagerCommonUtils,
                                            JobCoordinator jobCoordinator,
                                            ServiceRecoveryRegistry serviceRecoveryRegistry) {
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.jobCoordinator = jobCoordinator;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        serviceRecoveryRegistry.registerServiceRecoveryRegistry(buildServiceRegistryKey(), this);
    }

    @Override
    public void recoverService(String entityId) {
        LOG.info("recover interface instance {}", entityId);
        // Fetch the interface from interface config DS first.
        Interface interfaceConfig = interfaceManagerCommonUtils.getInterfaceFromConfigDS(entityId);
        if (interfaceConfig != null) {
            // Do a delete and recreate of the interface configuration.
            InstanceIdentifier<Interface> interfaceId = InterfaceManagerCommonUtils.getInterfaceIdentifier(
                    new InterfaceKey(entityId));
            LOG.trace("deleting interface instance {}", entityId);
            jobCoordinator.enqueueJob(entityId, () -> Collections.singletonList(
                txRunner.callWithNewWriteOnlyTransactionAndSubmit(
                    tx -> tx.delete(LogicalDatastoreType.CONFIGURATION, interfaceId))),
                    IfmConstants.JOB_MAX_RETRIES);
            LOG.trace("recreating interface instance {}, {}", entityId, interfaceConfig);
            jobCoordinator.enqueueJob(entityId, () -> Collections.singletonList(
                txRunner.callWithNewWriteOnlyTransactionAndSubmit(
                    tx -> tx.put(LogicalDatastoreType.CONFIGURATION, interfaceId, interfaceConfig))),
                    IfmConstants.JOB_MAX_RETRIES);
        }
    }

    private String buildServiceRegistryKey() {
        return GeniusIfmInterface.class.toString();
    }
}
