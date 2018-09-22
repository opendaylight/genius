/*
 * Copyright (c) 2017 NEC Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.nxmatches;

import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxCtMarkKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.ct.mark.grouping.NxmNxCtMark;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.ct.mark.grouping.NxmNxCtMarkBuilder;

/**
 * Nicira extension CT mark match.
 */
public class NxMatchCtMark extends NxMatchInfoHelper<NxmNxCtMark, NxmNxCtMarkBuilder> {
    private final long ctMark;
    private final long mask;

    public NxMatchCtMark(long ctMark, long mask) {
        super(NxmNxCtMarkKey.class);
        this.ctMark = ctMark;
        this.mask = mask;
    }

    @Override
    protected void applyValue(NxAugMatchNodesNodeTableFlowBuilder matchBuilder, NxmNxCtMark value) {
        matchBuilder.setNxmNxCtMark(value);
    }

    public long getCtMark() {
        return ctMark;
    }

    public long getMask() {
        return mask;
    }

    @Override
    protected void populateBuilder(NxmNxCtMarkBuilder builder) {
        builder.setCtMark(ctMark);
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

        NxMatchCtMark that = (NxMatchCtMark) other;

        if (ctMark != that.ctMark) {
            return false;
        }
        return mask == that.mask;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (ctMark ^ (ctMark >>> 32));
        result = 31 * result + (int) (mask ^ (mask >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "NxMatchCtMark[" + ctMark + "/" + mask + "]";
    }
}
