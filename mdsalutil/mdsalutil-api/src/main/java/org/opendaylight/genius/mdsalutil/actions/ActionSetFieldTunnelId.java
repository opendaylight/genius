/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import java.math.BigInteger;
import javax.annotation.Nullable;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.ActionType;
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
        super(ActionType.set_field_tunnel_id, new BigInteger[] {tunnelId}, actionKey);
        this.tunnelId = tunnelId;
        this.tunnelMask = null;
    }

    public ActionSetFieldTunnelId(BigInteger tunnelId, BigInteger tunnelMask) {
        this(0, tunnelId, tunnelMask);
    }

    public ActionSetFieldTunnelId(int actionKey, BigInteger tunnelId, BigInteger tunnelMask) {
        super(ActionType.set_field_tunnel_id, new BigInteger[] {tunnelId, tunnelMask}, actionKey);
        this.tunnelId = tunnelId;
        this.tunnelMask = tunnelMask;
    }

    @Deprecated
    public ActionSetFieldTunnelId(BigInteger[] bigActionValues) {
        super(ActionType.set_field_tunnel_id, bigActionValues);
        this.tunnelId = bigActionValues[0];
        this.tunnelMask = (bigActionValues.length > 1) ? bigActionValues[1] : null;
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

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
            .setKey(new ActionKey(newActionKey))
            .build();
    }
}
