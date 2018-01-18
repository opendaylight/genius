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
     * This method provide the counter name.
     * @param dpnId datapath id value
     * @return counter key string
     */
    public static String getCounterName(String dpnId, String counterName) {
        String counterNameKey = FcapsConstants.MODULENAME
                + FcapsConstants.ENTITY_TYPE_OFSWITCH + "<switchid=" + dpnId + ">." + counterName;
        return counterNameKey;
    }
}
