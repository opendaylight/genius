/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.countermanager;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {
    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    private PMRegistrationListener notificationListeners;
    private Thread listenerThread;

    @Override
    public void start(BundleContext context) throws Exception {
        LOG.info("Starting alarmmanager bundle");
        notificationListeners = new PMRegistrationListener(context);
        listenerThread = new Thread(notificationListeners);
        listenerThread.start();
    }

    @Override
    public void stop(BundleContext context) {
        LOG.info("Stopping alarmmanager bundle");
        notificationListeners.setShouldContinue(false);
        listenerThread.interrupt();
    }
}
