/*
 * Copyright © 2016 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.ActionType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;

/**
 * Set source IP action.
 */
public class ActionSetSourceIp extends ActionInfo {
    private final Ipv4Prefix source;

    public ActionSetSourceIp(String sourceIp) {
        this(0, sourceIp, null);
    }

    public ActionSetSourceIp(int actionKey, String sourceIp) {
        this(actionKey, sourceIp, null);
    }

    public ActionSetSourceIp(String sourceIp, String sourceMask) {
        this(0, sourceIp, sourceMask);
    }

    public ActionSetSourceIp(int actionKey, String sourceIp, String sourceMask) {
        this(actionKey, new Ipv4Prefix(sourceIp + "/" + (sourceMask != null ? sourceMask : "32")));
    }

    public ActionSetSourceIp(Ipv4Prefix source) {
        this(0, source);
    }

    public ActionSetSourceIp(int actionKey, Ipv4Prefix source) {
        super(ActionType.set_source_ip, source.toString().split("/"), actionKey);
        this.source = source;
    }

    @Deprecated
    public ActionSetSourceIp(ActionInfo actionInfo) {
        this(actionInfo.getActionKey(), actionInfo.getActionValues()[0],
            actionInfo.getActionValues().length > 1 ? actionInfo.getActionValues()[1] : null);
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    public Action buildAction(int newActionKey) {
        return new ActionBuilder()
            .setAction(new SetFieldCaseBuilder()
                .setSetField(new SetFieldBuilder()
                    .setLayer3Match(new Ipv4MatchBuilder()
                        .setIpv4Source(new Ipv4Prefix(source)).build())
                    .build())
                .build())
            .setKey(new ActionKey(newActionKey))
            .build();
    }

    public Ipv4Prefix getSource() {
        return source;
    }
}
