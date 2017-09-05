/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.snd;

import java.lang.management.ManagementFactory;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ITMStatusMonitor implements ITMStatusMonitorMBean {

    private String serviceStatus;
    private static ITMStatusMonitor itmStatusMonitor = new ITMStatusMonitor();
    private static final String JMX_ITM_OBJ_NAME = "com.ericsson.sdncp.services.status:type=SvcItmService";
    private static final Logger LOG = LoggerFactory.getLogger(ITMStatusMonitor.class);

    private ITMStatusMonitor() {
    }

    public void registerMbean() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objName = new ObjectName(JMX_ITM_OBJ_NAME);
            mbs.registerMBean(itmStatusMonitor, objName);
            LOG.info("itm MXBean registration SUCCESSFUL!!! {}", JMX_ITM_OBJ_NAME);
        } catch (InstanceAlreadyExistsException iaeEx) {
            LOG.error("itm MXBean registration FAILED with InstanceAlreadyExistsException", iaeEx);
        } catch (MBeanRegistrationException mbrEx) {
            LOG.error("itm MXBean registration FAILED with MBeanRegistrationException", mbrEx);
        } catch (NotCompliantMBeanException ncmbEx) {
            LOG.error("itm MXBean registration FAILED with NotCompliantMBeanException", ncmbEx);
        } catch (MalformedObjectNameException monEx) {
            LOG.error("itm MXBean registration FAILED with MalformedObjectNameException", monEx);
        }
    }

    public void unregisterMbean() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objName = new ObjectName(JMX_ITM_OBJ_NAME);
            mbs.unregisterMBean(objName);
            LOG.info("itm MXBean un-registration SUCCESSFUL!!! {}", JMX_ITM_OBJ_NAME);
        } catch (MalformedObjectNameException e) {
            LOG.error("itm MXBean un-registration FAILED with MalformedObjectNameException", e);
        } catch (MBeanRegistrationException e) {
            LOG.warn("itm MXBean un-registration FAILED with MBeanRegistrationException", e);
        } catch (InstanceNotFoundException e) {
            LOG.debug("itm MXBean un-registration FAILED with InstanceNotFoundException", e);
        }
    }

    public static ITMStatusMonitor getInstance() {
        return itmStatusMonitor;
    }

    @Override
    public String acquireServiceStatus() {
        return serviceStatus;
    }

    public void reportStatus(@SuppressWarnings("hiding") String serviceStatus) {
        this.serviceStatus = serviceStatus;
    }
}
