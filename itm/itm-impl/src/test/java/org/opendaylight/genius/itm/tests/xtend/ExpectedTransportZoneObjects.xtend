/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.tests.xtend;

import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.VtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefixBuilder;
import org.opendaylight.genius.itm.tests.ItmTestConstants;
import org.opendaylight.genius.itm.globals.ITMConstants;

import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

class ExpectedTransportZoneObjects {

    static def newTransportZone() {
        new TransportZoneBuilder >> [
            zoneName = "TZA"
            tunnelType = TunnelTypeVxlan
            subnets = #[
                new SubnetsBuilder >> [
                gatewayIp = IpAddressBuilder.getDefaultInstance(ITMConstants.DUMMY_GATEWAY_IP)
                prefix = IpPrefixBuilder.getDefaultInstance(ITMConstants.DUMMY_PREFIX)
                vlanId = ITMConstants.DUMMY_VLANID

                vteps = #[
                        new VtepsBuilder >> [
                            dpnId = ItmTestConstants.INT_DEF_BR_DPID
                            ipAddress = IpAddressBuilder.getDefaultInstance(ItmTestConstants.NB_TZ_TEP_IP)
                            optionOfTunnel = false
                        ]
                    ]
                ]
            ]
        ]
    }

    static def updatedTransportZone() {
            new TransportZoneBuilder >> [
                zoneName = "TZA"
                tunnelType = TunnelTypeVxlan
                subnets = #[
                    new SubnetsBuilder >> [
                    gatewayIp = IpAddressBuilder.getDefaultInstance(ITMConstants.DUMMY_GATEWAY_IP)
                    prefix = IpPrefixBuilder.getDefaultInstance(ITMConstants.DUMMY_PREFIX)
                    vlanId = ITMConstants.DUMMY_VLANID

                    vteps = #[
                            new VtepsBuilder >> [
                                dpnId = ItmTestConstants.INT_DEF_BR_DPID
                                ipAddress = IpAddressBuilder.getDefaultInstance(ItmTestConstants.DEF_TZ_TEP_IP)
                                optionOfTunnel = false
                            ]
                    ]
                ]
            ]
        ]
    }

}
