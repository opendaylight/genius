/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.IfmCacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OvsdbNodeListener implements ClusteredDataTreeChangeListener<OvsdbNodeAugmentation> {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbNodeListener.class);

    private DataBroker dataBroker;
    private final IfmCacheProvider ifmCacheProvider;
    private final ListenerRegistration<OvsdbNodeListener> registration;
    private final DataTreeIdentifier<OvsdbNodeAugmentation> treeId =
        new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, getWildcardPath());

    @Inject
    public OvsdbNodeListener(final DataBroker dataBroker, final IfmCacheProvider ifmCacheProvider) {
        this.dataBroker = dataBroker;
        this.ifmCacheProvider = ifmCacheProvider;
        registration = dataBroker.registerDataTreeChangeListener(treeId, OvsdbNodeListener.this);
    }

    @Override
    public void onDataTreeChanged(@Nonnull Collection<DataTreeModification<OvsdbNodeAugmentation>> changes) {
        for (DataTreeModification<OvsdbNodeAugmentation> change : changes) {
            final DataObjectModification<OvsdbNodeAugmentation> mod = change.getRootNode();
            InstanceIdentifier<OvsdbNodeAugmentation> key = change.getRootPath().getRootIdentifier();
            switch (mod.getModificationType()) {
                case DELETE:
                    ifmCacheProvider.removeOvsNode(key.firstKeyOf(Node.class).getNodeId().getValue());
                    break;
                case SUBTREE_MODIFIED:
                    ifmCacheProvider.updateOvsNode(key.firstKeyOf(Node.class).getNodeId().getValue(),
                        mod.getDataBefore(), mod.getDataAfter());
                    break;
                case WRITE:
                    ifmCacheProvider.addOvsNode(key.firstKeyOf(Node.class).getNodeId().getValue(),
                        mod.getDataAfter());
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }

    private InstanceIdentifier<OvsdbNodeAugmentation> getWildcardPath() {
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class)
            .child(Node.class).augmentation(OvsdbNodeAugmentation.class).build();
    }

}