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
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AlarmNotificationListeners {
    private static final String DOMAIN = "SDNC.FM";
    private AlarmServiceFacade alarmService;
    private final DelegateListener delegateListener = new DelegateListener();
    private MBeanServer mbs;
    private static final Logger LOG = LoggerFactory.getLogger(AlarmNotificationListeners.class);

    @Inject
    public AlarmNotificationListeners(AlarmServiceFacade alarmServiceFacade) {
        alarmService = alarmServiceFacade;
    }

    @PostConstruct
    public void start() {
        mbs = ManagementFactory.getPlatformMBeanServer();
        LOG.info("Register with MBean Server and Mbeans for notifications");
        addListenerToMBeans();
        addListenerToMBeanServer();
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

                if ("JMX.mbean.registered".equals(notificationType) && mbn.toString().contains(DOMAIN)) {
                    LOG.debug("Received registeration of Mbean {}", mbn);
                    try {
                        mbs.addNotificationListener(mbn, delegateListener, null, null);
                        LOG.debug("Added attribute notification listener for Mbean {}", mbn);
                    } catch (InstanceNotFoundException e) {
                        LOG.error("Exception while adding attribute notification of mbean ", e);
                    }
                }

                if ("JMX.mbean.unregistered".equals(notificationType) && mbn.toString().contains(DOMAIN)) {
                    LOG.debug("Time: {} MBean {} unregistered successfully", msnotification.getTimeStamp(),
                            msnotification.getMBeanName());
                }
            } else if (notification instanceof AttributeChangeNotification) {
                AttributeChangeNotification acn = (AttributeChangeNotification) notification;

                LOG.debug("Received attribute notification of Mbean: {} for attribute: {}", notification.getSource(),
                        acn.getAttributeName());
                if (alarmService != null) {
                    String value = acn.getNewValue().toString();
                    value = value.replace(value.charAt(0), ' ');
                    value = value.replace(value.charAt(value.lastIndexOf(']')), ' ');

                    String[] args = value.split(",");
                    String alarmName = args[0].trim();
                    String additionalText = args[1].trim();
                    String source = args[2].trim();

                    if ("raiseAlarmObject".equals(acn.getAttributeName())) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Received attributes to raise the alarm,"
                                    + "Alarm Name {} Additional Text {} Source {}",
                                    alarmName, additionalText, source);
                        }
                        alarmService.raiseAlarm(alarmName, additionalText, source);
                    } else if ("clearAlarmObject".equals(acn.getAttributeName())) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Received attributes  to clear the alarm,"
                                    + "Alarm Name {} Additional Text {} Source {}",
                                    alarmName, additionalText, source);
                        }
                        alarmService.clearAlarm(alarmName, additionalText, source);
                    }
                } else {
                    LOG.debug("Alarm service not available");
                }
            }
        }
    }

    /*
    * Register the listener with Mbean Server to receive the notifications to
    * know when new MBeans are registered or existing ones are removed.
    */
    private void addListenerToMBeanServer() {

        ObjectName delegate = null;
        try {
            delegate = new ObjectName("JMImplementation:type=MBeanServerDelegate");
        } catch (MalformedObjectNameException ex) {
            LOG.error("Invalid object name");
        }
        NotificationFilterSupport filter = new NotificationFilterSupport();
        filter.enableType("JMX.mbean.registered");
        filter.enableType("JMX.mbean.unregistered");

        try {
            mbs.addNotificationListener(delegate, delegateListener , filter, null);
            LOG.info("Added registeration listener with Mbean Server");
        } catch (InstanceNotFoundException ex) {
            LOG.error("Mbean Server is not found to add listener");

        }

    }

    /**
     * Pre-provisioning case to handle all alarm mbeans which are registered
     * before installation of framework bundle. Queries the platform Mbeanserver
     * to retrieve registered alarm mbean and add attribute notification
     * listener to it.
     */
    private void addListenerToMBeans() {
        Set<ObjectName> names = new TreeSet<ObjectName>(mbs.queryNames(null, null));
        LOG.debug("adding listener for MBeans");
        names.stream().filter(AlarmNotificationListeners::isBeanMatched).forEach(bean -> {
            try {
                mbs.addNotificationListener(bean, delegateListener, null, null);
                LOG.debug("Added attribute notification listener for Mbean {} ", bean);
            } catch (InstanceNotFoundException e) {
                LOG.error("Failed to add attribute notification for Mbean", e);
            }
        });
    }

    private static boolean isBeanMatched(ObjectName beanName) {
        return beanName.toString().contains(DOMAIN);
    }

    @PreDestroy
    public void stop() {
        LOG.info("AlarmNotificationListener::stop method called");
    }
}