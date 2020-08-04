/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import java.util.Map;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.utils.SuperTypeUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Helper for matches (this is designed to be absorbed into MatchInfo once we've cleaned up downstream users).
 */
public abstract class MatchInfoHelper<T extends DataObject, B extends Builder<T>> extends MatchInfo {
    private final Class<B> builderClass;

    MatchInfoHelper() {
        builderClass = SuperTypeUtil.getTypeParameter(getClass(), 1);
    }

    @Override
    public void createInnerMatchBuilder(Map<Class<?>, Object> mapMatchBuilder) {
        populateBuilder((B) mapMatchBuilder.computeIfAbsent(builderClass, key -> {
            try {
                return builderClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to create an instance of " + builderClass, e);
            }
        }));
    }

    @Override
    public void setMatch(MatchBuilder matchBuilder, Map<Class<?>, Object> mapMatchBuilder) {
        B builder = (B) mapMatchBuilder.remove(builderClass);

        if (builder != null) {
            applyValue(matchBuilder, builder.build());
        }
    }

    protected abstract void applyValue(MatchBuilder matchBuilder, T value);

    protected abstract void populateBuilder(B builder);

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        MatchInfoHelper<?, ?> that = (MatchInfoHelper<?, ?>) other;

        return builderClass != null ? builderClass.equals(that.builderClass) : that.builderClass == null;
    }

    @Override
    public int hashCode() {
        return builderClass != null ? builderClass.hashCode() : 0;
    }
}
