/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.validator;

import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransportZoneNameAllowed implements TransportZoneValidator {
    private static final Logger LOG = LoggerFactory.getLogger(TransportZoneNameAllowed.class);
    
    @Override
    public boolean validate(TransportZone zone) {
        if(zone.getZoneName().equalsIgnoreCase(ITMConstants.DEFAULT_TRANSPORT_ZONE)){
           LOG.warn(ITMConstants.DEFAULT_TRANSPORT_ZONE+ " are not processed ");
           return false;
        }
        return true;
    }
}
