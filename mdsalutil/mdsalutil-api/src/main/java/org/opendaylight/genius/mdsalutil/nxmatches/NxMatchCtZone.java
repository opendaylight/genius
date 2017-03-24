/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.nxmatches;

import com.google.common.collect.ComparisonChain;
import java.util.Comparator;
import org.opendaylight.genius.mdsalutil.MatchInfoBase;
import org.opendaylight.genius.mdsalutil.NxMatchFieldType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxCtZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.ct.zone.grouping.NxmNxCtZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.ct.zone.grouping.NxmNxCtZoneBuilder;

/**
 * Nicira extension CT zone match.
 */
public class NxMatchCtZone extends NxMatchInfoHelper<NxmNxCtZone, NxmNxCtZoneBuilder> {
    private final int zone;

    public NxMatchCtZone(int zone) {
        super(NxmNxCtZoneKey.class, NxMatchFieldType.ct_zone, new long[] {zone});
        this.zone = zone;
    }

    @Override
    protected void applyValue(NxAugMatchNodesNodeTableFlowBuilder matchBuilder, NxmNxCtZone value) {
        matchBuilder.setNxmNxCtZone(value);
    }

    @Override
    protected void populateBuilder(NxmNxCtZoneBuilder builder) {
        builder.setCtZone(zone);
    }

    public int getZone() {
        return zone;
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

        NxMatchCtZone that = (NxMatchCtZone) other;

        return zone == that.zone;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + zone;
        return result;
    }

    @Override
    public int compareTo(MatchInfoBase other) {
        return compareTo(other, new Comparator<NxMatchCtZone>() {
            @Override
            public int compare(NxMatchCtZone left, NxMatchCtZone right) {
                return ComparisonChain.start()
                  .compare(left.zone, right.zone)
                  .result();
            }
        });
    }

}
