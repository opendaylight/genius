/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.srm;

import com.google.common.util.concurrent.Futures;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.rpcs.rev170711.RecoverInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.rpcs.rev170711.RecoverOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.rpcs.rev170711.ReinstallInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.rpcs.rev170711.ReinstallOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.rpcs.rev170711.RpcSuccess;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.rpcs.rev170711.SrmRpcsService;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SrmRpcProvider implements SrmRpcsService {

    private static final Logger LOG = LoggerFactory.getLogger(SrmRpcProvider.class);

    private final DataBroker dataBroker;

    @Inject
    public SrmRpcProvider(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public Future<RpcResult<RecoverOutput>> recover(RecoverInput input) {
        LOG.trace("recover called for {}", input);
        RecoverOutput output = null;
        RpcResultBuilder<RecoverOutput> resultBuilder;
        try {
             output = SrmRpcUtils.callSrmOp(dataBroker, input);
            LOG.trace("recover output: {}", output);
            if (RpcSuccess.class.equals(output.getResponse())) {
                resultBuilder = RpcResultBuilder.<RecoverOutput>success()
                    .withResult(output);
            } else {
                resultBuilder = RpcResultBuilder.<RecoverOutput>failed()
                    .withResult(output)
                    .withError(ErrorType.APPLICATION, output.getMessage());
            }
        } catch (NullPointerException e) {
            LOG.error("Exception calling SrmRecoveryOp", e);
            resultBuilder = RpcResultBuilder.<RecoverOutput>failed()
                .withError(ErrorType.APPLICATION, output.getMessage());
        }
        return Futures.immediateFuture(resultBuilder.build());
    }

    @Override
    public Future<RpcResult<ReinstallOutput>> reinstall(ReinstallInput input) {
        LOG.trace("reinstall called for {}", input);

        ReinstallOutput output = null;
        RpcResultBuilder<ReinstallOutput> resultBuilder;
        try {
            output = SrmRpcUtils.callSrmOp(dataBroker, input);
            LOG.trace("reinstall output: {}", output);
            if (output.isSuccessful()) {
                resultBuilder = RpcResultBuilder.<ReinstallOutput>success()
                    .withResult(output);
            } else {
                resultBuilder = RpcResultBuilder.<ReinstallOutput>failed()
                    .withResult(output)
                    .withError(ErrorType.APPLICATION, output.getMessage());
            }
        } catch (NullPointerException e) {
            LOG.error("Exception calling SrmReinstallOp", e);
            resultBuilder = RpcResultBuilder.<ReinstallOutput>failed()
                .withError(ErrorType.APPLICATION, output.getMessage());
        }
        return Futures.immediateFuture(resultBuilder.build());
    }

}
