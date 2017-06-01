/*
 * Copyright © 2017 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;

/**
 * Set source IPv6 action.
 */
public class ActionSetSourceIpv6 extends ActionInfo {
    private final Ipv6Prefix source;

    public ActionSetSourceIpv6(String sourceIp) {
        this(0, sourceIp, null);
    }

    public ActionSetSourceIpv6(int actionKey, String sourceIp) {
        this(actionKey, sourceIp, null);
    }

    public ActionSetSourceIpv6(String sourceIp, String sourceMask) {
        this(0, sourceIp, sourceMask);
    }

    public ActionSetSourceIpv6(int actionKey, String sourceIp, String sourceMask) {
        this(actionKey, new Ipv6Prefix(sourceIp + "/" + (sourceMask != null ? sourceMask : "128")));
    }

    public ActionSetSourceIpv6(Ipv6Prefix source) {
        this(0, source);
    }

    public ActionSetSourceIpv6(int actionKey, Ipv6Prefix source) {
        super(actionKey);
        this.source = source;
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
                        .setIpv6Source(new Ipv6Prefix(source)).build())
                    .build())
                .build())
            .setKey(new ActionKey(newActionKey))
            .build();
    }

    public Ipv6Prefix getSource() {
        return source;
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

        ActionSetSourceIpv6 that = (ActionSetSourceIpv6) other;

        return source != null ? source.equals(that.source) : that.source == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (source != null ? source.hashCode() : 0);
        return result;
    }
}
