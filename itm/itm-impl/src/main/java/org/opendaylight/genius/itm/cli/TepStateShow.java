/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

@Command(scope = "tep", name = "state-show", description="Monitors tunnel state")

    public class TepStateShow extends OsgiCommandSupport {

       @Override

       protected Object doExecute() throws Exception {

              session.getConsole().println("Executing show TEP states command");

       return null;

       }
    }