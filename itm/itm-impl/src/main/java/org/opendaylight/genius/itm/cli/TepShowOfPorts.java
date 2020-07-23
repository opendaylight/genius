/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import java.util.Collection;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.cache.OfTepStateCache;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.of.teps.state.OfTep;

@Command(scope = "tep", name = "show-ofports", description = "Monitors of ports")

public class TepShowOfPorts extends OsgiCommandSupport {

    private final IITMProvider itmProvider;
    private final OfTepStateCache ofTepStateCache;

    public TepShowOfPorts(IITMProvider itmProvider, OfTepStateCache ofTepStateCache) {
        this.itmProvider = itmProvider;
        this.ofTepStateCache = ofTepStateCache;
    }

    @Override
    protected Object doExecute() {
        Collection<OfTep> ofPorts = ofTepStateCache.getAllPresent();
        if (!ofPorts.isEmpty()) {
            itmProvider.showOfPorts(ofPorts);
        } else {
            System.out.println("No OF ports configured on the switch");
        }
        return null;
    }
}
