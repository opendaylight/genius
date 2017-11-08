/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.mdsalutil;

/**
 * This service is only intented to be used by code that
 * needs to accomodate the full replay based upgrade.
 */
public interface UpgradeState {
    boolean isUpgradeInProgress();
}
