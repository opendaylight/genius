/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsapp.performancecounter;

import java.lang.management.ManagementFactory;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.opendaylight.genius.fcapsappjmx.NumberOfOFPorts;
import org.opendaylight.genius.fcapsappjmx.NumberOfOFSwitchCounter;
import org.opendaylight.genius.fcapsappjmx.PacketInCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PMAgent {
    private MBeanServer mbs = null;
    private ObjectName switchMBeanName = null;
    private ObjectName portMBeanName = null;
    private ObjectName pktInMBeanName = null;
    private static final String SWITCH_BEANNAME = "SDNC.PM:type=NumberOfOFSwitchCounter";
    private static final String PORTS_BEANNAME = "SDNC.PM:type=NumberOfOFPortsCounter";
    private static final String PKTIN_BEANNAME = "SDNC.PM:type=InjectedPacketInCounter";

    private static NumberOfOFSwitchCounter switchCounterBean = new NumberOfOFSwitchCounter();
    private static NumberOfOFPorts portcounterBean = new NumberOfOFPorts();
    private static PacketInCounter packetInCounter = new PacketInCounter();

    private static final Logger LOG = LoggerFactory.getLogger(PMAgent.class);

    public PMAgent() {
        mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            switchMBeanName = new ObjectName(SWITCH_BEANNAME);
            portMBeanName = new ObjectName(PORTS_BEANNAME);
            pktInMBeanName = new ObjectName(PKTIN_BEANNAME);
        } catch (MalformedObjectNameException e) {
            LOG.error("ObjectName instance creation failed for BEANAME", e);
        }
    }

    @PostConstruct
    public void start() throws Exception {
        registerMbeanForEFS();
        registerMbeanForPorts();
        registerMbeanForPacketIn();
    }

    public void registerMbeanForEFS() {
        try {
            if (!mbs.isRegistered(switchMBeanName)) {
                mbs.registerMBean(switchCounterBean, switchMBeanName);
                LOG.info("Registered Mbean {} successfully", switchMBeanName);
            }

        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            LOG.error("Registeration failed for Mbean {}", switchMBeanName, e);
        }
    }

    public void registerMbeanForPorts() {
        try {
            if (!mbs.isRegistered(portMBeanName)) {
                mbs.registerMBean(portcounterBean, portMBeanName);
                LOG.info("Registered Mbean {} successfully", portMBeanName);
            }
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            LOG.error("Registeration failed for Mbean {}", portMBeanName, e);
        }
    }

    public void registerMbeanForPacketIn() {
        try {
            if (!mbs.isRegistered(pktInMBeanName)) {
                mbs.registerMBean(packetInCounter, pktInMBeanName);
                LOG.info("Registered Mbean {} successfully", pktInMBeanName);
            }
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            LOG.error("Registeration failed for Mbean {}", pktInMBeanName, e);
        }
    }

    public void connectToPMAgent(Map map) {
        switchCounterBean.updateCounter(map);
    }

    public void connectToPMAgentForNOOfPorts(Map map) {
        portcounterBean.updateCounter(map);
    }

    public void sendPacketInCounterUpdate(Map map) {
        packetInCounter.updateCounter(map);
    }
}
