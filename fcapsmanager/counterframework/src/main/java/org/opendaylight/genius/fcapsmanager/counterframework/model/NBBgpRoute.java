/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.counterframework.model;

import java.math.BigInteger;

public class NBBgpRoute {
    private BigInteger route_distinguisher;
    private BigInteger routes;

    public BigInteger getRoute_distinguisher() {
        return route_distinguisher;
    }

    public void setRoute_distinguisher(BigInteger route_distinguisher) {
        this.route_distinguisher = route_distinguisher;
    }

    public BigInteger getRoutes() {
        return routes;
    }

    public void setRoutes(BigInteger routes) {
        this.routes = routes;
    }
}