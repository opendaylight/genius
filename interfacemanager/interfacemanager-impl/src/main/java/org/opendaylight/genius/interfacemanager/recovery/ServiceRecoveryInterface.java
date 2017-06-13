/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.recovery;

public interface ServiceRecoveryInterface {
    /**
     * Initiates recovery mechanism for a particular interface-manager entity.
     *
     * @param entityName
     *            The type entity for which recovery has to be started. eg : INTERFACE or DPN.
     * @param entityType
     *            The type of service recovery. eg :SERVICE or INSTANCE.
     * @param entityId
     *            The unique identifier for the service instance.
     */
    boolean recoverService(String entityName, String entityType, String entityId);
}
