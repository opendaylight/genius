/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import java.math.BigInteger;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TunnelBuilder;

/**
 * Set tunnel id field action.
 */
public class ActionSetFieldTunnelId extends ActionInfo {

    private final BigInteger tunnelId;
    @Nullable private final BigInteger tunnelMask;

    public ActionSetFieldTunnelId(BigInteger tunnelId) {
        this(0, tunnelId);
    }

    public ActionSetFieldTunnelId(int actionKey, BigInteger tunnelId) {
        this(actionKey, tunnelId, null);
    }

    public ActionSetFieldTunnelId(BigInteger tunnelId, BigInteger tunnelMask) {
        this(0, tunnelId, tunnelMask);
    }

    public ActionSetFieldTunnelId(int actionKey, BigInteger tunnelId, BigInteger tunnelMask) {
        super(actionKey);
        this.tunnelId = tunnelId;
        this.tunnelMask = tunnelMask;
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    @Override
    public Action buildAction(int newActionKey) {
        TunnelBuilder tunnelBuilder = new TunnelBuilder()
            .setTunnelId(tunnelId);
        if (tunnelMask != null) {
            tunnelBuilder.setTunnelMask(tunnelMask);
        }
        return new ActionBuilder()
            .setAction(
                new SetFieldCaseBuilder()
                    .setSetField(
                        new SetFieldBuilder()
                            .setTunnel(tunnelBuilder.build())
                            .build())
                    .build())
            .withKey(new ActionKey(newActionKey))
            .build();
    }

    public BigInteger getTunnelId() {
        return tunnelId;
    }

    @Nullable
    public BigInteger getTunnelMask() {
        return tunnelMask;
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

        ActionSetFieldTunnelId that = (ActionSetFieldTunnelId) other;

        if (tunnelId != null ? !tunnelId.equals(that.tunnelId) : that.tunnelId != null) {
            return false;
        }
        return tunnelMask != null ? tunnelMask.equals(that.tunnelMask) : that.tunnelMask == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (tunnelId != null ? tunnelId.hashCode() : 0);
        result = 31 * result + (tunnelMask != null ? tunnelMask.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ActionSetFieldTunnelId [tunnelId=" + tunnelId + ", tunnelMask=" + tunnelMask + ", getActionKey()="
                + getActionKey() + "]";
    }

}
