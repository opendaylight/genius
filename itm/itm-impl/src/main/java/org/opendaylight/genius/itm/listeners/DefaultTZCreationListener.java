/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.serviceutils.srm.RecoverableListener;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractClusteredSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DefaultTZCreationListener extends AbstractClusteredSyncDataTreeChangeListener<TransportZone>
        implements RecoverableListener {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultTZCreationListener.class);

    private final DataBroker dataBroker;
    private OvsdbNodeListener ovsdbNodeListener;
    final JobCoordinator jobCoordinator;
    final ItmConfig itmConfig;
    private boolean isregistered = true;

    @Inject
    public DefaultTZCreationListener(final DataBroker dataBroker,
                                     final JobCoordinator jobCoordinator,
                                     final ItmConfig itmConfig) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(TransportZones.class).child(TransportZone.class));
        this.dataBroker = dataBroker;
        this.jobCoordinator = jobCoordinator;
        this.itmConfig = itmConfig;
    }

    @Override
    public void registerListener() {
        register();
    }

    @Override
    public void deregisterListener() {
        ovsdbNodeListener.close();
        close();
    }

    @Override
    public void add(@Nonnull TransportZone transportZone) {
        LOG.debug("Received Transport Zone Add Event: {}", transportZone);
        if (transportZone.getZoneName().equals(ITMConstants.DEFAULT_TRANSPORT_ZONE)) {
            if (isregistered) {
                this.ovsdbNodeListener = new OvsdbNodeListener(dataBroker, itmConfig, jobCoordinator);
                isregistered = false;
            }

        }

    }
}
