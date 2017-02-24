/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.tests.xtend;

import org.opendaylight.genius.itm.tests.ItmTestConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembershipBuilder;

import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

class ExpectedDpnEndPointObjects {

    static def newDpnEndPointVxLanType() {
        new DpnEndpointsBuilder >> [
            DPNTEPsInfo  = #[
              new DPNTEPsInfoBuilder >> [
                    up = ItmTestConstants.FALSE_BOOL
                    DPNID = ItmTestConstants.dpId1
                    tunnelEndPoints = #[
                          new TunnelEndPointsBuilder >> [
                             gwIpAddress = ItmTestConstants.gtwyIp1
                             interfaceName = ItmTestConstants.parentInterfaceName
                             ipAddress = ItmTestConstants.ipAddress1
                             portname = ItmTestConstants.portName1
                             subnetMask = ItmTestConstants.ipPrefixTest
                             tunnelType = ItmTestConstants.TUNNEL_TYPE_VXLAN
                             VLANID = ItmTestConstants.vlanId
                             optionOfTunnel = ItmTestConstants.OPT_OF_TUNNEL
                             tzMembership = #[
                                  new TzMembershipBuilder >> [
                                    zoneName = ItmTestConstants.TZ_NAME
                                  ]
                             ]
                          ]
                    ]
               ]
            ]
        ]
    }

    static def newDpnEndPointGreType() {
            new DpnEndpointsBuilder >> [
                DPNTEPsInfo  = #[
                  new DPNTEPsInfoBuilder >> [
                        up = ItmTestConstants.FALSE_BOOL
                        DPNID = ItmTestConstants.dpId1
                        tunnelEndPoints = #[
                              new TunnelEndPointsBuilder >> [
                                 gwIpAddress = ItmTestConstants.gtwyIp1
                                 interfaceName = ItmTestConstants.parentInterfaceName
                                 ipAddress = ItmTestConstants.ipAddress1
                                 portname = ItmTestConstants.portName1
                                 subnetMask = ItmTestConstants.ipPrefixTest
                                 tunnelType = ItmTestConstants.TUNNEL_TYPE_GRE
                                 VLANID = ItmTestConstants.vlanId
                                 optionOfTunnel = ItmTestConstants.OPT_OF_TUNNEL
                                 tzMembership = #[
                                      new TzMembershipBuilder >> [
                                        zoneName = ItmTestConstants.TZ_NAME
                                      ]
                                 ]
                              ]
                        ]
                  ]
                ]
            ]
    }
}
