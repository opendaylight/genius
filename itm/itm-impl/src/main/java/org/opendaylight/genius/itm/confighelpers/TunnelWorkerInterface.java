/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

public  interface  TunnelWorkerInterface {

    List<ListenableFuture<Void>> buildTunnelFutureList();
}
