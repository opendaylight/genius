/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager;

import com.google.common.base.Optional;

import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPoolBuilder;

public interface IdHolder {

    Optional<Long> allocateId();

    Optional<String> allocateId(Long id, Long expirationTimeSec, String idKey);

    void addId(long id);

    void addId(Long id, Long expirationTimeSec, String idKey);

    boolean isIdAvailable(long curTimeSec);

    boolean isIdAvailable(Long id);

    long getAvailableIdCount();

    void refreshDataStore(IdPoolBuilder idPoolBuilder);

    void removeId(long id);
}
