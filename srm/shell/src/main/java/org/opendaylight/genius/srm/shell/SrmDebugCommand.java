/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.srm.shell;

import com.google.common.util.concurrent.CheckedFuture;
import java.util.concurrent.ExecutionException;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.ops.rev170711.ServiceOps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "srm", name = "debug", description = "SRM debug commands")
public class SrmDebugCommand extends OsgiCommandSupport {

    final Logger logger = LoggerFactory.getLogger(SrmDebugCommand.class);

    private DataBroker dataBroker;

    public SrmDebugCommand(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Option(name = "-c", aliases = {"--clear-ops"}, description = "Clear operations DS",
        required = false, multiValued = false)
    boolean clearOps;


    @Override
    protected Object doExecute() throws Exception {
        if (clearOps) {
            clearOpsDs();
        } else {
            getHelp();
        }
        return null;
    }

    private void clearOpsDs() {
        InstanceIdentifier<ServiceOps> path = getInstanceIdentifier();
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.OPERATIONAL, path);
        CheckedFuture<Void, TransactionCommitFailedException> futures = tx.submit();
        try {
            futures.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error clearing ServiceOps DS. path:{}, data:{}", path);
        }
    }

    private static InstanceIdentifier<ServiceOps> getInstanceIdentifier() {
        return InstanceIdentifier.create(ServiceOps.class);
    }

    private String getHelp() {
        StringBuilder help = new StringBuilder("Usage:");
        help.append("debug -cop\n");
        return help.toString();
    }

}
