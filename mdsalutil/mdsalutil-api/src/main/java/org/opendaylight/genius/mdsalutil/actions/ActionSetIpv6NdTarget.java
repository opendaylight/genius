/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;

/**
 * Set IPv6 ND TarAbstractClusteredAsyncDataTreeChangeListenerget action.
 */
public class ActionSetIpv6NdTarget extends ActionInfo {

    private final Ipv6Address ndTarget;


    public ActionSetIpv6NdTarget(Ipv6Address ndTarget) {
        this(0, ndTarget);
    }

    public ActionSetIpv6NdTarget(int actionKey, Ipv6Address ndTarget) {
        super(actionKey);
        this.ndTarget = ndTarget;
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
                                        .setIpv6NdTarget(new Ipv6Address(ndTarget)).build())
                                .build())
                        .build())
                .setKey(new ActionKey(newActionKey))
                .build();
    }

    public Ipv6Address getNdTarget() {
        return ndTarget;
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

        ActionSetIpv6NdTarget that = (ActionSetIpv6NdTarget) other;

        return ndTarget != null ? ndTarget.equals(that.ndTarget) : that.ndTarget == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (ndTarget != null ? ndTarget.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ActionSetIpv6NdTarget [source=" + ndTarget + ", getActionKey()=" + getActionKey() + "]";
    }

}
