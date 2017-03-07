/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.alivenessmonitor.protocols.internal;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.primitives.UnsignedBytes;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.liblldp.Packet;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandler;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandlerRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.EtherTypes;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractAlivenessProtocolHandler<P extends Packet> implements AlivenessProtocolHandler<P> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAlivenessProtocolHandler.class);

    private final DataBroker dataBroker;

    AbstractAlivenessProtocolHandler(
            final DataBroker dataBroker,
            final AlivenessProtocolHandlerRegistry alivenessProtocolHandlerRegistry,
            final EtherTypes etherType) {
        this.dataBroker = dataBroker;
        alivenessProtocolHandlerRegistry.register(etherType, this);
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

    protected Optional<byte[]> getMacAddress(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
                .interfaces.rev140508.interfaces.state.Interface interfaceState, String interfaceName) {
        String macAddress = interfaceState.getPhysAddress().getValue();

        if (!Strings.isNullOrEmpty(macAddress)) {
            return Optional.of(parseMacAddress(macAddress));
        }
        return Optional.absent();
    }

    private byte[] parseMacAddress(String macAddress) {
        byte cur;

        String[] addressPart = macAddress.split(":");
        int size = addressPart.length;

        byte[] part = new byte[size];
        for (int i = 0; i < size; i++) {
            cur = UnsignedBytes.parseUnsignedByte(addressPart[i], 16);
            part[i] = cur;
        }

        return part;
    }

}
