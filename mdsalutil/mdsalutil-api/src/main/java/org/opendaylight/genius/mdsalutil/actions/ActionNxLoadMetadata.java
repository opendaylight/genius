/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import java.math.BigInteger;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfMetadataCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.DstBuilder;
import org.opendaylight.yangtools.yang.common.Empty;

/**
 * NX load metadata action.
 */
public class ActionNxLoadMetadata extends ActionInfo {

    private final BigInteger value;
    private final Integer startBit;
    private final Integer endBit;

    public ActionNxLoadMetadata(BigInteger value, Integer startBit, Integer endBit) {
        this(0, value, startBit, endBit);
    }

    public ActionNxLoadMetadata(int actionKey, BigInteger value, Integer startBit, Integer endBit) {
        super(actionKey);
        this.value = value;
        this.startBit = startBit;
        this.endBit = endBit;
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    @Override
    public Action buildAction(int newActionKey) {
        return new ActionBuilder()
            .setAction(new NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder()
                .setNxRegLoad(new NxRegLoadBuilder()
                    .setDst(new DstBuilder()
                        .setDstChoice(new DstOfMetadataCaseBuilder().setOfMetadata(Empty.getInstance()).build())
                        .setStart(startBit)
                        .setEnd(endBit)
                        .build())
                    .setValue(value)
                    .build())
                .build())
            .withKey(new ActionKey(newActionKey))
            .build();
    }

    public BigInteger getValue() {
        return value;
    }

    public Integer getStartBit() {
        return startBit;
    }

    public Integer getEndBit() {
        return endBit;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (endBit == null ? 0 : endBit.hashCode());
        result = prime * result + (startBit == null ? 0 : startBit.hashCode());
        result = prime * result + (value == null ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ActionNxLoadMetadata other = (ActionNxLoadMetadata) obj;
        if (endBit == null) {
            if (other.endBit != null) {
                return false;
            }
        } else if (!endBit.equals(other.endBit)) {
            return false;
        }
        if (startBit == null) {
            if (other.startBit != null) {
                return false;
            }
        } else if (!startBit.equals(other.startBit)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ActionNxLoadMetadata [value=" + value + ", startBit=" + startBit + ", endBit=" + endBit
                + ", getValue()=" + getValue() + ", getStartBit()=" + getStartBit() + ", getEndBit()=" + getEndBit()
                + "]";
    }
}
