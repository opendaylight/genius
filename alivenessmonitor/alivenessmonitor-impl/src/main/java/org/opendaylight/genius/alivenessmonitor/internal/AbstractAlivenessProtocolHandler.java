/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.alivenessmonitor.internal;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.EtherTypes;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractAlivenessProtocolHandler implements AlivenessProtocolHandler {
    private final DataBroker dataBroker;

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAlivenessProtocolHandler.class);

    AbstractAlivenessProtocolHandler(final DataBroker dataBroker,
            final AlivenessMonitor alivenessMonitor,
            final EtherTypes etherType) {
        this.dataBroker = dataBroker;
        alivenessMonitor.registerHandler(etherType, this);
    }

    private <T extends DataObject> Optional<T> read(
            LogicalDatastoreType datastoreType, InstanceIdentifier<T> path) {
        try {
            ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction();

            return tx.read(datastoreType, path).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Cannot read object {} from datastore: ", path, e);

            throw new RuntimeException(e);
        }
    }

    // @formatter:off
    protected org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces
        .state.Interface getInterfaceFromOperDS(String interfaceName) {
        InstanceIdentifier.InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .interfaces.rev140508.interfaces.state.Interface> idBuilder = InstanceIdentifier
                .builder(InterfacesState.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces
                        .state.Interface.class,
                            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                            .interfaces.state.InterfaceKey(interfaceName));
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces
            .state.Interface> id = idBuilder.build();

        return read(LogicalDatastoreType.OPERATIONAL, id).orNull();
    }
    // @formatter:on

    private InstanceIdentifier<Interface> getInterfaceIdentifier(
            InterfaceKey interfaceKey) {
        InstanceIdentifier.InstanceIdentifierBuilder<Interface> interfaceInstanceIdentifierBuilder = InstanceIdentifier
                .builder(Interfaces.class).child(Interface.class, interfaceKey);

        return interfaceInstanceIdentifierBuilder.build();
    }

    protected byte[] getMacAddress(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                .interfaces.rev140508.interfaces.state.Interface interfaceState, String interfaceName) {
        String macAddress = interfaceState.getPhysAddress().getValue();

        if (!Strings.isNullOrEmpty(macAddress)) {
            return AlivenessMonitorUtil.parseMacAddress(macAddress);
        }
        return null;
    }

}
