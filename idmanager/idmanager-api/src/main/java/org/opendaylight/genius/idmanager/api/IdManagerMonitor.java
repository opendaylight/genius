/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.idmanager.api;

import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;

/**
 * Monitoring for {@link IdManagerService}.
 *
 * @author Michael Vorburger
 */
public interface IdManagerMonitor {

    /**
     * Obtain description of ID pool/s. Suitable for usage e.g. by a CLI tool.
     *
     * @return Map with poolName as key, and a String describing the pool
     *         (e.g. with information about availableIds &amp; releasedIds)
     */
    Map<String, String> getLocalPoolsDetails();

}
