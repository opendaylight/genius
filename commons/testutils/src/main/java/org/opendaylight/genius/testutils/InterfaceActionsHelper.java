/*
 * Copyright © 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.testutils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.actions.ActionNxResubmit;
import org.opendaylight.genius.mdsalutil.actions.ActionRegLoad;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldTunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;


public class InterfaceActionsHelper {

    public static final int REG6_START_INDEX = 0;
    public static final int REG6_END_INDEX = 31;

    List<ActionInfo> buildEgressActions(int ifIndex) {
        List<ActionInfo> result = new ArrayList<>();
        addEgressActionInfosForInterface(ifIndex, 0, result);
        return result;
    }

    List<ActionInfo> buildEgressActions(int ifIndex, Long tunnelKey) {
        List<ActionInfo> result = new ArrayList<>();
        result.add(new ActionSetFieldTunnelId(0, BigInteger.valueOf(tunnelKey)));
        addEgressActionInfosForInterface(ifIndex, 1, result);
        return result;
    }

    List<Action> convertInfo(List<ActionInfo> listActionInfo) {
        List<Action> actionsList = new ArrayList<>();
        for (ActionInfo actionInfo : listActionInfo) {
            actionsList.add(actionInfo.buildAction());
        }
        return actionsList;
    }

    public static void addEgressActionInfosForInterface(int ifIndex, int actionKeyStart, List<ActionInfo> result) {
        long regValue = MetaDataUtil.getReg6ValueForLPortDispatcher(ifIndex, NwConstants.DEFAULT_SERVICE_INDEX);
        result.add(new ActionRegLoad(actionKeyStart++, NxmNxReg6.class, REG6_START_INDEX,
                REG6_END_INDEX, regValue));
        result.add(new ActionNxResubmit(actionKeyStart++, NwConstants.EGRESS_LPORT_DISPATCHER_TABLE));
    }
}
