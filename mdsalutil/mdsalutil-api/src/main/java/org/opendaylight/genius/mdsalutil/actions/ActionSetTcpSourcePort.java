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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;

/**
 * Set TCP source port action.
 */
public class ActionSetTcpSourcePort extends ActionInfo {
    private final int port;

    public ActionSetTcpSourcePort(int port) {
        this(0, port);
    }

    public ActionSetTcpSourcePort(int actionKey, int port) {
        super(ActionType.set_tcp_source_port, new String[] {Integer.toString(port)}, actionKey);
        this.port = port;
    }

    @Deprecated
    public ActionSetTcpSourcePort(ActionInfo actionInfo) {
        this(actionInfo.getActionKey(), Integer.parseInt(actionInfo.getActionValues()[0]));
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    public Action buildAction(int newActionKey) {
        return new ActionBuilder()
            .setAction(new SetFieldCaseBuilder()
                .setSetField(new SetFieldBuilder()
                    .setLayer4Match(new TcpMatchBuilder()
                        .setTcpSourcePort(new PortNumber(port)).build())
                    .build())
                .build())
            .setKey(new ActionKey(newActionKey))
            .build();
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ActionSetTcpSourcePort that = (ActionSetTcpSourcePort) o;

        return port == that.port;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + port;
        return result;
    }
}
