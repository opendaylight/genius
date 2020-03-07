/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.alivenessmonitor.protocols.impl;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Service;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandler;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandlerRegistry;
import org.opendaylight.openflowplugin.libraries.liblldp.Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProtocolType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of AlivenessProtocolHandlerRegistry.
 * Implementations of this interface are expected to be thread-safe.
 *
 * @author Michael Vorburger.ch
 */
@Singleton
@Service(classes = AlivenessProtocolHandlerRegistry.class)
public class AlivenessProtocolHandlerRegistryImpl implements AlivenessProtocolHandlerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(AlivenessProtocolHandlerRegistryImpl.class);

    private final Map<MonitorProtocolType, AlivenessProtocolHandler<?>> protocolTypeToProtocolHandler =
            new EnumMap<>(MonitorProtocolType.class);
    private final Map<Class<?>, AlivenessProtocolHandler<?>> packetTypeToProtocolHandler = new HashMap<>();

    @Override
    public synchronized void register(MonitorProtocolType protocolType, AlivenessProtocolHandler<?> protocolHandler) {
        protocolTypeToProtocolHandler.put(protocolType, protocolHandler);
        packetTypeToProtocolHandler.put(protocolHandler.getPacketClass(), protocolHandler);
        LOG.trace("Registered AlivenessProtocolHandler protocolType={}, protocolHandler={}", protocolType,
                protocolHandler);
    }

    @Override
    public synchronized AlivenessProtocolHandler<?> getOpt(MonitorProtocolType protocolType) {
        return protocolTypeToProtocolHandler.get(protocolType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <T extends Packet> AlivenessProtocolHandler<T> getOpt(Class<T> packetClass) {
        return (AlivenessProtocolHandler<T>) packetTypeToProtocolHandler.get(packetClass);
    }

    @Override
    public AlivenessProtocolHandler<?> get(MonitorProtocolType protocolType) {
        AlivenessProtocolHandler<?> handler = getOpt(protocolType);
        if (handler == null) {
            throw new IllegalStateException("No handler registered for protocolType: " + protocolType);
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
