/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;

public enum NxmNxRegister {
  NxmNxReg0(NxmNxReg0.class),
  NxmNxReg1(NxmNxReg1.class),
  NxmNxReg6(NxmNxReg6.class)
  ;

  Class<? extends NxmNxReg> nxmNxReg;

  private NxmNxRegister(Class<? extends NxmNxReg> nxmReg) {
    nxmNxReg = nxmReg;
  }
  public final Class<? extends NxmNxReg> getClassName() {
    return nxmNxReg;
  }
}
