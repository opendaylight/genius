/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.counterframework.model;

import java.math.BigInteger;

public class NBTableCounters {
    private BigInteger flow_count;
    private BigInteger table_id;

    public BigInteger getFlow_count() {
        return flow_count;
    }

    public void setFlow_count(BigInteger flow_count) {
        this.flow_count = flow_count;
    }

    public BigInteger getTable_id() {
        return table_id;
    }

    public void setTable_id(BigInteger table_id) {
        this.table_id = table_id;
    }
}