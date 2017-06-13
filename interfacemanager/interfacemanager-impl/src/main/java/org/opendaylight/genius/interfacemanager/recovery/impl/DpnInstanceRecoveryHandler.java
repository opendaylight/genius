/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.recovery.impl;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.interfacemanager.recovery.ServiceRecoveryInterface;
import org.opendaylight.genius.interfacemanager.recovery.registry.ServiceRecoveryRegistry;
import org.opendaylight.genius.interfacemanager.recovery.utils.ServiceRecoveryConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DpnInstanceRecoveryHandler implements ServiceRecoveryInterface {
    private static final Logger LOG = LoggerFactory.getLogger(DpnInstanceRecoveryHandler.class);
    DataBroker dataBroker;

    @Inject
    public DpnInstanceRecoveryHandler(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @PostConstruct
    public void init() {
        LOG.info("{} start", getClass().getSimpleName());
        ServiceRecoveryRegistry.registerServiceRecoveryRegistry(buildServiceRegistryKey(), this);
    }

    @PreDestroy
    public void close() {
        LOG.trace("{} close", getClass().getSimpleName());
    }

    @Override
    public boolean recoverService(String entityName, String entityType, String entityId) {
        LOG.info("Recovering dpn - WIP");
        return true;
    }

    private String buildServiceRegistryKey() {
        return new StringBuilder(ServiceRecoveryConstants.SERVICE_INSTANCE_PREFIX).append(
            ServiceRecoveryConstants.SERVICE_ENTITY_NAME_SEPARATOR).append(ServiceRecoveryConstants.SERVICE_NAME)
            .append(ServiceRecoveryConstants.SERVICE_INSTANCE_DPN).toString();
    }
}
