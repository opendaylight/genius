/*
 * Copyright © 2016 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;

/**
 * Set UDP destination port action.
 */
public class ActionSetUdpDestinationPort extends ActionInfo {
    private static final long serialVersionUID = 1L;

    private final int port;

    public ActionSetUdpDestinationPort(int port) {
        this(0, port);
    }

    public ActionSetUdpDestinationPort(int actionKey, int port) {
        super(actionKey);
        this.port = port;
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
                    .setLayer4Match(new UdpMatchBuilder()
                        .setUdpDestinationPort(new PortNumber(port)).build())
                    .build())
                .build())
            .setKey(new ActionKey(newActionKey))
            .build();
    }

    public int getPort() {
        return port;
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

        ActionSetUdpDestinationPort that = (ActionSetUdpDestinationPort) other;

        return port == that.port;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return "ActionSetUdpDestinationPort [port=" + port + ", getActionKey()=" + getActionKey() + "]";
    }

}
