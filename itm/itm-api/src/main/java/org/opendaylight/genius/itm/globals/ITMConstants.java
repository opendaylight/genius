/*
 * Copyright (c) 2015 - 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.globals;

import java.math.BigInteger;

public class ITMConstants{
  public static final BigInteger COOKIE_ITM = new BigInteger("9000000", 16);
  public static final BigInteger COOKIE_ITM_EXTERNAL = new BigInteger("9050000", 16);

  public static final String ITM_IDPOOL_NAME = "Itmservices";
  public static final long ITM_IDPOOL_START = 1L;
  public static final String ITM_IDPOOL_SIZE = "100000";
  public static int LLDP_SERVICE_ID = 0;
  //Tunnel Monitoring
  public static final boolean DEFAULT_MONITOR_ENABLED = true;
  public static final int DEFAULT_MONITOR_INTERVAL = 10;
  public static final int BFD_DEFAULT_MONITOR_INTERVAL = 100;
  public static final String DUMMY_IP_ADDRESS = "0.0.0.0";
  public static final String TUNNEL_TYPE_VXLAN = "VXLAN";
  public static final String TUNNEL_TYPE_GRE = "GRE";
}

