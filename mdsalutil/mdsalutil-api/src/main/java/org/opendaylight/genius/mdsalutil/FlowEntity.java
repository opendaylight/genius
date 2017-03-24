/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.math.BigInteger;
import java.util.List;
import org.eclipse.jdt.annotation.Nullable;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.opendaylight.genius.infra.OpenDaylightImmutableStyle;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;

@Immutable
@OpenDaylightImmutableStyle
public abstract class FlowEntity extends AbstractSwitchEntity {

    // This is required as it will cause the code generation by @Immutable.org to implement Builder,
    // which is required Xtend sources can use the XtendBuilderExtensions.operator_doubleGreaterThan
    public abstract static class Builder implements org.opendaylight.yangtools.concepts.Builder<FlowEntity> {}

    // This was done because MDSALManager has this hard-coded like this, upon MDSALManager.installFlow()
    @Default
    public BigInteger getCookie() {
        return new BigInteger("0110000", 16);
    }

    public abstract String getFlowId();

    public abstract @Nullable String getFlowName();

    @Default
    public int getHardTimeOut() {
        return 0;
    }

    @Default
    public int getIdleTimeOut() {
        return 0;
    }

    public abstract List<InstructionInfo> getInstructionInfoList();

    public abstract List<MatchInfoBase> getMatchInfoList();

    @Default
    public int getPriority() {
        return 0;
    }

    @Default
    public boolean getSendFlowRemFlag() {
        return false;
    }

    @Default
    public boolean getStrictFlag() {
        return false;
    }

    public abstract short getTableId();

    public FlowBuilder getFlowBuilder() {
        FlowBuilder flowBuilder = new FlowBuilder();

        flowBuilder.setKey(new FlowKey(new FlowId(getFlowId())));

        flowBuilder.setTableId(getTableId());
        flowBuilder.setPriority(getPriority());
        flowBuilder.setFlowName(getFlowName());
        flowBuilder.setIdleTimeout(getIdleTimeOut());
        flowBuilder.setHardTimeout(getHardTimeOut());
        flowBuilder.setCookie(new FlowCookie(getCookie()));
        flowBuilder.setMatch(MDSALUtil.buildMatches(getMatchInfoList()));
        flowBuilder.setInstructions(MDSALUtil.buildInstructions(getInstructionInfoList()));

        flowBuilder.setStrict(getStrictFlag());
        // TODO flowBuilder.setResyncFlag(getResyncFlag());
        if (getSendFlowRemFlag()) {
            flowBuilder.setFlags(new FlowModFlags(false, false, false, false, true));
        }

        flowBuilder.setBarrier(false);
        flowBuilder.setInstallHw(true);

        return flowBuilder;
    }

}
