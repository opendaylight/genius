/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsapp.performancecounter;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.infrautils.metrics.Counter;
import org.opendaylight.infrautils.metrics.Labeled;
import org.opendaylight.infrautils.metrics.MetricDescriptor;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PacketInCounterHandler implements PacketProcessingListener {
    private static final Logger LOG = LoggerFactory.getLogger(PacketInCounterHandler.class);

    private final PMAgent agent;
    private final MetricProvider metricProvider;
    private final Labeled<Labeled<Labeled<Counter>>> packetInCounter;

    @Inject
    public PacketInCounterHandler(final PMAgent agent, MetricProvider metricProvider) {
        this.agent = agent;
        this.metricProvider = metricProvider;
        packetInCounter =  metricProvider.newCounter(MetricDescriptor.builder().anchor(this)
                .project("genius").module("fcapsapplication").id("entitycounter")
                .build(), "entitytype", "switchid","name");
    }

    @Override
    public void onPacketReceived(PacketReceived notification) {
        Counter counter;
        LOG.debug("Ingress packet notification received");
        if (notification.getIngress() == null) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("invalid PacketReceived notification");
            }
            return;
        }
        String dpnId = getDpnId(notification.getIngress().getValue().toString());
        counter = packetInCounter.label("OFSwitch").label(dpnId).label("packetin");
        counter.increment();
    }

    /*
     * Method to extract DpnId
     */

    @NonNull
    private String getDpnId(@NonNull String id) {
        String[] nodeNo = id.split(":");
        String[] dpnId = nodeNo[1].split("]");
        return dpnId[0];
    }

    public void nodeRemovedNotification(String dpnId) {
        Counter counter;
        if (dpnId != null) {
            dpnId = dpnId.split(":")[1];
            LOG.debug("Dpnvalue Id {}", dpnId);
            counter = packetInCounter.label("OFSwitch").label(dpnId).label("packetin");
            counter.close();
        } else {
            LOG.error("DpnId is null upon nodeRemovedNotification");
        }
    }
}
