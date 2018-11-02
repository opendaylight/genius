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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandler;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandlerRegistry;
import org.opendaylight.genius.datastoreutils.SingleTransactionDataBroker;
import org.opendaylight.openflowplugin.libraries.liblldp.Packet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProtocolType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

abstract class AbstractAlivenessProtocolHandler<P extends Packet> implements AlivenessProtocolHandler<P> {

    // private static final Logger LOG = LoggerFactory.getLogger(AbstractAlivenessProtocolHandler.class);

    private final SingleTransactionDataBroker singleTxDataBroker;

    AbstractAlivenessProtocolHandler(
            final DataBroker dataBroker,
            final AlivenessProtocolHandlerRegistry alivenessProtocolHandlerRegistry,
            final MonitorProtocolType protocolType) {
        this.singleTxDataBroker = new SingleTransactionDataBroker(dataBroker);
        alivenessProtocolHandlerRegistry.register(protocolType, this);
    }

    // @formatter:off
    protected org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces
        .state.Interface getInterfaceFromOperDS(String interfaceName) throws ReadFailedException {
        InstanceIdentifier.InstanceIdentifierBuilder<Interface> idBuilder = InstanceIdentifier
                .builder(InterfacesState.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces
                        .state.Interface.class,
                            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220
                            .interfaces.state.InterfaceKey(interfaceName));
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces
            .state.Interface> id = idBuilder.build();

        return singleTxDataBroker.syncRead(LogicalDatastoreType.OPERATIONAL, id);
    }
    // @formatter:on

    protected Optional<byte[]> getMacAddress(Interface interfaceState) {
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
