/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.monitoring;

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JMXAlarmAgent {
    private static final Logger LOG = LoggerFactory.getLogger(JMXAlarmAgent.class);
    private static final String BEANNAME = "SDNC.FM:name=DataPathAlarmBean";

    private final MBeanServer mbs;
    private ObjectName alarmName;

    private static DataPathAlarm alarmBean = new DataPathAlarm();

    public JMXAlarmAgent() {
        // Get the platform MBeanServer
        mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            alarmName = new ObjectName(BEANNAME);
        } catch (MalformedObjectNameException e) {
            LOG.error("ObjectName instance creation failed for BEANAME {} : {}", BEANNAME, e);
        }
    }

    public void registerMbean() {
        // Unique identification of MBeans
        try {
            // Uniquely identify the MBeans and register them with the platform
            // MBeanServer
            if (!mbs.isRegistered(alarmName)) {
                mbs.registerMBean(alarmBean, alarmName);
                LOG.debug("Registered Mbean {} successfully", alarmName);
            }
        } catch (Exception e) {
            LOG.error("Registeration failed for Mbean {} :{}", alarmName, e);
        }
    }

    public void unregisterMbean() {
        try {
            if (mbs.isRegistered(alarmName)) {
                mbs.unregisterMBean(alarmName);
                LOG.debug("Unregistered Mbean {} successfully", alarmName);
            }
        } catch (Exception e) {
            LOG.error("UnRegisteration failed for Mbean {} :{}", alarmName, e);
        }
    }

    public void invokeFMraisemethod(String alarmId, String text, String src) {
        try {
            mbs.invoke(alarmName, "raiseAlarm", new Object[] { alarmId, text, src },
                    new String[] { String.class.getName(), String.class.getName(), String.class.getName() });
            LOG.trace("Invoked raiseAlarm function for Mbean {} with source {}", BEANNAME, src);
        } catch (Exception e) {
            LOG.error("Invoking raiseAlarm method failed for Mbean {} :{}", alarmName, e);
        }
    }

    public void invokeFMclearmethod(String alarmId, String text, String src) {
        try {
            mbs.invoke(alarmName, "clearAlarm", new Object[] { alarmId, text, src },
                    new String[] { String.class.getName(), String.class.getName(), String.class.getName() });
            LOG.trace("Invoked clearAlarm function for Mbean {} with source {}", BEANNAME, src);
        } catch (Exception e) {
            LOG.error("Invoking clearAlarm method failed for Mbean {} :{}", alarmName, e);
        }
    }
}
