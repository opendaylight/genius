/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.nxmatches;

import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxCtStateKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.ct.state.grouping.NxmNxCtState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.ct.state.grouping.NxmNxCtStateBuilder;

/**
 * Nicira extension CT state match.
 */
public class NxMatchCtState extends NxMatchInfoHelper<NxmNxCtState, NxmNxCtStateBuilder> {
    private final long state;
    private final long mask;

    public NxMatchCtState(long state, long mask) {
        super(NxmNxCtStateKey.class);
        this.state = state;
        this.mask = mask;
    }

    @Override
    protected void applyValue(NxAugMatchNodesNodeTableFlowBuilder matchBuilder, NxmNxCtState value) {
        matchBuilder.setNxmNxCtState(value);
    }

    public long getState() {
        return state;
    }

    public long getMask() {
        return mask;
    }

    @Override
    protected void populateBuilder(NxmNxCtStateBuilder builder) {
        builder.setCtState(state);
        builder.setMask(mask);
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

        NxMatchCtState that = (NxMatchCtState) other;

        if (state != that.state) {
            return false;
        }
        return mask == that.mask;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (state ^ (state >>> 32));
        result = 31 * result + (int) (mask ^ (mask >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "NxMatchCtState[" + state + "/" + mask + "]";
    }
}
