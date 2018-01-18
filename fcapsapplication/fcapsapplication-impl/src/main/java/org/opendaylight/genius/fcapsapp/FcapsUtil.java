/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsapp;

/**
 * Created by ankit.kumar on 22-01-2018.
 */
public final class FcapsUtil {


    private FcapsUtil() {
    }

    /**
     * This method provide the counter name in below pattern
     * odl.<projectName>.<moduleName>.entitycounter/entitytype:port.entityid:
     * <switchid=value;portid=value;aliasid=value>.<countername>
     * @param dpnId
     * @return
     */
    public static String getCounterName(String dpnId, String counterName) {
        String dpnName = FcapsConstants.MODULENAME
                + FcapsConstants.ENTITY_TYPE_OFSWITCH + "<switchid=" + dpnId + ">." + counterName;
        return dpnName;
    }
}
