/*
 * Copyright © 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.networkutils;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.id.pools.IdPool;


public interface RDUtils {

    String getRD(String rdKey) throws ExecutionException, InterruptedException;

    void releaseRD(String rdKey) throws ExecutionException, InterruptedException;

    Optional<IdPool> getRDPool() throws ReadFailedException;
}