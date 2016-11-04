/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.alarmmanager;

import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.TreeSet;
import javax.management.AttributeChangeNotification;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.opendaylight.genius.fcapsmanager.AlarmServiceFacade;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlarmNotificationListeners implements Runnable {

    private static final String DOMAIN = "SDNC.FM";

    private static final Logger LOG = LoggerFactory.getLogger(AlarmNotificationListeners.class);

    private boolean shouldContinue = true;
    private final DelegateListener delegateListener = new DelegateListener();
    private final BundleContext context;
    private MBeanServer mbs;

    public AlarmNotificationListeners(BundleContext context) {
        this.context = context;
    }

    public void setShouldContinue(boolean shouldContinue) {
        this.shouldContinue = shouldContinue;
    }

    /**
     * Platform dependent bundle injects its handle and it is retrieved in the method.
     */
    private AlarmServiceFacade getAlarmServiceSPI() {
        AlarmServiceFacade service = null;
        if (context != null) {
            ServiceReference<?> serviceReference = context.getServiceReference(AlarmServiceFacade.class.getName());
            if (serviceReference != null) {
                service = (AlarmServiceFacade) context.getService(serviceReference);
            }
        }
        return service;
    }

    /**
     * Gets register notification when a mbean is registered in platform
     * Mbeanserver and checks if it is alarm mbean and add attribute
     * notification listener to it. Gets attribute notification when alarm mbean
     * is updated by the application.
     */
    public class DelegateListener implements NotificationListener {

        @Override
        public void handleNotification(Notification notification, Object obj) {
            if (notification instanceof MBeanServerNotification) {
                MBeanServerNotification msnotification = (MBeanServerNotification) notification;
                String notificationType = msnotification.getType();
                ObjectName mbn = msnotification.getMBeanName();

                if (notificationType.equals("JMX.mbean.registered")) {
                    if (mbn.toString().contains(DOMAIN)) {
                        LOG.debug("Received registeration of Mbean " + mbn);
                        try {
                            mbs.addNotificationListener(mbn,delegateListener, null, null);
                            LOG.debug("Added attribute notification listener for Mbean " + mbn);
                        } catch (InstanceNotFoundException e) {
                            LOG.error("Exception while adding attribute notification of mbean {}", e);
                        }
                    }
                }

                if (notificationType.equals("JMX.mbean.unregistered")) {
                    if (mbn.toString().contains(DOMAIN)) {
                        LOG.debug("Time: " + msnotification.getTimeStamp() + "MBean "
                                + msnotification.getMBeanName() + " unregistered successfully");
                    }
                }
            } else if (notification instanceof AttributeChangeNotification) {
                AttributeChangeNotification acn =
                        (AttributeChangeNotification) notification;

                LOG.debug("Received attribute notification of Mbean: "
                        + notification.getSource()
                        + " for attribute:" + acn.getAttributeName() );

                if (acn.getAttributeName().toString().equals("raiseAlarmObject")) {
                    String value = acn.getNewValue().toString();
                    value = value.replace(value.charAt(0), ' ');
                    value = value.replace(value.charAt(value.lastIndexOf("]")), ' ');

                    String[] args = value.split(",");
                    LOG.debug("Receive attribute value :" + args[0].trim() + args[1].trim() + args[2].trim());
                    if (getAlarmServiceSPI() != null) {
                        getAlarmServiceSPI().raiseAlarm(args[0].trim(),args[1].trim(),args[2].trim());
                    } else {
                        LOG.debug("Alarm service not available");
                    }
                } else if (acn.getAttributeName().toString().equals("clearAlarmObject")) {
                    String value = acn.getNewValue().toString();
                    value = value.replace(value.charAt(0), ' ');
                    value = value.replace(value.charAt(value.lastIndexOf("]")), ' ');

                    String[] args = value.split(",");
                    LOG.debug("Receive attribute value :" + args[0].trim() + args[1].trim() + args[2].trim());
                    if (getAlarmServiceSPI() != null) {
                        getAlarmServiceSPI().clearAlarm(args[0].trim(), args[1].trim(), args[2].trim());
                    } else {
                        LOG.debug("Alarm service not available");
                    }
                }
            }
        }
    }

    /**
     * Gets the platform MBeanServer instance and registers to get notification
     * whenever alarm mbean is registered in the mbeanserver.
     */
    @Override
    public void run() {
        mbs = ManagementFactory.getPlatformMBeanServer();

        queryMbeans();

        ObjectName delegate = null;
        try {
            delegate = new ObjectName("JMImplementation:type=MBeanServerDelegate");
        } catch (MalformedObjectNameException e) {
            LOG.error("Failed to create JMX ObjectName", e);
            return;
        }
        NotificationFilterSupport filter = new NotificationFilterSupport();
        filter.enableType("JMX.mbean.registered");
        filter.enableType("JMX.mbean.unregistered");

        try {
            mbs.addNotificationListener(delegate, delegateListener, filter, null);
            LOG.debug("Added registeration listener for Mbean {}", delegate);
        } catch (InstanceNotFoundException e) {
            LOG.error("Failed to add registeration listener {}", e);
            return;
        }

        while (shouldContinue) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                shouldContinue = false;
            }
        }
    }

    /**
     * Pre-provisioning case to handle all alarm mbeans which are registered
     * before installation of framework bundle. Queries the platform Mbeanserver
     * to retrieve registered alarm mbean and add attribute notification
     * listener to it.
     */
    public void queryMbeans() {
        Set<ObjectName> names = new TreeSet<>(mbs.queryNames(null, null));
        LOG.debug("Queried MBeanServer for MBeans:");
        for (ObjectName beanName : names) {
            if (beanName.toString().contains(DOMAIN)) {
                try {
                    mbs.addNotificationListener(beanName,delegateListener, null, null);
                    LOG.debug("Added attribute notification listener for Mbean " + beanName);
                } catch (InstanceNotFoundException e) {
                    LOG.error("Failed to add attribute notification for Mbean {}", e);
                }
            }
        }
    }

}
