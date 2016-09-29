/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.idmanager.shell;

import java.util.concurrent.ConcurrentMap;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.genius.idmanager.IdUtils;
import org.opendaylight.genius.utils.cache.CacheUtil;

@Command(scope = "idmanager", name = "show", description = "display local pool id cache")
public class IdManagerCacheCli extends OsgiCommandSupport {
    private static final String DEMARCATION = "=================================";

    @Option(name = "-pool", aliases = {"--pool"}, description = "pool name",
            required = false, multiValued = false)
    String poolName;

    @Override
    protected Object doExecute() throws Exception {
        showIdPoolCache();
        return null;
    }

    private void showIdPoolCache() {
        ConcurrentMap<String, Object> cache =  (ConcurrentMap<String, Object>) CacheUtil.getCache(IdUtils.ID_POOL_CACHE);
        if (cache == null) {
            session.getConsole().println("No caches available");
            return;
        }
        session.getConsole().println("No of pools in cluster " + cache.keySet().size());
        session.getConsole().println(DEMARCATION);
        if (poolName == null) {
            for (String idPoolName : cache.keySet()) {
                print(idPoolName, cache.get(idPoolName));
                session.getConsole().println(DEMARCATION);
                session.getConsole().println(DEMARCATION);
            }
            return;
        }
        Object idPool = cache.get(poolName);
        if (idPool == null) {
            session.getConsole().println("Local Id pool not found for " + poolName);
            return;
        }
        print(poolName, idPool);
    }

    private void print(String idPoolName, Object idPool) {
        session.getConsole().println("Pool name : " + idPoolName);
        session.getConsole().println("IdPool " + idPool);
    }
}