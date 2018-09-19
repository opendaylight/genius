/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.alivenessmonitor.protocols;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.openflowplugin.libraries.liblldp.Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProtocolType;

/**
 * Registry of {@link AlivenessProtocolHandler}s.
 *
 * @author Michael Vorburger.ch
 */
@ThreadSafe
public interface AlivenessProtocolHandlerRegistry {

    void register(MonitorProtocolType protocolType, AlivenessProtocolHandler<?> protocolHandler);

    @Nullable AlivenessProtocolHandler<?> getOpt(MonitorProtocolType protocolType);

    @Nullable <T extends Packet> AlivenessProtocolHandler<T> getOpt(Class<T> packetClass);

    @Nonnull AlivenessProtocolHandler<?> get(MonitorProtocolType protocolType);
}
