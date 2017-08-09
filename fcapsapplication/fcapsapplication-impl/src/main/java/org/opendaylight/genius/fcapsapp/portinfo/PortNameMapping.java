/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsapp.portinfo;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortNameMapping implements PortNameMappingMBean {
    private static Map<String, String> portNameToPortIdMap = new HashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(PortNameMapping.class);

    private static String beanName = "Ports:type=PortNameMapping";

    @Override
    public Map<String, String> getPortIdtoPortNameMap() {
        return portNameToPortIdMap;
    }

    @Override
    public String getPortName(String portId) {
        return portNameToPortIdMap.get(portId);
    }

    public static void updatePortMap(String portName, String portId, String status) {
        if (status.equals("ADD")) {
            portNameToPortIdMap.put(portId, portName);
            LOG.debug("PortId {} : portName {} added", portId, portName);
        } else if (status.equals("DELETE")) {
            portNameToPortIdMap.remove(portId);
            LOG.debug("PortId {} : portName {} removed", portId, portName);
        }
    }

    public static void registerPortMappingBean() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName mbeanName = null;

        try {
            mbeanName = new ObjectName(beanName);
        } catch (MalformedObjectNameException e) {
            LOG.error("ObjectName instance creation failed for BEANAME {}", beanName, e);

        }
        try {
            if (!mbs.isRegistered(mbeanName)) {
                mbs.registerMBean(new PortNameMapping(), mbeanName);
                LOG.debug("Registered Mbean {} successfully", mbeanName);
            }

        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            LOG.error("Registeration failed for Mbean {}", mbeanName, e);
        }
    }
}
