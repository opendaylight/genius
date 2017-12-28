/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.hwvtep;

import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Caches hwvtep Node HA info.
 *
 * @author Thomas Pantelis
 */
public interface HwvtepNodeHACache {
    void addChild(InstanceIdentifier<Node> parentId, InstanceIdentifier<Node> childId);

    boolean isHAEnabledDevice(InstanceIdentifier<?> nodeId);

    boolean isHAParentNode(InstanceIdentifier<Node> nodeId);

    @Nonnull
    Set<InstanceIdentifier<Node>> getChildrenForHANode(InstanceIdentifier<Node> parentId);

    @Nonnull
    Set<InstanceIdentifier<Node>> getHAParentNodes();

    @Nonnull
    Set<InstanceIdentifier<Node>> getHAChildNodes();

    InstanceIdentifier<Node> getParent(InstanceIdentifier<Node> childId);

    void removeParent(InstanceIdentifier<Node> parentId);

    void updateConnectedNodeStatus(InstanceIdentifier<Node> nodeId);

    void updateDisconnectedNodeStatus(InstanceIdentifier<Node> nodeId);

    @Nonnull
    Map<String, Boolean> getNodeConnectionStatuses();
}
