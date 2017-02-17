/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.alivenessmonitor.protocols;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.EtherTypes;

/**
 * Registry of {@link AlivenessProtocolHandler}s.
 *
 * @author Michael Vorburger.ch
 */
@Singleton
@ThreadSafe
public class AlivenessProtocolHandlerRegistry {

    private final Map<EtherTypes, AlivenessProtocolHandler> ethTypeToProtocolHandler = new EnumMap<>(EtherTypes.class);
    private final Map<Class<?>, AlivenessProtocolHandler> packetTypeToProtocolHandler = new HashMap<>();

    public synchronized void register(EtherTypes etherType, AlivenessProtocolHandler protocolHandler) {
        ethTypeToProtocolHandler.put(etherType, protocolHandler);
        packetTypeToProtocolHandler.put(protocolHandler.getPacketClass(), protocolHandler);
    }

    // TODO does this have to be synchronized? It's only reading, never
    // modifying fields.. but the content of the Maps pointed to by the fields
    // could have been modified, so.. ?
    public synchronized @Nullable AlivenessProtocolHandler getOpt(EtherTypes etherType) {
        return ethTypeToProtocolHandler.get(etherType);
    }

    public synchronized @Nullable AlivenessProtocolHandler getOpt(Class<? extends Object> packetClass) {
        return packetTypeToProtocolHandler.get(packetClass);
    }

    public @NonNull AlivenessProtocolHandler get(EtherTypes etherType) {
        AlivenessProtocolHandler handler = getOpt(etherType);
        if (handler == null) {
            throw new IllegalStateException("No handler registered for etherType: " + etherType);
        }
        return handler;
    }

    public @NonNull AlivenessProtocolHandler get(Class<?> packetClass) {
        AlivenessProtocolHandler handler = getOpt(packetClass);
        if (handler == null) {
            throw new IllegalStateException("No handler registered for packetClass: " + packetClass);
        }
        return handler;
    }

}
