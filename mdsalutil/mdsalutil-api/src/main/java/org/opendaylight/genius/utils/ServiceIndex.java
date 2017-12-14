/*
 * Copyright (c) 2016 HPE, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.ovsdb.utils.config.ConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility helping to configure service index.
 *
 *
 * @author Konstantin Pozdeev, HPE
 */
public final class ServiceIndex {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceIndex.class);

    private static Map<String, Short> serviceIndexMap = new ConcurrentHashMap<>();

    private ServiceIndex() { }

    public static short getIndex(String serviceName, short defaultValue) {
        if (serviceIndexMap.containsKey(serviceName)) {
            return serviceIndexMap.get(serviceName);
        }

        String servicePriority = ConfigProperties.getProperty(ServiceIndex.class, serviceName,
                String.valueOf(defaultValue));
        if (servicePriority != null) {
            try {
                serviceIndexMap.put(serviceName, Short.valueOf(servicePriority));
            } catch (NumberFormatException ex) {
                LOG.error("Wrong configuration for service {} index {}", serviceName, servicePriority);
            }
        }
        if (!serviceIndexMap.containsKey(serviceName)) {
            LOG.trace("Using default value for service {} index {}", serviceName, defaultValue);
            serviceIndexMap.put(serviceName, defaultValue);
        }

        return serviceIndexMap.get(serviceName);
    }
}
