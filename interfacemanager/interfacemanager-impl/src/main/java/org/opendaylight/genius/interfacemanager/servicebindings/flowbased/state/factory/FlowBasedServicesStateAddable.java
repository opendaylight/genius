/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;

public interface FlowBasedServicesStateAddable {
    List<ListenableFuture<Void>> bindServicesOnInterface(Interface ifaceState);
    List<ListenableFuture<Void>> bindServicesOnInterfaceType(String ifaceName);
}
