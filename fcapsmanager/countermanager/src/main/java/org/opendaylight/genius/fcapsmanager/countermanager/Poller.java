/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.countermanager;

import java.lang.management.ManagementFactory;
import org.opendaylight.genius.fcapsmanager.PMService;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class Poller {
    private final static org.slf4j.Logger LOG = LoggerFactory.getLogger(Poller.class);

    private static PMService pmService = null;
    public Poller(){
        LOG.info("Poller constructor called");
    }
    //This method do the Polling every 5 second and retrieves the the counter details
    //@Override
    public void polling() {
        LOG.debug("Poller Polling Mbean List and the content is " + PMRegistrationListener.beanNames);
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(new Pollerthread(), 0, 5, TimeUnit.SECONDS);
    }

    public PMService getPmService() {
        return pmService;
    }

    public void setPmService(PMService pmService) {
        this.pmService = pmService;
        LOG.info("Poller Pmservice set");
    }
}

class Pollerthread implements Runnable {
    private final static org.slf4j.Logger LOG = LoggerFactory.getLogger(Pollerthread.class);
    MBeanServer mbs = null;
    Map<String,String> getCounter = new HashMap<>();
    Poller poller = new Poller();

    /**
     * Retrieve countermap from each counter mbean and send to platform
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        try {
            mbs = ManagementFactory.getPlatformMBeanServer();
            for (ObjectName objectName : PMRegistrationListener.beanNames) {
                getCounter=(Map<String, String>) mbs.invoke(objectName, "retrieveCounterMap",null,null);
                if(poller.getPmService() != null)
                    poller.getPmService().connectToPMFactory(getCounter);
                else
                    LOG.debug("PM service not available");
            }
        } catch (Exception e) {
            LOG.error("Exception caught {} ", e);
        }
    }
}