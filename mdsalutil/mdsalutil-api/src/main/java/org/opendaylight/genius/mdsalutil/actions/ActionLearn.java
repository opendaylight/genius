/*
 * Copyright © 2016 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.actions;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.FlowModAddMatchFromFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.FlowModAddMatchFromValueCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.FlowModCopyFieldIntoFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.FlowModCopyValueIntoFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.FlowModOutputToPortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.flow.mod.add.match.from.field._case.FlowModAddMatchFromFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.flow.mod.add.match.from.value._case.FlowModAddMatchFromValueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.flow.mod.copy.field.into.field._case.FlowModCopyFieldIntoFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.flow.mod.copy.value.into.field._case.FlowModCopyValueIntoFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.flow.mod.spec.flow.mod.spec.flow.mod.output.to.port._case.FlowModOutputToPortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionLearnNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.learn.grouping.NxLearnBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.learn.grouping.nx.learn.FlowMods;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.learn.grouping.nx.learn.FlowModsBuilder;

/**
 * Learn action.
 */
public class ActionLearn extends ActionInfo {

    private final int idleTimeout;
    private final int hardTimeout;
    private final int priority;
    private final BigInteger cookie;
    private final int flags;
    private final short tableId;
    private final int finIdleTimeout;
    private final int finHardTimeout;
    private final List<FlowMod> flowMods = new ArrayList<>();

    public ActionLearn(int idleTimeout, int hardTimeout, int priority, BigInteger cookie, int flags, short tableId,
        int finIdleTimeout, int finHardTimeout, List<FlowMod> flowMods) {
        this(0, idleTimeout, hardTimeout, priority, cookie, flags, tableId, finIdleTimeout, finHardTimeout, flowMods);
    }

