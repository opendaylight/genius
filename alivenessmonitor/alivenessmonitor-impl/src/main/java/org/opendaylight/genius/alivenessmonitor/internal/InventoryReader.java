/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.alivenessmonitor.internal;

import com.google.common.base.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class InventoryReader {
    private final DataBroker dataService;
    private static final Logger LOG = LoggerFactory.getLogger(InventoryReader.class);

    InventoryReader(DataBroker dataService) {
        this.dataService = dataService;
    }

    public String getMacAddress(InstanceIdentifier<NodeConnector> nodeConnectorId) {
        // TODO: Use mdsal apis to read
        Optional<NodeConnector> optNc = read(LogicalDatastoreType.OPERATIONAL, nodeConnectorId);
        if (optNc.isPresent()) {
            NodeConnector nodeConnector = optNc.get();
            FlowCapableNodeConnector fcnc = nodeConnector.getAugmentation(FlowCapableNodeConnector.class);
            MacAddress macAddress = fcnc.getHardwareAddress();
            return macAddress.getValue();
        }
        return null;
    }

    private <T extends DataObject> Optional<T> read(LogicalDatastoreType datastoreType, InstanceIdentifier<T> path) {
        ReadOnlyTransaction tx = dataService.newReadOnlyTransaction();
        try {
            return tx.read(datastoreType, path).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Cannot read from datastore: ", e);
            throw new RuntimeException(e);
        }
    }
}
