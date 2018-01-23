/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.alivenessmonitor.protocols.internal;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandler;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandlerRegistry;
import org.opendaylight.openflowplugin.libraries.liblldp.Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.EtherTypes;

/**
 * Implementation of AlivenessProtocolHandlerRegistry.
 *
 * @author Michael Vorburger.ch
 */
@Singleton
@ThreadSafe
public class AlivenessProtocolHandlerRegistryImpl implements AlivenessProtocolHandlerRegistry {

    private final Map<EtherTypes, AlivenessProtocolHandler<?>> ethTypeToProtocolHandler =
            new EnumMap<>(EtherTypes.class);
    private final Map<Class<?>, AlivenessProtocolHandler<?>> packetTypeToProtocolHandler = new HashMap<>();

    @Override
    public synchronized void register(EtherTypes etherType, AlivenessProtocolHandler<?> protocolHandler) {
        ethTypeToProtocolHandler.put(etherType, protocolHandler);
        packetTypeToProtocolHandler.put(protocolHandler.getPacketClass(), protocolHandler);
    }

    @Override
    public synchronized AlivenessProtocolHandler<?> getOpt(EtherTypes etherType) {
        return ethTypeToProtocolHandler.get(etherType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <T extends Packet> AlivenessProtocolHandler<T> getOpt(Class<T> packetClass) {
        return (AlivenessProtocolHandler<T>) packetTypeToProtocolHandler.get(packetClass);
    }

    @Override
    public AlivenessProtocolHandler<?> get(EtherTypes etherType) {
        AlivenessProtocolHandler<?> handler = getOpt(etherType);
        if (handler == null) {
            throw new IllegalStateException("No handler registered for etherType: " + etherType);
        }
        return handler;
    }

    public AlivenessProtocolHandler<?> get(Class<?> packetClass) {
        AlivenessProtocolHandler<?> handler = packetTypeToProtocolHandler.get(packetClass);
        if (handler == null) {
            throw new IllegalStateException("No handler registered for packetClass: " + packetClass);
        }
        return handler;
    }

}