    public ActionLearn(int actionKey, int idleTimeout, int hardTimeout, int priority, BigInteger cookie, int flags,
        short tableId, int finIdleTimeout, int finHardTimeout, List<FlowMod> flowMods) {
        super(actionKey);
        this.idleTimeout = idleTimeout;
        this.hardTimeout = hardTimeout;
        this.priority = priority;
        this.cookie = cookie;
        this.flags = flags;
        this.tableId = tableId;
        this.finIdleTimeout = finIdleTimeout;
        this.finHardTimeout = finHardTimeout;
        this.flowMods.addAll(flowMods);
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

    @Override
    public Action buildAction(int newActionKey) {
        NxLearnBuilder learnBuilder = new NxLearnBuilder();
        learnBuilder
            .setIdleTimeout(idleTimeout)
            .setHardTimeout(hardTimeout)
            .setPriority(priority)
            .setCookie(cookie)
            .setFlags(flags)
            .setTableId(tableId)
            .setFinIdleTimeout(finIdleTimeout)
            .setFinHardTimeout(finHardTimeout);

        learnBuilder.setFlowMods(this.flowMods.stream().map(FlowMod::buildFlowMod).collect(Collectors.toList()));

        return new ActionBuilder()
            .setKey(new ActionKey(newActionKey))
            .setAction(new NxActionLearnNodesNodeTableFlowApplyActionsCaseBuilder()
                .setNxLearn(learnBuilder.build()).build())
            .build();
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public int getHardTimeout() {
        return hardTimeout;
    }

    public int getPriority() {
        return priority;
    }

    public BigInteger getCookie() {
        return cookie;
    }

    public int getFlags() {
        return flags;
    }

    public short getTableId() {
        return tableId;
    }

    public int getFinIdleTimeout() {
        return finIdleTimeout;
    }

    public int getFinHardTimeout() {
        return finHardTimeout;
    }

    public List<FlowMod> getFlowMods() {
        return flowMods;
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

        ActionLearn that = (ActionLearn) other;

        if (idleTimeout != that.idleTimeout) {
            return false;
        }
        if (hardTimeout != that.hardTimeout) {
            return false;
        }
        if (priority != that.priority) {
            return false;
        }
        if (flags != that.flags) {
            return false;
        }
        if (tableId != that.tableId) {
            return false;
        }
        if (finIdleTimeout != that.finIdleTimeout) {
            return false;
        }
        if (finHardTimeout != that.finHardTimeout) {
            return false;
        }
        if (cookie != null ? !cookie.equals(that.cookie) : that.cookie != null) {
            return false;
        }
        return flowMods.equals(that.flowMods);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + idleTimeout;
        result = 31 * result + hardTimeout;
        result = 31 * result + priority;
        result = 31 * result + (cookie != null ? cookie.hashCode() : 0);
        result = 31 * result + flags;
        result = 31 * result + tableId;
        result = 31 * result + finIdleTimeout;
        result = 31 * result + finHardTimeout;
        result = 31 * result + flowMods.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ActionLearn [actionKey=" + getActionKey() + ", idleTimeout=" + idleTimeout + ", hardTimeout="
                + hardTimeout + ", priority=" + priority + ", cookie=" + cookie + ", flags=" + flags + ", tableId="
                + tableId + ", finIdleTimeout=" + finIdleTimeout + ", finHardTimeout=" + finHardTimeout + ", flowMods="
                + flowMods + "]";
    }

    public interface FlowMod {
        FlowMods buildFlowMod();
    }

    public static class MatchFromField implements FlowMod {
        private final long sourceField;
        private final int srcOffset;
        private final long destField;
        private final int dstOffset;
        private final int bits;

        public MatchFromField(long sourceField, int srcOffset, long destField, int dstOffset, int bits) {
            this.sourceField = sourceField;
            this.srcOffset = srcOffset;
            this.destField = destField;
            this.dstOffset = dstOffset;
            this.bits = bits;
        }

        public MatchFromField(long sourceField, long destField, int bits) {
            this(sourceField, 0, destField, 0, bits);
        }

        public long getSourceField() {
            return sourceField;
        }

        public long getDestField() {
            return destField;
        }

        public int getBits() {
            return bits;
        }

        public int getSrcOffset() {
            return srcOffset;
        }

        public int getDstOffset() {
            return dstOffset;
        }

        @Override
        public FlowMods buildFlowMod() {
            FlowModAddMatchFromFieldBuilder builder = new FlowModAddMatchFromFieldBuilder();
            builder.setSrcField(sourceField);
            builder.setSrcOfs(srcOffset);
            builder.setDstField(destField);
            builder.setDstOfs(dstOffset);
            builder.setFlowModNumBits(bits);

            FlowModsBuilder flowModsBuilder = new FlowModsBuilder();
            FlowModAddMatchFromFieldCaseBuilder caseBuilder = new FlowModAddMatchFromFieldCaseBuilder();
            caseBuilder.setFlowModAddMatchFromField(builder.build());
            flowModsBuilder.setFlowModSpec(caseBuilder.build());
            return flowModsBuilder.build();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            MatchFromField that = (MatchFromField) other;

            if (sourceField != that.sourceField) {
                return false;
            }
            if (destField != that.destField) {
                return false;
            }
            if (srcOffset != that.srcOffset) {
                return false;
            }
            if (dstOffset != that.dstOffset) {
                return false;
            }
            return bits == that.bits;
        }

        @Override
        public int hashCode() {
            int result = (int) (sourceField ^ sourceField >>> 32);
            result = 31 * result + (int) (destField ^ destField >>> 32);
            result = 31 * result + srcOffset;
            result = 31 * result + dstOffset;
            result = 31 * result + bits;
            return result;
        }

        @Override
        public String toString() {
            return "MatchFromField [sourceField=" + sourceField + ", srcOffset=" + srcOffset + ", destField="
                    + destField + ", dstOffset=" + dstOffset + ", bits=" + bits + "]";
        }
    }

    public static class MatchFromValue implements FlowMod {
        private final int value;
        private final long sourceField;
        private final int srcOffset;
        private final int bits;

        public MatchFromValue(int value, long sourceField, int srcOffset, int bits) {
            this.value = value;
            this.sourceField = sourceField;
            this.srcOffset = srcOffset;
            this.bits = bits;
        }

        public MatchFromValue(int value, long sourceField, int bits) {
            this(value, sourceField, 0, bits);
        }

        public int getValue() {
            return value;
        }

        public long getSourceField() {
            return sourceField;
        }

        public int getBits() {
            return bits;
        }

        public int getSrcOffset() {
            return srcOffset;
        }

        @Override
        public FlowMods buildFlowMod() {
            FlowModAddMatchFromValueBuilder builder = new FlowModAddMatchFromValueBuilder();
            builder.setValue(value);
            builder.setSrcField(sourceField);
            builder.setSrcOfs(srcOffset);
            builder.setFlowModNumBits(bits);

            FlowModsBuilder flowModsBuilder = new FlowModsBuilder();
            FlowModAddMatchFromValueCaseBuilder caseBuilder = new FlowModAddMatchFromValueCaseBuilder();
            caseBuilder.setFlowModAddMatchFromValue(builder.build());
            flowModsBuilder.setFlowModSpec(caseBuilder.build());
            return flowModsBuilder.build();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            MatchFromValue that = (MatchFromValue) other;

            if (value != that.value) {
                return false;
            }
            if (sourceField != that.sourceField) {
                return false;
            }
            if (srcOffset != that.srcOffset) {
                return false;
            }
            return bits == that.bits;
        }

        @Override
        public int hashCode() {
            int result = value;
            result = 31 * result + (int) (sourceField ^ sourceField >>> 32);
            result = 31 * result + srcOffset;
            result = 31 * result + bits;
            return result;
        }

        @Override
        public String toString() {
            return "MatchFromValue [value=" + value + ", sourceField=" + sourceField + ", srcOffset="
                    + srcOffset + ", bits=" + bits + "]";
        }
    }

    public static class CopyFromField implements FlowMod {
        private final long sourceField;
        private final int srcOffset;
        private final long destField;
        private final int dstOffset;
        private final int bits;

        public CopyFromField(long sourceField, int srcOffset, long destField, int dstOffset, int bits) {
            this.sourceField = sourceField;
            this.srcOffset = srcOffset;
            this.destField = destField;
            this.dstOffset = dstOffset;
            this.bits = bits;
        }

        public CopyFromField(long sourceField, long destField, int bits) {
            this(sourceField, 0, destField, 0, bits);
        }

        public long getSourceField() {
            return sourceField;
        }

        public long getDestField() {
            return destField;
        }

        public int getBits() {
            return bits;
        }

        public int getSrcOffset() {
            return srcOffset;
        }

        public int getDstOffset() {
            return dstOffset;
        }

        @Override
        public FlowMods buildFlowMod() {
            FlowModCopyFieldIntoFieldBuilder builder = new FlowModCopyFieldIntoFieldBuilder();
            builder.setSrcField(sourceField);
            builder.setSrcOfs(srcOffset);
            builder.setDstField(destField);
            builder.setDstOfs(dstOffset);
            builder.setFlowModNumBits(bits);

            FlowModsBuilder flowModsBuilder = new FlowModsBuilder();
            FlowModCopyFieldIntoFieldCaseBuilder caseBuilder = new FlowModCopyFieldIntoFieldCaseBuilder();
            caseBuilder.setFlowModCopyFieldIntoField(builder.build());
            flowModsBuilder.setFlowModSpec(caseBuilder.build());
            return flowModsBuilder.build();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            CopyFromField that = (CopyFromField) other;

            if (sourceField != that.sourceField) {
                return false;
            }
            if (destField != that.destField) {
                return false;
            }
            if (srcOffset != that.srcOffset) {
                return false;
            }
            if (dstOffset != that.dstOffset) {
                return false;
            }
            return bits == that.bits;
        }

        @Override
        public int hashCode() {
            int result = (int) (sourceField ^ sourceField >>> 32);
            result = 31 * result + (int) (destField ^ destField >>> 32);
            result = 31 * result + srcOffset;
            result = 31 * result + dstOffset;
            result = 31 * result + bits;
            return result;
        }

        @Override
        public String toString() {
            return "CopyFromField [sourceField=" + sourceField + ", srcOffset=" + srcOffset + ", destField="
                    + destField + ", dstOffset=" + dstOffset + ", bits=" + bits + "]";
        }
    }

    public static class CopyFromValue implements FlowMod {
        private final int value;
        private final long destField;
        private final int dstOffset;
        private final int bits;

        public CopyFromValue(int value, long destField, int bits) {
            this(value, destField, 0, bits);
        }

        public CopyFromValue(int value, long destField, int dstOffset, int bits) {
            this.value = value;
            this.destField = destField;
            this.dstOffset = dstOffset;
            this.bits = bits;
        }

        public int getValue() {
            return value;
        }

        public long getDestField() {
            return destField;
        }

        public int getBits() {
            return bits;
        }

        public int getDstOffset() {
            return dstOffset;
        }

        @Override
        public FlowMods buildFlowMod() {
            FlowModCopyValueIntoFieldBuilder builder = new FlowModCopyValueIntoFieldBuilder();
            builder.setValue(value);
            builder.setDstField(destField);
            builder.setDstOfs(dstOffset);
            builder.setFlowModNumBits(bits);

            FlowModsBuilder flowModsBuilder = new FlowModsBuilder();
            FlowModCopyValueIntoFieldCaseBuilder caseBuilder = new FlowModCopyValueIntoFieldCaseBuilder();
            caseBuilder.setFlowModCopyValueIntoField(builder.build());
            flowModsBuilder.setFlowModSpec(caseBuilder.build());
            return flowModsBuilder.build();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            CopyFromValue that = (CopyFromValue) other;

            if (value != that.value) {
                return false;
            }
            if (destField != that.destField) {
                return false;
            }
            if (dstOffset != that.dstOffset) {
                return false;
            }
            return bits == that.bits;
        }

        @Override
        public int hashCode() {
            int result = value;
            result = 31 * result + (int) (destField ^ destField >>> 32);
            result = 31 * result +  dstOffset;
            result = 31 * result + bits;
            return result;
        }

        @Override
        public String toString() {
            return "CopyFromValue [value=" + value + ", destField=" + destField + ", dstOffset=" + dstOffset
                    + ", bits=" + bits + "]";
        }
    }

    public static class OutputToPort implements FlowMod {
        private final long sourceField;
        private final int bits;

        public OutputToPort(long sourceField, int bits) {
            this.sourceField = sourceField;
            this.bits = bits;
        }

        public long getSourceField() {
            return sourceField;
        }

        public int getBits() {
            return bits;
        }

        @Override
        public FlowMods buildFlowMod() {
            FlowModOutputToPortBuilder builder = new FlowModOutputToPortBuilder();
            builder.setSrcField(sourceField);
            builder.setSrcOfs(0);
            builder.setFlowModNumBits(bits);

            FlowModsBuilder flowModsBuilder = new FlowModsBuilder();
            FlowModOutputToPortCaseBuilder caseBuilder = new FlowModOutputToPortCaseBuilder();
            caseBuilder.setFlowModOutputToPort(builder.build());
            flowModsBuilder.setFlowModSpec(caseBuilder.build());
            return flowModsBuilder.build();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            OutputToPort that = (OutputToPort) other;

            if (sourceField != that.sourceField) {
                return false;
            }
            return bits == that.bits;
        }

        @Override
        public int hashCode() {
            int result = (int) (sourceField ^ sourceField >>> 32);
            result = 31 * result + bits;
            return result;
        }

        @Override
        public String toString() {
            return "OutputToPort [sourceField=" + sourceField + ", bits=" + bits + "]";
        }
    }
}
