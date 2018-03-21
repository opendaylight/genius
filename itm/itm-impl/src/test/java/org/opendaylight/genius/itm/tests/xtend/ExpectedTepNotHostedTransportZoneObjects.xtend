/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.tests.xtend;

import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.tepsnothostedintransportzone.UnknownVtepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TepsNotHostedInTransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;

import org.opendaylight.genius.itm.tests.ItmTestConstants;

import static extension org.opendaylight.mdsal.binding.testutils.XtendBuilderExtensions.operator_doubleGreaterThan

class ExpectedTepNotHostedTransportZoneObjects {

    static def newTepNotHostedTransportZone() {
        new TepsNotHostedInTransportZoneBuilder >> [
            zoneName = ItmTestConstants.NOT_HOSTED_TZ_NAME
            unknownVteps = #[
                  new UnknownVtepsBuilder >> [
                         dpnId = ItmTestConstants.NOT_HOSTED_INT_TZ_TEPDPN_ID
                         ipAddress = IpAddressBuilder.getDefaultInstance(ItmTestConstants.NOT_HOSTED_TZ_TEP_IP)
                         ofTunnel = ItmTestConstants.OF_TUNNEL
                   ]
            ]
        ]
    }
}
