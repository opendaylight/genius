/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.alivenessmonitor.protocols.internal;

import static org.opendaylight.mdsal.binding.util.Datastore.OPERATIONAL;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.primitives.UnsignedBytes;
import java.util.concurrent.ExecutionException;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandler;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandlerRegistry;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunnerImpl;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.openflowplugin.libraries.liblldp.Packet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProtocolType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

abstract class AbstractAlivenessProtocolHandler<P extends Packet> implements AlivenessProtocolHandler<P> {

    // private static final Logger LOG = LoggerFactory.getLogger(AbstractAlivenessProtocolHandler.class);

    private final ManagedNewTransactionRunner txRunner;

    AbstractAlivenessProtocolHandler(
            final DataBroker dataBroker,
            final AlivenessProtocolHandlerRegistry alivenessProtocolHandlerRegistry,
            final MonitorProtocolType protocolType) {
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        alivenessProtocolHandlerRegistry.register(protocolType, this);
    }

    // @formatter:off
    protected org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces
        .state.Interface getInterfaceFromOperDS(String interfaceName) throws ReadFailedException {
        InstanceIdentifier.InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .interfaces.rev140508.interfaces.state.Interface> idBuilder = InstanceIdentifier
                .builder(InterfacesState.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces
                        .state.Interface.class,
                            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                            .interfaces.state.InterfaceKey(interfaceName));
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces
            .state.Interface> id = idBuilder.build();

        try {
            return txRunner.applyWithNewReadWriteTransactionAndSubmit(OPERATIONAL,
                input -> input.read(id).get().orElse(null)).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new ReadFailedException("Error", e);
        }
/*
        return txRunner.applyWithNewReadWriteTransactionAndSubmit(OPERATIONAL,
            input -> input.read(id).get().orElse(null)).get();

        return txRunner.applyInterruptiblyWithNewReadOnlyTransactionAndClose(OPERATIONAL,
            (InterruptibleCheckedFunction<TypedReadTransaction<Operational>, java.util.Optional<Interface>,
                ExecutionException>) tx -> tx.read(id).get());
*/
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
