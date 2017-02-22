/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.nxmatches;

import com.google.common.collect.ImmutableBiMap;
import java.util.Map;
import org.opendaylight.genius.mdsalutil.NxMatchFieldType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.ExtensionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg4Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg5Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg6Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.reg.grouping.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.reg.grouping.NxmNxRegBuilder;

/**
 * Nicira extension register match.
 */
public class NxMatchRegister extends NxMatchInfoHelper<NxmNxReg, NxmNxRegBuilder> {
    private static final Map<Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match
            .rev140421.NxmNxReg>, Class<? extends ExtensionKey>> KEYS =
            ImmutableBiMap.of(
                    NxmNxReg4.class, NxmNxReg4Key.class,
                    NxmNxReg5.class, NxmNxReg5Key.class,
                    NxmNxReg6.class, NxmNxReg6Key.class
                    );

    private final Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg>
            register;
    private final long value;

    public NxMatchRegister(
            Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg>
                    register,
            long value) {
        super(register.equals(NxmNxReg4.class) ? NxMatchFieldType.nxm_reg_4
                        : register.equals(NxmNxReg5.class) ? NxMatchFieldType.nxm_reg_5 : NxMatchFieldType.nxm_reg_6,
                new long[] {value});
        if (!KEYS.containsKey(register)) {
            throw new IllegalArgumentException("Unknown NXM register " + register);
        }
        this.register = register;
        this.value = value;
    }

    @Override
    protected void applyValue(MatchBuilder matchBuilder, NxmNxReg value) {
        NxAugMatchNodesNodeTableFlow nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxReg(value).build();
        GeneralAugMatchNodesNodeTableFlow existingAugmentations = matchBuilder
                .getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
        GeneralAugMatchNodesNodeTableFlow genAugMatch = generalAugMatchBuilder(existingAugmentations,
                nxAugMatch, KEYS.get(register));
        matchBuilder.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, genAugMatch);
    }

    @Override
    protected void populateBuilder(NxmNxRegBuilder builder) {
        builder.setReg(register).setValue(value);
    }

    public Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg>
    getRegister() {
        return register;
    }

    public long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NxMatchRegister that = (NxMatchRegister) o;

        if (value != that.value) return false;
        return register != null ? register.equals(that.register) : that.register == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (register != null ? register.hashCode() : 0);
        result = 31 * result + (int) (value ^ (value >>> 32));
        return result;
    }
}
