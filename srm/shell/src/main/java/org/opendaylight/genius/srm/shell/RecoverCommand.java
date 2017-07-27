/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.srm.shell;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.rpcs.rev170711.SrmRpcsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "srm", name = "recover", description = "Recover service or instance")
public class RecoverCommand extends OsgiCommandSupport {

    private static final Logger LOG = LoggerFactory.getLogger(RecoverCommand.class);

    private final SrmRpcsService srmRpcService;

    public RecoverCommand(SrmRpcsService srmRpcService) {
        this.srmRpcService = srmRpcService;
    }

    @Argument(index = 0, name = "type", description = "EntityType, required", required = false, multiValued = false)
    String type;

    @Argument(index = 1, name = "name", description = "EntityName, required", required = false, multiValued = false)
    String name;

    @Argument(index = 2, name = "id", description = "EntityId, optional", required = false, multiValued = false)
    String id;

    @Override
    protected Object doExecute() throws Exception {
        session.getConsole().println(getHelp());
        return null;
    }

    private String getHelp() {
        StringBuilder help = new StringBuilder("Usage:");
        help.append("srm:recover <type> <name> [ <id> ]\n");
        return help.toString();
    }

}
