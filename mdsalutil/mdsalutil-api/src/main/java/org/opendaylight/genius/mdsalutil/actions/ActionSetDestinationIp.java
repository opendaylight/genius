/*
 * Copyright © 2016 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;

/**
 * Set destination IP action.
 */
public class ActionSetDestinationIp extends ActionInfo {

    private final Ipv4Prefix destination;

    public ActionSetDestinationIp(String destinationIp) {
        this(0, destinationIp, null);
    }

    public ActionSetDestinationIp(int actionKey, String destinationIp) {
        this(actionKey, destinationIp, null);
    }

    public ActionSetDestinationIp(String destinationIp, String destinationMask) {
        this(0, destinationIp, destinationMask);
    }

    public ActionSetDestinationIp(int actionKey, String destinationIp, String destinationMask) {
        this(actionKey, new Ipv4Prefix(destinationIp + "/" + (destinationMask != null ? destinationMask : "32")));
    }

    public ActionSetDestinationIp(Ipv4Prefix destination) {
        this(0, destination);
    }

    public ActionSetDestinationIp(int actionKey, Ipv4Prefix destination) {
        super(actionKey);
        this.destination = destination;
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
                    .setLayer3Match(new Ipv4MatchBuilder()
                        .setIpv4Destination(new Ipv4Prefix(destination)).build())
                    .build())
                .build())
            .setKey(new ActionKey(newActionKey))
            .build();
    }

    public Ipv4Prefix getDestination() {
        return destination;
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

        ActionSetDestinationIp that = (ActionSetDestinationIp) other;

        return destination != null ? destination.equals(that.destination) : that.destination == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (destination != null ? destination.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ActionSetDestinationIp [destination=" + destination + ", getActionKey()=" + getActionKey() + "]";
    }

}
