/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils;

import org.opendaylight.genius.utils.batching.ResourceBatchingManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * OSGi Activator used to shutdown thread pools etc. created by utilities in this bundle.
 *
 * @author Michael Vorburger.ch
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        ResourceBatchingManager.getInstance().close();
    }

}
