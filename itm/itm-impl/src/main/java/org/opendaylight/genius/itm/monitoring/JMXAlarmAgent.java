/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.monitoring;

import java.lang.management.ManagementFactory;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JMXAlarmAgent {
    private static final Logger LOG = LoggerFactory.getLogger(JMXAlarmAgent.class);
    private static final String BEANNAME = "SDNC.FM:name=DataPathAlarmBean";

    private final MBeanServer mbs;
    private ObjectName alarmName;

    private static DataPathAlarm alarmBean = new DataPathAlarm();

    public JMXAlarmAgent() throws JMException {
        // Get the platform MBeanServer
        mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            alarmName = new ObjectName(BEANNAME);
            LOG.debug("Su nombre es: {}", BEANNAME);
        } catch (MalformedObjectNameException e) {
            LOG.error("ObjectName {} instance creation failed.", BEANNAME, e);
            throw e;
        }
    }

    public void registerMbean() throws JMException {
        if (!mbs.isRegistered(alarmName)) {
            try {
                mbs.registerMBean(alarmBean, alarmName);
                LOG.debug("Mbean {} successfully registered.", alarmName);
            } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
                LOG.error("Registeration failed for Mbean {}:", alarmName, e);
                throw e;
            }
        }
    }

    public void unregisterMbean() {
        if (mbs.isRegistered(alarmName)) {
            try {
                mbs.unregisterMBean(alarmName);
            } catch (MBeanRegistrationException | InstanceNotFoundException e) {
                LOG.error("Mbean {} cannot be unregistered: ", alarmName, e);
            }
            LOG.debug("Unregistered Mbean {} successfully.", alarmName);
        }
    }

    public void invokeFMraisemethod(String alarmId, String text, String src) {
        try {
            mbs.invoke(alarmName, "raiseAlarm", new Object[] { alarmId, text, src },
                    new String[] { String.class.getName(), String.class.getName(), String.class.getName() });
            LOG.trace("Invoked raiseAlarm function for Mbean {} with source {}", BEANNAME, src);
        } catch (InstanceNotFoundException | ReflectionException | MBeanException e) {
            LOG.error("Invoking raiseAlarm method failed for Mbean {} : ", alarmName, e);
        }
    }

    public void invokeFMclearmethod(String alarmId, String text, String src) {
        try {
            mbs.invoke(alarmName, "clearAlarm", new Object[] { alarmId, text, src },
                    new String[] { String.class.getName(), String.class.getName(), String.class.getName() });
            LOG.trace("Invoked clearAlarm function for Mbean {} with source {}", BEANNAME, src);
        } catch (InstanceNotFoundException | ReflectionException | MBeanException e) {
            LOG.error("Invoking clearAlarm method failed for Mbean {} : ", alarmName, e);
        }
    }
}
