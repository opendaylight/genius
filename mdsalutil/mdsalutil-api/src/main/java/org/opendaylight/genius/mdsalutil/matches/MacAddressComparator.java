/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.matches;

import java.util.Comparator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;

class MacAddressComparator implements Comparator<MacAddress> {

    static MacAddressComparator INSTANCE = new MacAddressComparator();

    @Override
    public int compare(MacAddress left, MacAddress right) {
        return left.getValue().compareTo(right.getValue());
    }
}