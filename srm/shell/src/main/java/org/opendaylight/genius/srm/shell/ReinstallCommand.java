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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.EntityTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.EntityTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "srm", name = "reinstall", description = "Reinstall service or instance")
public class ReinstallCommand extends OsgiCommandSupport {

    private static final Logger LOG = LoggerFactory.getLogger(ReinstallCommand.class);

    private SrmRpcsService srmRpcService;
    private final Class<? extends EntityTypeBase> entityType = EntityTypeService.class;


    public ReinstallCommand(SrmRpcsService srmRpcService) {
        this.srmRpcService = srmRpcService;
    }

    @Argument(index = 0, name = "name", description = "EntityName of type service, required",
        required = false, multiValued = false)
    String name;

    @Override
    protected Object doExecute() throws Exception {
        session.getConsole().println(getHelp());
        return null;
    }

    private String getHelp() {
        StringBuilder help = new StringBuilder("Usage:");
        help.append("srm:reinstall <name>\n");
        return help.toString();
    }

}
