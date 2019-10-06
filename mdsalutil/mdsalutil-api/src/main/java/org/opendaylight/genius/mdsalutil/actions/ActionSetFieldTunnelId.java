/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TunnelBuilder;
import org.opendaylight.yangtools.yang.common.Uint64;

/**
 * Set tunnel id field action.
 */
public class ActionSetFieldTunnelId extends ActionInfo {

    private final Uint64 tunnelId;
    private final @Nullable Uint64 tunnelMask;

    public ActionSetFieldTunnelId(Uint64 tunnelId) {
        this(0, tunnelId);
    }

    public ActionSetFieldTunnelId(int actionKey, Uint64 tunnelId) {
        this(actionKey, tunnelId, null);
    }

    public ActionSetFieldTunnelId(Uint64 tunnelId, Uint64 tunnelMask) {
        this(0, tunnelId, tunnelMask);
    }

    public ActionSetFieldTunnelId(int actionKey, Uint64 tunnelId, Uint64 tunnelMask) {
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

    public Uint64 getTunnelId() {
        return tunnelId;
    }

    public @Nullable Uint64 getTunnelMask() {
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
