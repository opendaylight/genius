/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;

import java.math.BigInteger;

public interface ItmTestConstants {
    String DEF_TZ_TEP_IP = "192.168.56.30";
    String STR_DEF_TZ_TEP_DPN_ID = "1";
    BigInteger INT_DEF_TZ_TEP_DPN_ID = BigInteger.valueOf(1);

    String TZ_TEP_IP = "192.168.56.40";
    String STR_TZ_TEP_DPN_ID = "2";
    BigInteger INT_TZ_TEP_DPN_ID = BigInteger.valueOf(2);

    String TZ_NAME = "TZA";

    String DUMMY_PORT_NAME = "";
}
