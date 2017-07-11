/*
 * Copyright (c) 2015 - 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.mdsalutil.actions;

import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Dscp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;

public class ActionSetFieldDscp extends ActionInfo {
    private static final long serialVersionUID = 1L;

    private final short dscp;

    public ActionSetFieldDscp(short dscp) {
        this(0, dscp);
    }

    public ActionSetFieldDscp(int actionKey, short dscp) {
        super(actionKey);
        this.dscp = dscp;
    }

    public short getDscp() {
        return dscp;
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    @Override
    public Action buildAction(int newActionKey) {
        return new ActionBuilder().setAction(
                new SetFieldCaseBuilder().setSetField(
                        new SetFieldBuilder().setIpMatch(
                                new IpMatchBuilder().setIpDscp(
                                        new Dscp(dscp)).build())
                                .build()).build()).setKey(new ActionKey(newActionKey)).build();
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

        ActionSetFieldDscp that = (ActionSetFieldDscp) other;

        return dscp == that.dscp;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + dscp;
        return result;
    }

    @Override
    public String toString() {
        return "ActionSetFieldDscp [dscp=" + dscp + ", getActionKey()=" + getActionKey() + "]";
    }

}
