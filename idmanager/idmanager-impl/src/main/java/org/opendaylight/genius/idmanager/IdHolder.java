/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager;

import java.util.Optional;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolBuilder;

public interface IdHolder {

    Optional<Long> allocateId();

    void addId(long id);

    boolean isIdAvailable(long curTimeSec);

    long getAvailableIdCount();

    void refreshDataStore(IdPoolBuilder idPoolBuilder);
}
