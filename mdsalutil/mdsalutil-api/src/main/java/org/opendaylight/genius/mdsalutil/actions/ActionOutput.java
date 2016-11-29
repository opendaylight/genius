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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;

/**
 * Output action.
 */
public class ActionOutput extends ActionInfo {
    private final long portNum;
    private final int maxLength;

    public ActionOutput(long portNum) {
        super(ActionType.output, new String[] { Long.toString(portNum) });
        this.portNum = portNum;
        this.maxLength = 0;
    }

    public ActionOutput(long portNum, int maxLength) {
        super(ActionType.output, new String[] { Long.toString(portNum), Integer.toString(maxLength) });
        this.portNum = portNum;
        this.maxLength = maxLength;
    }

    public ActionOutput(int actionKey, long portNum) {
        super(ActionType.output, new String[] { Long.toString(portNum) }, actionKey);
        this.portNum = portNum;
        this.maxLength = 0;
    }

    @Deprecated
    public ActionOutput(String[] actionValues) {
        this(Long.parseLong(actionValues[0]), actionValues.length == 2 ? Integer.parseInt(actionValues[1]) : 0);
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    public Action buildAction(int newActionKey) {
        return new ActionBuilder().setAction(
                new OutputActionCaseBuilder().setOutputAction(
                        new OutputActionBuilder().setMaxLength(maxLength)
                                .setOutputNodeConnector(new Uri(Long.toString(portNum))).build()).build())
                .setKey(new ActionKey(newActionKey)).build();
    }
}
