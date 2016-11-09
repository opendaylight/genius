/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.countermanager;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PMRegistrationListener implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(PMRegistrationListener.class);
    private static MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

    private boolean shouldContinue = true;
    private static final String DOMAIN = "SDNC.PM";
    private static final String MBEAN_REGISTERED = "JMX.mbean.registered";
    private static final String MBEAN_UNREGISTERED = "JMX.mbean.unregistered";
    protected static Collection<ObjectName> beanNames = new HashSet<>();
    private BundleContext context = null;

    public PMRegistrationListener(BundleContext context) {
        this.context = context;
    }

    public void setShouldContinue(boolean shouldContinue) {
        this.shouldContinue = shouldContinue;
    }

    /**
     * Gets register notification when a mbean is registered in platform
     * Mbeanserver and checks if it is counter mbean and add it to the map.
     */
    public static class DelegateListener implements NotificationListener {

        @Override
        public void handleNotification(Notification notification, Object obj) {
            if (notification instanceof MBeanServerNotification) {
                MBeanServerNotification msnotification = (MBeanServerNotification) notification;
                String notificationType = msnotification.getType();
                ObjectName mbn = msnotification.getMBeanName();

                if (MBEAN_REGISTERED.equals(notificationType)) {
                    String mbean = mbn.toString();
                    if (mbean.contains(DOMAIN)) {
                        beanNames.add(mbn);
                        LOG.debug("Beans are " + beanNames);
                    }
                }
                if (MBEAN_UNREGISTERED.equals(notificationType) && mbn.toString().contains(DOMAIN)) {
                    beanNames.remove(mbn);
                    LOG.error(mbn + " MBean has been unregistered");
                }
            }
        }
    }

    @Override
    public void run() {
        queryMbeans();
        DelegateListener delegateListener = new DelegateListener();
        ObjectName delegate = null;
        try {
            delegate = new ObjectName("JMImplementation:type=MBeanServerDelegate");
        } catch (MalformedObjectNameException e) {
            LOG.error("Malformed object name: {} ", e);
        }
        NotificationFilterSupport filter = new NotificationFilterSupport();

        filter.enableType(MBEAN_REGISTERED);
        filter.enableType(MBEAN_UNREGISTERED);

        LOG.debug("Add PM Registeration Notification Listener");
        try {
            mbs.addNotificationListener(delegate, delegateListener, filter, null);
        } catch (InstanceNotFoundException e) {
            LOG.debug("Instance not found: {}", e);
        }
        Poller poller = new Poller(this.context);
        poller.polling();
        waitforNotification();
    }

    /**
     * Prepovising case to handle all counter mbeans which are registered before
     * the installation of framework bundle Queries the platform Mbeanserver to
     * retrieve registered counter mbean and add it to the map
     */
    public void queryMbeans() {
        Set<ObjectName> names = new TreeSet<>(mbs.queryNames(null, null));
        LOG.debug("\nQueried MBeanServer for MBeans:");
        for (ObjectName name : names) {
            if (name.toString().contains(DOMAIN)) {
                beanNames.add(name);
            }
        }
    }

    private void waitforNotification() {
        while (shouldContinue) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                shouldContinue = false;
            }
        }
    }
}
