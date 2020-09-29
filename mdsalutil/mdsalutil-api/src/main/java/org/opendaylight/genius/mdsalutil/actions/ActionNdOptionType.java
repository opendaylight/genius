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
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.eric.match.rev180730.Icmpv6NdOptionsTypeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.eric.match.rev180730.eric.of.icmpv6.nd.options.type.grouping.EricOfIcmpv6NdOptionsTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlowWriteActionsSetField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlowWriteActionsSetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralExtensionListGrouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.grouping.ExtensionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionListBuilder;

/**
 * Set source IPv6 action.
 */
public class ActionNdOptionType extends ActionInfo {

    private final short ndOptionType;

    public ActionNdOptionType(short ndOptionType) {
        this(0, ndOptionType);
    }

    public ActionNdOptionType(int actionKey, short ndOptionType) {
        super(actionKey);
        this.ndOptionType = ndOptionType;
    }

    public short getNdOptionType() {
        return ndOptionType;
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
                                    .setEricOfIcmpv6NdOptionsType(new EricOfIcmpv6NdOptionsTypeBuilder()
                                        .setIcmpv6NdOptionsType(ndOptionType)
                                        .build())
                                    .build())
                                .build())
                            .setExtensionKey(Icmpv6NdOptionsTypeKey.class)
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

        ActionNdOptionType that = (ActionNdOptionType) other;

        return ndOptionType == that.ndOptionType;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + ndOptionType;
        return result;
    }

    @Override
    public String toString() {
        return "ActionNdOptionType [source=" + ndOptionType + ", getActionKey()=" + getActionKey() + "]";
    }
}
