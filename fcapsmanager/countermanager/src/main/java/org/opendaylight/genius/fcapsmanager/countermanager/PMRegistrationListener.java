/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.countermanager;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final String DOMAIN = "SDNC.PM";
    private static final String MBEAN_REGISTERED = "JMX.mbean.registered";
    private static final String MBEAN_UNREGISTERED = "JMX.mbean.unregistered";

    private static final Logger LOG = LoggerFactory.getLogger(PMRegistrationListener.class);

    private final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

    private boolean shouldContinue = true;
    private final Set<ObjectName> beanNames = ConcurrentHashMap.newKeySet();
    private final BundleContext bundleContext;

    public PMRegistrationListener(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setShouldContinue(boolean shouldContinue) {
        this.shouldContinue = shouldContinue;
    }

    public Collection<ObjectName> getBeanNames() {
        return beanNames;
    }

    /**
     * Gets register notification when a mbean is registered in platform
     * Mbeanserver and checks if it is counter mbean and add it to the map.
     */
    private class DelegateListener implements NotificationListener {

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
                        LOG.debug("Beans are {}", beanNames);
                    }
                }
                if (MBEAN_UNREGISTERED.equals(notificationType) && mbn.toString().contains(DOMAIN)) {
                    beanNames.remove(mbn);
                    LOG.error("{} MBean has been unregistered", mbn);
                }
            }
        }
    }

    @Override
    public void run() {
        queryMbeans();
        DelegateListener delegateListener = new DelegateListener();
        ObjectName delegate;
        try {
            delegate = new ObjectName("JMImplementation:type=MBeanServerDelegate");
        } catch (MalformedObjectNameException e) {
            LOG.error("Malformed object", e);
            return;
        }
        NotificationFilterSupport filter = new NotificationFilterSupport();

        filter.enableType(MBEAN_REGISTERED);
        filter.enableType(MBEAN_UNREGISTERED);

        LOG.debug("Add PM Registeration Notification Listener");
        try {
            mbs.addNotificationListener(delegate, delegateListener, filter, null);
        } catch (InstanceNotFoundException e) {
            LOG.error("Instance not found", e);
            return;
        }
        Poller poller = new Poller(bundleContext, this);
        poller.polling();
        waitforNotification();
    }

    /**
     * Pre-provisioning case to handle all counter mbeans which are registered before
     * the installation of framework bundle Queries the platform Mbeanserver to
     * retrieve registered counter mbean and add it to the map.
     */
    private void queryMbeans() {
        Set<ObjectName> names = new TreeSet<>(mbs.queryNames(null, null));
        LOG.debug("Queried MBeanServer for MBeans:");
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
