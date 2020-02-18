/*
 * Copyright (c) 2019 Alten Calsoft Labs India Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.ericmatches;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.utils.SuperTypeUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.eric.match.rev180730.EricAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.eric.match.rev180730.EricAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.ExtensionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.grouping.ExtensionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionListBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;


public abstract class EricMatchInfoHelper<T extends DataObject, B extends Builder<T>> extends MatchInfo {
    private final Class<B> builderClass;
    // The key class can't be a type parameter, it varies in some subclasses
    private final Class<? extends ExtensionKey> keyClass;

    protected EricMatchInfoHelper(Class<? extends ExtensionKey> keyClass) {
        this.keyClass = keyClass;
        builderClass = SuperTypeUtil.getTypeParameter(getClass(), 1);
    }

    @Override
    public void createInnerMatchBuilder(Map<Class<?>, Object> mapMatchBuilder) {
        populateBuilder((B) mapMatchBuilder.computeIfAbsent(builderClass, key -> {
            try {
                return builderClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("Unable to create an instance of " + builderClass, e);
            }
        }));
    }

    @Override
    public void setMatch(MatchBuilder matchBuilder, Map<Class<?>, Object> mapMatchBuilder) {
        B builder = (B) mapMatchBuilder.remove(builderClass);

        if (builder != null) {

            EricAugMatchNodesNodeTableFlowBuilder ericAugMatchBuilder = new EricAugMatchNodesNodeTableFlowBuilder();
            applyValue(ericAugMatchBuilder, builder.build());
            EricAugMatchNodesNodeTableFlow ericAugMatch = ericAugMatchBuilder.build();
            GeneralAugMatchNodesNodeTableFlow existingAugmentations = matchBuilder
                    .augmentation(GeneralAugMatchNodesNodeTableFlow.class);
            GeneralAugMatchNodesNodeTableFlow genAugMatch = generalAugMatchBuilder(existingAugmentations,
                    ericAugMatch, keyClass);
            matchBuilder.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, genAugMatch);
        }
    }

    private GeneralAugMatchNodesNodeTableFlow generalAugMatchBuilder(
            GeneralAugMatchNodesNodeTableFlow existingAugmentations, EricAugMatchNodesNodeTableFlow ericAugMatch,
            Class<? extends ExtensionKey> extentionKey) {
        List<ExtensionList> extensions = null;
        if (existingAugmentations != null) {
            extensions = existingAugmentations.getExtensionList();
        }
        if (extensions == null) {
            extensions = new ArrayList<>();
        }
        extensions.add(new ExtensionListBuilder().setExtensionKey(extentionKey)
                .setExtension(
                        new ExtensionBuilder().addAugmentation(EricAugMatchNodesNodeTableFlow.class, ericAugMatch)
                                .build())
                .build());
        return new GeneralAugMatchNodesNodeTableFlowBuilder().setExtensionList(extensions).build();
    }

    protected abstract void applyValue(EricAugMatchNodesNodeTableFlowBuilder matchBuilder, T value);

    protected abstract void populateBuilder(B builder);

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        EricMatchInfoHelper<?, ?> that = (EricMatchInfoHelper<?, ?>) other;

        if (builderClass != null ? !builderClass.equals(that.builderClass) : that.builderClass != null) {
            return false;
        }
        return keyClass != null ? keyClass.equals(that.keyClass) : that.keyClass == null;
    }

    @Override
    public int hashCode() {
        int result = builderClass != null ? builderClass.hashCode() : 0;
        result = 31 * result + (keyClass != null ? keyClass.hashCode() : 0);
        return result;
    }
}
