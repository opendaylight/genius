/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsappjmx;

import java.util.Map;

public interface PacketInCounterMBean {
    //-----------
    // operations
    //-----------
    void updateCounter(Map<String, String> map);
    Map<String, String> retrieveCounterMap();
}
