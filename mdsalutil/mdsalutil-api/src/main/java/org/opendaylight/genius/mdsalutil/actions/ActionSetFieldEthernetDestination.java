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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;

/**
 * Set Ethernet destination field action.
 */
public class ActionSetFieldEthernetDestination extends ActionInfo {
    private final MacAddress destination;

    public ActionSetFieldEthernetDestination(MacAddress destination) {
        super(ActionType.set_field_eth_dest, new String[] {destination.getValue()});
        this.destination = destination;
    }

    @Deprecated
    public ActionSetFieldEthernetDestination(String[] actionValues) {
        super(ActionType.set_field_eth_dest, actionValues);
        this.destination = new MacAddress(actionValues[0]);
    }

    public Action buildAction(int newActionKey) {
        return new ActionBuilder()
            .setAction(new SetFieldCaseBuilder()
                .setSetField(new SetFieldBuilder()
                    .setEthernetMatch(new EthernetMatchBuilder()
                        .setEthernetDestination(new EthernetDestinationBuilder().setAddress(destination).build())
                        .build())
                    .build())
                .build())
            .setKey(new ActionKey(newActionKey))
            .build();
    }
}
