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
