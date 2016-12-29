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
    String defTzTepIp = "192.168.56.30";
    String strDefTzTepdpnId = "1";
    BigInteger intDefTzTepdpnId = BigInteger.valueOf(1);

    String tzTepIp = "192.168.56.40";
    String strTzTepdpnId = "2";
    BigInteger intTzTepdpnId = BigInteger.valueOf(2);

    String tzName = "TZA";

    String dummyPortName = "";
}
