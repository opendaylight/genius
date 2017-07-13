/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.test.infra;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;

/**
 * Unit test the ordering of a YANG list with key.
 *
 * @author Michael Vorburger.ch
 */
public class ListSortingTest {

    @Test
    public void testListSorting() {
        TerminationPointBuilder tpb = new TerminationPointBuilder();

        OvsdbTerminationPointAugmentationBuilder augmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();
        augmentationBuilder.setOptions(Lists.newArrayList(
                new OptionsBuilder().setOption("B").setValue("b").build(),
                new OptionsBuilder().setOption("A").setValue("a").build()));

        tpb.addAugmentation(OvsdbTerminationPointAugmentation.class, augmentationBuilder.build());

        TerminationPoint tp = tpb.build();

        tp.getAugmentation(OvsdbTerminationPointAugmentation.class).getOptions()
            .sort((o1, o2) -> o1.getKey().toString().compareTo(o2.getKey().toString()));

        assertThat(tp.toString()).isEqualTo("TerminationPoint [_key=TerminationPointKey [], augmentation=["
                + "OvsdbTerminationPointAugmentation [_options=["
                + "Options [_key=OptionsKey [_option=A], _option=A, _value=a, augmentation=[]], "
                + "Options [_key=OptionsKey [_option=B], _option=B, _value=b, augmentation=[]]], ]]]");
    }
}
