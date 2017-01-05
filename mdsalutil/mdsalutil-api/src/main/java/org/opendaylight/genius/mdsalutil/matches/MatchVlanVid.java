/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import org.opendaylight.genius.mdsalutil.MatchFieldType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;

/**
 * VLAN identifier match.
 */
public class MatchVlanVid extends MatchInfoHelper<VlanMatch, VlanMatchBuilder> {
    private final int vlanId;

    public MatchVlanVid(int vlanId) {
        super(MatchFieldType.vlan_vid, new long[] {vlanId});
        this.vlanId = vlanId;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, VlanMatch value) {
        matchBuilder.setVlanMatch(value);
    }

    @Override
    protected void populateBuilder(VlanMatchBuilder builder) {
        builder.setVlanId(new VlanIdBuilder()
                .setVlanId(new VlanId(vlanId))
                .setVlanIdPresent(vlanId != 0)
                .build());
    }

    public int getVlanId() {
        return vlanId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MatchVlanVid that = (MatchVlanVid) o;

        return vlanId == that.vlanId;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + vlanId;
        return result;
    }
}
