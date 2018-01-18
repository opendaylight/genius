/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsapp.performancecounter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.genius.fcapsapp.FcapsConstants;
import org.opendaylight.infrautils.metrics.Counter;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.genius.fcapsapp.FcapsConstants.MODULENAME;

@Singleton
public class PacketInCounterHandler implements PacketProcessingListener {
    private static final Logger LOG = LoggerFactory.getLogger(PacketInCounterHandler.class);
    private static final Long FIRST_VALUE = 0L;

    private final PMAgent agent;
    private final ConcurrentMap<String, AtomicLong> ingressPacketMap = new ConcurrentHashMap<>();
    private final MetricProvider metricProvider;
    private List<String> dpIdList = new ArrayList();
    private Map<String,Counter> counterMap = new HashMap<>();



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
        if(dpIdList.contains(dpnId)) {
            dpIdList.add(dpnId);
            connectToPMAgent(dpnId);
        }
    }

    private void connectToPMAgent(String dpnId) {

        Counter  counter =metricProvider.newCounter(this,getCounterName(dpnId));
        counter.increment();
        counterMap.put(getCounterName(dpnId),counter);
    }

    private String getCounterName(String dpnId){
        String dpnName=MODULENAME + FcapsConstants.ENTITY_TYPE_OFSWITCH + "switchid= " + dpnId + "/packetIn";
        return dpnName;
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
            if(counterMap.containsKey(getCounterName(dpnId))){

                counterMap.get(getCounterName(dpnId)).close();
                counterMap.remove(getCounterName(dpnId));
            }
        } else {
            LOG.error("DpnId is null upon nodeRemovedNotification");
        }
    }
}
