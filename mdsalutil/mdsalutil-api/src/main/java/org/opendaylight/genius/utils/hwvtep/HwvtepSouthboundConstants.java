/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.hwvtep;

import com.google.common.collect.ImmutableBiMap;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.EncapsulationTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.EncapsulationTypeVxlanOverIpv4;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;

// TODO (eperefr) Remove this class once it's been added to hwvtep-southbound
public class HwvtepSouthboundConstants {

    public static final String HWVTEP_ENTITY_TYPE = "hwvtep";
    public static final ImmutableBiMap<Class<? extends EncapsulationTypeBase>, String> ENCAPS_TYPE_MAP = new ImmutableBiMap.Builder<Class<? extends EncapsulationTypeBase>, String>()
            .put(EncapsulationTypeVxlanOverIpv4.class, "vxlan_over_ipv4").build();
    public static final String ELAN_ENTITY_TYPE = "elan";
    public static final String ELAN_ENTITY_NAME = "elan";
    public static final String TEP_PREFIX = "vxlan_over_ipv4:";
    public static final String PSWITCH_URI_PREFIX = "physicalswitch";
    public static final TopologyId HWVTEP_TOPOLOGY_ID = new TopologyId(new Uri("hwvtep:1"));
}

