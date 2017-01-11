/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.counterframework.model;

import java.math.BigInteger;
import java.util.List;

public class NBBgpCounters {
    private BigInteger total_routes;
    private List<NBBgpNeighbor> bgp_neighbor_counters;
    private List<NBBgpRoute> bgp_route_counters;

    public BigInteger getTotal_routes() {
        return total_routes;
    }

    public void setTotal_routes(BigInteger total_routes) {
        this.total_routes = total_routes;
    }

    public List<NBBgpNeighbor> getBgp_neighbor_counters() {
        return bgp_neighbor_counters;
    }

    public void setBgp_neighbor_counters(List<NBBgpNeighbor> bgp_neighbor_counters) {
        this.bgp_neighbor_counters = bgp_neighbor_counters;
    }

    public List<NBBgpRoute> getBgp_route_counters() {
        return bgp_route_counters;
    }

    public void setBgp_route_counters(List<NBBgpRoute> bgp_route_counters) {
        this.bgp_route_counters = bgp_route_counters;
    }
}