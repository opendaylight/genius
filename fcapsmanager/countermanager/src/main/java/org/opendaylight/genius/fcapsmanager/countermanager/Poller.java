/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.countermanager;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.opendaylight.genius.fcapsmanager.PMServiceFacade;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Poller {
    private static final Logger LOG = LoggerFactory.getLogger(Poller.class);

    private final BundleContext bundleContext;
    private final PMRegistrationListener pmRegistrationListener;

    public Poller(BundleContext bundleContext, PMRegistrationListener pmRegistrationListener) {
        this.bundleContext = bundleContext;
        this.pmRegistrationListener = pmRegistrationListener;
    }

    /**
     * This method do the Polling every 5 second and retrieves the counter details.
     */
    public void polling() {
        LOG.debug("Poller Polling Mbean List and the content is {}", pmRegistrationListener.getBeanNames());
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(new PollerTask(), 0, 5, TimeUnit.SECONDS);
    }

    /**
     * Platform dependent bundle injects its handle and it is retrieved in the method.
     */
    private PMServiceFacade getPMServiceSPI() {
        PMServiceFacade service = null;
        if (bundleContext != null) {
            ServiceReference<?> serviceReference = bundleContext.getServiceReference(PMServiceFacade.class.getName());
            service = (PMServiceFacade) bundleContext.getService(serviceReference);
        }
        return service;
    }

    private class PollerTask implements Runnable {
        private final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        /**
         * Retrieve counters map from each counter mbean and send to platform.
         */
        @Override
        public void run() {
            for (ObjectName objectName : pmRegistrationListener.getBeanNames()) {
                try {
                    Map<String, String> counters = (Map<String, String>) mbs.invoke(objectName, "retrieveCounterMap",
                            null, null);
                    PMServiceFacade pmServiceFacade = getPMServiceSPI();
                    if (pmServiceFacade != null) {
                        pmServiceFacade.connectToPMFactory(counters);
                    } else {
                        LOG.debug("PM service not available");
                    }
                } catch (InstanceNotFoundException | ReflectionException | MBeanException e) {
                    LOG.error("Exception caught while retrieving countermap from mbean and sending to platform", e);
                }
            }
        }
    }
}
