/*
 * Copyright (c) 2019 Alten Calsoft Labs India Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import java.util.Collections;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.openflowplugin.extension.api.GroupingLooseResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.eric.match.rev180730.EricAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.eric.match.rev180730.Icmpv6NdReservedKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.eric.match.rev180730.eric.of.icmpv6.nd.reserved.grouping.EricOfIcmpv6NdReservedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlowWriteActionsSetField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlowWriteActionsSetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralExtensionListGrouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.grouping.ExtensionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionListBuilder;

/**
 * Set source IPv6 action.
 */
public class ActionNdReserved extends ActionInfo {

    private final long ndReserved;

    public ActionNdReserved(long ndReserved) {
        this(0, ndReserved);
    }

    public ActionNdReserved(int actionKey, long ndReserved) {
        super(actionKey);
        this.ndReserved = ndReserved;
    }

    public long getNdReserved() {
        return ndReserved;
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    @Override
    public Action buildAction(int newActionKey) {
        GroupingLooseResolver<GeneralExtensionListGrouping> eqGroup =
                new GroupingLooseResolver<>(GeneralExtensionListGrouping.class);
        eqGroup.add(GeneralAugMatchNodesNodeTableFlowWriteActionsSetField.class);

        return new ActionBuilder()
            .setAction(new SetFieldCaseBuilder()
                .setSetField(new SetFieldBuilder()
                    .addAugmentation(new GeneralAugMatchNodesNodeTableFlowWriteActionsSetFieldBuilder()
                        .setExtensionList(Collections.singletonList(new ExtensionListBuilder()
                            .setExtension(new ExtensionBuilder()
                                .addAugmentation(new EricAugMatchNodesNodeTableFlowBuilder()
                                    .setEricOfIcmpv6NdReserved(new EricOfIcmpv6NdReservedBuilder()
                                        .setIcmpv6NdReserved(ndReserved)
                                        .build())
                                    .build())
                                .build())
                            .setExtensionKey(Icmpv6NdReservedKey.class)
                            .build()))
                        .build())
                    .build())
                .build())
            .withKey(new ActionKey(newActionKey))
            .build();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }

        ActionNdReserved that = (ActionNdReserved) other;

        return ndReserved == that.ndReserved;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (ndReserved ^ ndReserved >>> 32);
        return result;
    }

    @Override
    public String toString() {
        return "ActionNdReserved [source=" + ndReserved + ", getActionKey()=" + getActionKey() + "]";
    }
}
