/*
 * Copyright © 2016 Red Hat, Inc. and others.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;

/**
 * Set Ethernet source field action.
 */
public class ActionSetFieldEthernetSource extends ActionInfo {
    private static final long serialVersionUID = 1L;

    private final MacAddress source;

    public ActionSetFieldEthernetSource(MacAddress source) {
        this(0, source);
    }

    public ActionSetFieldEthernetSource(int actionKey, MacAddress source) {
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
                    .setEthernetMatch(new EthernetMatchBuilder()
                        .setEthernetSource(new EthernetSourceBuilder().setAddress(source).build())
                        .build())
                    .build())
                .build())
            .setKey(new ActionKey(newActionKey))
            .build();
    }

    public MacAddress getSource() {
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

        ActionSetFieldEthernetSource that = (ActionSetFieldEthernetSource) other;

        return source != null ? source.equals(that.source) : that.source == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (source != null ? source.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ActionSetFieldEthernetSource [source=" + source + ", getActionKey()=" + getActionKey() + "]";
    }

}
