/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.nxmatches;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.genius.mdsalutil.NxMatchInfo;
import org.opendaylight.genius.utils.SuperTypeUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.ExtensionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.grouping.ExtensionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Helper for Nicira extension matches (this is designed to be absorbed into MatchInfo once we've cleaned up
 * downstream users).
 */
public abstract class NxMatchInfoHelper<T extends DataObject, B extends Builder<T>> implements NxMatchInfo {
    private final Class<B> builderClass;
    // The key class can't be a type parameter, it varies in some subclasses
    private final Class<? extends ExtensionKey> keyClass;
    private final Constructor<B> ctor;

    protected NxMatchInfoHelper(Class<? extends ExtensionKey> keyClass) {
        this.keyClass = keyClass;
        builderClass = SuperTypeUtil.getTypeParameter(getClass(), 1);
        try {
            ctor = builderClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(builderClass + " does not define a no-arg constructor", e);
        }
    }

    @Override
    public void createInnerMatchBuilder(Map<Class<?>, Object> mapMatchBuilder) {
        populateBuilder((B) mapMatchBuilder.computeIfAbsent(builderClass, key -> {
            try {
                return ctor.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to create an instance of " + builderClass, e);
            }
        }));
    }

    @Override
    public void setMatch(MatchBuilder matchBuilder, Map<Class<?>, Object> mapMatchBuilder) {
        B builder = (B) mapMatchBuilder.remove(builderClass);

        if (builder != null) {
            NxAugMatchNodesNodeTableFlowBuilder nxAugMatchBuilder = new NxAugMatchNodesNodeTableFlowBuilder();
            applyValue(nxAugMatchBuilder, builder.build());
            NxAugMatchNodesNodeTableFlow nxAugMatch = nxAugMatchBuilder.build();
            GeneralAugMatchNodesNodeTableFlow existingAugmentations = matchBuilder
                    .augmentation(GeneralAugMatchNodesNodeTableFlow.class);
            matchBuilder.addAugmentation(generalAugMatchBuilder(existingAugmentations, nxAugMatch, keyClass));
        }
    }

    private static GeneralAugMatchNodesNodeTableFlow generalAugMatchBuilder(
            GeneralAugMatchNodesNodeTableFlow existingAugmentations, NxAugMatchNodesNodeTableFlow nxAugMatch,
            Class<? extends ExtensionKey> extentionKey) {

        Map<ExtensionListKey, ExtensionList> extensions = null;

        if (existingAugmentations != null) {
            extensions = existingAugmentations.getExtensionList();
        }
        if (extensions == null) {
            extensions = new HashMap<>();
        }

        ExtensionList extensionList = new ExtensionListBuilder().setExtensionKey(extentionKey)
                .setExtension(new ExtensionBuilder().addAugmentation(nxAugMatch).build()).build();
        extensions.put(extensionList.key(),extensionList);
        return new GeneralAugMatchNodesNodeTableFlowBuilder().setExtensionList(extensions).build();
    }

    protected abstract void applyValue(NxAugMatchNodesNodeTableFlowBuilder matchBuilder, T value);

    protected abstract void populateBuilder(B builder);

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        NxMatchInfoHelper<?, ?> that = (NxMatchInfoHelper<?, ?>) other;

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

    @Override
    public abstract String toString();
}
