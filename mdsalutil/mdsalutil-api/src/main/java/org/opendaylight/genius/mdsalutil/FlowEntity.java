/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yangtools.util.EvenMoreObjects;

public class FlowEntity extends AbstractSwitchEntity {
    private static final long serialVersionUID = 1L;

    private short tableId;
    private String flowId;
    private int priority;
    private String flowName;
    private int idleTimeOut;
    private int hardTimeOut;
    private BigInteger biCookie;
    private List<MatchInfoBase> listMatchInfo = new ArrayList<>();
    private List<InstructionInfo> listInstructionInfo = new ArrayList<>();

    private boolean strictFlag;
    private boolean sendFlowRemFlag;

    private transient FlowBuilder flowBuilder;

    public FlowEntity(BigInteger dpnId) {
        super(dpnId);
    }

    public FlowEntity(long dpnId) {
        this(BigInteger.valueOf(dpnId));
    }

    @Override
    public String toString() {
        return "FlowEntity [dpnId=" + getDpnId() + ", tableId=" + tableId + ", flowId=" + flowId + ", priority="
                + priority + ", flowName=" + flowName + ", idleTimeOut=" + idleTimeOut + ", hardTimeOut="
                + hardTimeOut + ", cookie=" + biCookie + ", matchInfo=" + listMatchInfo
                + ", instructionInfo=" + listInstructionInfo + ", strictFlag=" + strictFlag
                + ", sendFlowRemFlag=" + sendFlowRemFlag + "]";
    }

    public BigInteger getCookie() {
        return biCookie;
    }

    public String getFlowId() {
        return flowId;
    }

    public String getFlowName() {
        return flowName;
    }

    public int getHardTimeOut() {
        return hardTimeOut;
    }

    public int getIdleTimeOut() {
        return idleTimeOut;
    }

    public List<InstructionInfo> getInstructionInfoList() {
        return listInstructionInfo;
    }

    public List<MatchInfoBase> getMatchInfoList() {
        return listMatchInfo;
    }

    public int getPriority() {
        return priority;
    }

    public boolean getSendFlowRemFlag() {
        return sendFlowRemFlag;
    }

    public boolean getStrictFlag() {
        return strictFlag;
    }

    public short getTableId() {
        return tableId;
    }

    public void setCookie(BigInteger biCookie) {
        this.biCookie = biCookie;
        this.flowBuilder = null;
    }

    public FlowBuilder getFlowBuilder() {
        if (flowBuilder == null) {
            flowBuilder = new FlowBuilder();

            flowBuilder.setKey(new FlowKey(new FlowId(getFlowId())));

            flowBuilder.setTableId(getTableId());
            flowBuilder.setPriority(getPriority());
            flowBuilder.setFlowName(getFlowName());
            flowBuilder.setIdleTimeout(getIdleTimeOut());
            flowBuilder.setHardTimeout(getHardTimeOut());
            flowBuilder.setCookie(new FlowCookie(getCookie()));
            flowBuilder.setMatch(MDSALUtil.buildMatches(getMatchInfoList()));
            flowBuilder.setInstructions(MDSALUtil.buildInstructions(getInstructionInfoList()));

            flowBuilder.setStrict(getStrictFlag());
            // TODO Fix Me
            //m_flowBuilder.setResyncFlag(getResyncFlag());
            if (getSendFlowRemFlag()) {
                flowBuilder.setFlags(new FlowModFlags(false, false, false, false, true));
            }

            flowBuilder.setBarrier(false);
            flowBuilder.setInstallHw(true);
        }

        return flowBuilder;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
        if (this.flowBuilder != null) {
            this.flowBuilder.setKey(new FlowKey(new FlowId(flowId)));
        }
    }

    public void setFlowName(String flowName) {
        this.flowName = flowName;
        this.flowBuilder = null;
    }

    public void setHardTimeOut(int hardTimeOut) {
        this.hardTimeOut = hardTimeOut;
        this.flowBuilder = null;
    }

    public void setIdleTimeOut(int idleTimeOut) {
        this.idleTimeOut = idleTimeOut;
        this.flowBuilder = null;
    }

    public void setInstructionInfoList(List<InstructionInfo> listInstructionInfo) {
        this.listInstructionInfo = listInstructionInfo;
        this.flowBuilder = null;
    }

    @SuppressWarnings("unchecked")
    public void setMatchInfoList(List<? extends MatchInfoBase> listMatchInfo) {
        this.listMatchInfo = (List<MatchInfoBase>) listMatchInfo;
        this.flowBuilder = null;
    }

    public void setPriority(int priority) {
        this.priority = priority;
        this.flowBuilder = null;
    }

    public void setSendFlowRemFlag(boolean sendFlowRemFlag) {
        this.sendFlowRemFlag = sendFlowRemFlag;
        this.flowBuilder = null;
    }

    public void setStrictFlag(boolean strictFlag) {
        this.strictFlag = strictFlag;
        this.flowBuilder = null;
    }

    public void setTableId(short tableId) {
        this.tableId = tableId;
        this.flowBuilder = null;
    }

    // int variant is a convenience, because Java is dumb: setTableId(123) does
    // not work without this, and there is no short literal like 123l for long.
    public void setTableId(int shTableId) {
        if (shTableId > Short.MAX_VALUE || shTableId < Short.MIN_VALUE) {
            throw new IllegalArgumentException("tableId not a short: " + shTableId);
        }
        setTableId((short) shTableId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDpnId(), tableId, flowId, priority, flowName, idleTimeOut,
                hardTimeOut, biCookie, listMatchInfo, listInstructionInfo, strictFlag, sendFlowRemFlag);
    }

    @Override
    public boolean equals(Object obj) {
        return EvenMoreObjects.equalsHelper(this, obj,
            (self, other) -> Objects.equals(self.getDpnId(), other.getDpnId())
                          && Objects.equals(self.tableId, other.tableId)
                          && Objects.equals(self.flowId, other.flowId)
                          && Objects.equals(self.priority, other.priority)
                          && Objects.equals(self.flowName, other.flowName)
                          && Objects.equals(self.idleTimeOut, other.idleTimeOut)
                          && Objects.equals(self.hardTimeOut, other.hardTimeOut)
                          && Objects.equals(self.biCookie, other.biCookie)
                          && Objects.equals(self.listMatchInfo, other.listMatchInfo)
                          && Objects.equals(self.listInstructionInfo, other.listInstructionInfo)
                          && Objects.equals(self.strictFlag, other.strictFlag)
                          && Objects.equals(self.sendFlowRemFlag, other.sendFlowRemFlag)
                );
    }

}
