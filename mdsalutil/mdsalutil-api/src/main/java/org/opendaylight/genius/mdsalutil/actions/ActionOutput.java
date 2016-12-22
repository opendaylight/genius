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
        this(0, outputNodeConnector);
    }

    public ActionOutput(Uri outputNodeConnector, int maxLength) {
        this(0, outputNodeConnector, maxLength);
    }

    public ActionOutput(int actionKey, Uri outputNodeConnector) {
        super(ActionType.output, new String[] { outputNodeConnector.getValue() }, actionKey);
        this.outputNodeConnector = outputNodeConnector;
        this.maxLength = 0;
    }

    public ActionOutput(int actionKey, Uri outputNodeConnector, int maxLength) {
        super(ActionType.output, new String[] {outputNodeConnector.getValue(), Integer.toString(maxLength)}, actionKey);
        this.outputNodeConnector = outputNodeConnector;
        this.maxLength = maxLength;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ActionOutput that = (ActionOutput) o;

        if (maxLength != that.maxLength) return false;
        return outputNodeConnector != null ? outputNodeConnector.equals(
                that.outputNodeConnector) : that.outputNodeConnector == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (outputNodeConnector != null ? outputNodeConnector.hashCode() : 0);
        result = 31 * result + maxLength;
        return result;
    }
}
