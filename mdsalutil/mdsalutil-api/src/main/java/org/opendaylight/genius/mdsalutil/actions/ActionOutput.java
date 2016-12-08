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
    private final Uri outputNodeConnector;
    private final int maxLength;

    public ActionOutput(Uri outputNodeConnector) {
        super(ActionType.output, new String[] { outputNodeConnector.getValue() });
        this.outputNodeConnector = outputNodeConnector;
        this.maxLength = 0;
    }

    public ActionOutput(Uri outputNodeConnector, int maxLength) {
        super(ActionType.output, new String[] { outputNodeConnector.getValue(), Integer.toString(maxLength) });
        this.outputNodeConnector = outputNodeConnector;
        this.maxLength = maxLength;
    }

    public ActionOutput(int actionKey, Uri outputNodeConnector) {
        super(ActionType.output, new String[] { outputNodeConnector.getValue() }, actionKey);
        this.outputNodeConnector = outputNodeConnector;
        this.maxLength = 0;
    }

    @Deprecated
    public ActionOutput(String[] actionValues) {
        this(new Uri(actionValues[0]), actionValues.length == 2 ? Integer.parseInt(actionValues[1]) : 0);
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    public Action buildAction(int newActionKey) {
        return new ActionBuilder().setAction(
                new OutputActionCaseBuilder().setOutputAction(
                        new OutputActionBuilder().setMaxLength(maxLength)
                                .setOutputNodeConnector(outputNodeConnector).build()).build())
                .setKey(new ActionKey(newActionKey)).build();
    }
}
