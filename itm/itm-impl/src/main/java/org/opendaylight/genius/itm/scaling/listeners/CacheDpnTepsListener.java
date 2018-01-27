/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.listeners;

import java.util.Collection;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.ItmScaleUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CacheDpnTepsListener implements ClusteredDataTreeChangeListener<DpnsTeps> {
    private static final Logger LOG = LoggerFactory.getLogger(CacheDpnTepsListener.class);

    private ListenerRegistration<CacheDpnTepsListener> registration;
    private final DataBroker dataBroker;
    private final IdManagerService idManager;
    private final IMdsalApiManager mdsalApiManager;
    private final JobCoordinator jobCoordinator;
    private final DataTreeIdentifier<DpnsTeps> treeId =
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, getWildcardPath());

    @Inject
    @SuppressWarnings("checkstyle:IllegalCatch")
    public CacheDpnTepsListener(final DataBroker dataBroker, final IdManagerService idManager,
                                final IMdsalApiManager mdsalApiManager, final IInterfaceManager interfaceManager,
                                final JobCoordinator jobCoordinator) {
        this.dataBroker = dataBroker;
        this.idManager = idManager;
        this.mdsalApiManager = mdsalApiManager;
        this.jobCoordinator = jobCoordinator;
        try {
            LOG.trace("Registering on path: {}", treeId);
            if (interfaceManager.isItmDirectTunnelsEnabled()) {
                LOG.debug("ITM Direct Tunnels is Enabled, hence registering this listener");
                registration = dataBroker.registerDataTreeChangeListener(treeId, CacheDpnTepsListener.this);
            } else {
                LOG.debug("ITM Direct Tunnels is not Enabled, therefore not registering this listener");
            }
        } catch (final Exception e) {
            LOG.warn("CacheDpnTepsListener registration failed", e);
        }
    }

    @PreDestroy
    public void close() throws Exception {
        if (registration != null) {
            registration.close();
        }
    }

    protected InstanceIdentifier<DpnsTeps> getWildcardPath() {
        return InstanceIdentifier.create(DpnTepsState.class).child(DpnsTeps.class);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<DpnsTeps>> changes) {
        for (DataTreeModification<DpnsTeps> change : changes) {
            final DataObjectModification<DpnsTeps> mod = change.getRootNode();
            switch (mod.getModificationType()) {
                case DELETE:
                    ItmScaleUtils.removeFromDpnsTepsCache(mod.getDataBefore());
                    ItmScaleUtils.removeFromDpnTepInterfaceCache(mod.getDataBefore());
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    ItmScaleUtils.addDpnsTepsToCache(mod.getDataAfter());
                    ItmScaleUtils.addDpnTepInterfaceToCache(mod.getDataAfter(), dataBroker, idManager,
                            mdsalApiManager, jobCoordinator);
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }
}
