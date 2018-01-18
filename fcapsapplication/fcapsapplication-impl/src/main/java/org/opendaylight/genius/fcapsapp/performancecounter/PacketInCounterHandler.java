/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsapp.performancecounter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.fcapsapp.FcapsUtil;
import org.opendaylight.infrautils.metrics.Counter;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PacketInCounterHandler implements PacketProcessingListener {
    private static final Logger LOG = LoggerFactory.getLogger(PacketInCounterHandler.class);

    private final PMAgent agent;
    private final ConcurrentMap<String, AtomicLong> ingressPacketMap = new ConcurrentHashMap<>();
    private final MetricProvider metricProvider;
    private Map<String,Counter> counterMap = new ConcurrentHashMap<>();
    private String counterName = "packetIn";

    @Inject
    public PacketInCounterHandler(final PMAgent agent, MetricProvider metricProvider) {
        this.agent = agent;
        this.metricProvider = metricProvider;
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
        String counterKey = FcapsUtil.getCounterName(dpnId,counterName);
        counterMap.computeIfAbsent(counterKey,
                (counter -> metricProvider.newCounter(this, counterKey))).increment();
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
            if (counterMap.containsKey(FcapsUtil.getCounterName(dpnId,counterName))) {
                counterMap.get(FcapsUtil.getCounterName(dpnId,counterName)).close();
                counterMap.remove(FcapsUtil.getCounterName(dpnId,counterName));
            }
        } else {
            LOG.error("DpnId is null upon nodeRemovedNotification");
        }
    }
}
