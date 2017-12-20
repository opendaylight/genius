/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsapp.performancecounter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PacketInCounterHandler implements PacketProcessingListener {
    private static final Logger LOG = LoggerFactory.getLogger(PacketInCounterHandler.class);
    private static final Long FIRST_VALUE = 0L;

    private final PMAgent agent;
    private final ConcurrentMap<String, AtomicLong> ingressPacketMap = new ConcurrentHashMap<>();

    @Inject
    public PacketInCounterHandler(final PMAgent agent) {
        this.agent = agent;
    }

    @Override
    public void onPacketReceived(PacketReceived notification) {
        LOG.debug("Ingress packet notification received");
        if (notification.getIngress() == null) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("invalid PacketReceived notification");
            }
            return;
        }
        String dpnId = getDpnId(notification.getIngress().getValue().toString());
        ingressPacketMap.computeIfAbsent(dpnId,(counter -> new AtomicLong(FIRST_VALUE))).incrementAndGet();
        connectToPMAgent();
    }

    private void connectToPMAgent() {
        Map<String, String> packetInMap = new HashMap<>();
        ingressPacketMap.forEach((dpnId, count) -> packetInMap
                .put("InjectedOFMessagesSent:" + "dpnId_" + dpnId + "_InjectedOFMessagesSent", String.valueOf(count)));
        agent.sendPacketInCounterUpdate(packetInMap);
    }

    /*
     * Method to extract DpnId
     */
    @Nonnull
    private String getDpnId(@Nonnull String id) {
        String[] nodeNo = id.split(":");
        String[] dpnId = nodeNo[1].split("]");
        return dpnId[0];
    }

    public void nodeRemovedNotification(String dpnId) {
        if (dpnId != null) {
            dpnId = dpnId.split(":")[1];
            LOG.debug("Dpnvalue Id {}", dpnId);
            if (ingressPacketMap.containsKey(dpnId)) {
                ingressPacketMap.remove(dpnId);
                connectToPMAgent();
                LOG.debug("Node {} Removed for PacketIn counter", dpnId);
            }
        } else {
            LOG.error("DpnId is null upon nodeRemovedNotification");
        }
    }
}
