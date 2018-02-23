/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.utilities;

import org.immutables.value.Value;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Value.Immutable
@ImmutableStyle
public abstract class NodeConnectorInfo {

    @Value.Parameter
    public abstract InstanceIdentifier<FlowCapableNodeConnector> getNodeConnectorId();

    @Value.Parameter
    public abstract FlowCapableNodeConnector getNodeConnector();
}
