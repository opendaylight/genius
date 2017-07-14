/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.counterframework.counternorthbound;

import org.eclipse.persistence.jaxb.rs.MOXyJsonProvider;

import javax.ws.rs.core.Application;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CounterNorthboundApplication extends Application  {
    private static final int HASHMAP_SIZE = 3;

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        //northbound URIs
        classes.add(StatisticsRestApi.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        MOXyJsonProvider moxyJsonProvider = new MOXyJsonProvider();

        moxyJsonProvider.setAttributePrefix("@");
        moxyJsonProvider.setFormattedOutput(true);
        moxyJsonProvider.setIncludeRoot(false);
        moxyJsonProvider.setMarshalEmptyCollections(true);
        moxyJsonProvider.setValueWrapper("$");

        Map<String, String> namespacePrefixMapper = new HashMap<String, String>(HASHMAP_SIZE);
        // FIXME: fill in next two with XSD
        namespacePrefixMapper.put("router", "router");
        namespacePrefixMapper.put("provider", "provider");
        namespacePrefixMapper.put("binding", "binding");
        namespacePrefixMapper.put("trunkport", "trunkport");
        moxyJsonProvider.setNamespacePrefixMapper(namespacePrefixMapper);
        moxyJsonProvider.setNamespaceSeparator(':');

        Set<Object> set = new HashSet<Object>(1);
        set.add(moxyJsonProvider);
        return set;
    }
}