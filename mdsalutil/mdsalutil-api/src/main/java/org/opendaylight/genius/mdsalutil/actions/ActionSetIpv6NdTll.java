/*
 * Copyright © 2018 Alten Calsoftlabs India Pvt Ltd. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;

/**
 * Set IPv6 ND TLL action.
 */
public class ActionSetIpv6NdTll extends ActionInfo {

    private final MacAddress ndTll;


    public ActionSetIpv6NdTll(MacAddress ndTll) {
        this(0, ndTll);
    }

    public ActionSetIpv6NdTll(int actionKey, MacAddress ndTll) {
        super(actionKey);
        this.ndTll = ndTll;
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    @Override
    public Action buildAction(int newActionKey) {
        return new ActionBuilder()
                .setAction(new SetFieldCaseBuilder()
                        .setSetField(new SetFieldBuilder()
                                .setLayer3Match(new Ipv6MatchBuilder()
                                        .setIpv6NdTll(new MacAddress(ndTll)).build())
                                .build())
                        .build())
                .setKey(new ActionKey(newActionKey))
                .build();
    }

    public MacAddress getNdTll() {
        return ndTll;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }

        ActionSetIpv6NdTll that = (ActionSetIpv6NdTll) other;

        return ndTll != null ? ndTll.equals(that.ndTll) : that.ndTll == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (ndTll != null ? ndTll.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ActionSetIpv6NdTll [source=" + ndTll + ", getActionKey()=" + getActionKey() + "]";
    }

}
