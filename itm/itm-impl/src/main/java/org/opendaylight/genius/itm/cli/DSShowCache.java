/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "ds", name = "showcache", description = "view the cached data")
public class DSShowCache extends OsgiCommandSupport {
    @Argument(index = 0, name = "CacheName", description = "CacheName", required = true, multiValued = false)
    private String cacheName;
    private static final Logger logger = LoggerFactory.getLogger(DSShowCache.class);
    private IITMProvider itmProvider;

    public void setItmProvider(IITMProvider itmProvider) {
        this.itmProvider = itmProvider;
    }

    @Override
    protected Object doExecute() throws Exception {
        logger.debug("Executing show Cache command");
        itmProvider.showCache(cacheName);
        return null;
    }

}
