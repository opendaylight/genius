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
import org.opendaylight.genius.mdsalutil.ActionType;
import org.opendaylight.genius.mdsalutil.NwConstants;
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

    @Deprecated
    public ActionLearn(String[] actionValues, String[][] actionValuesMatrix) {
        super(ActionType.learn, actionValues, actionValuesMatrix);
        int i = 0;
        this.idleTimeout = Integer.parseInt(actionValues[i++]);
        this.hardTimeout = Integer.parseInt(actionValues[i++]);
        this.priority = Integer.parseInt(actionValues[i++]);
        this.cookie = new BigInteger(actionValues[i++]);
        this.flags = Integer.parseInt(actionValues[i++]);
        this.tableId = Short.parseShort(actionValues[i++]);
        this.finIdleTimeout = Integer.parseInt(actionValues[i++]);
        this.finHardTimeout = Integer.parseInt(actionValues[i++]);

        for (String[] actionValueMatrix : actionValuesMatrix) {
            i = 0;
            switch (NwConstants.LearnFlowModsType.valueOf(actionValueMatrix[i++])) {
                case MATCH_FROM_FIELD:
                    this.flowMods.add(new MatchFromField(actionValueMatrix));
                    break;
                case MATCH_FROM_VALUE:
                    this.flowMods.add(new MatchFromValue(actionValueMatrix));
                    break;
                case COPY_FROM_FIELD:
                    this.flowMods.add(new CopyFromField(actionValueMatrix));
                    break;
                case COPY_FROM_VALUE:
                    this.flowMods.add(new CopyFromValue(actionValueMatrix));
                    break;
                case OUTPUT_TO_PORT:
                    this.flowMods.add(new OutputToPort(actionValueMatrix));
                    break;
            }
        }
    }

    public ActionLearn(int idleTimeout, int hardTimeout, int priority, BigInteger cookie, int flags, short tableId,
        int finIdleTimeout, int finHardTimeout, List<FlowMod> flowMods) {
        this(0, idleTimeout, hardTimeout, priority, cookie, flags, tableId, finIdleTimeout, finHardTimeout, flowMods);
    }

    public ActionLearn(int actionKey, int idleTimeout, int hardTimeout, int priority, BigInteger cookie, int flags,
        short tableId, int finIdleTimeout, int finHardTimeout, List<FlowMod> flowMods) {
        super(ActionType.learn, new String[] {
                Integer.toString(idleTimeout),
                Integer.toString(hardTimeout),
                Integer.toString(priority),
                cookie.toString(),
                Integer.toString(flags),
                Short.toString(tableId),
                Integer.toString(finIdleTimeout),
                Integer.toString(finHardTimeout)
            }, convertFlowModsToStrings(flowMods), actionKey);
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

    @Deprecated
    private static String[][] convertFlowModsToStrings(List<FlowMod> flowMods) {
        String[][] result = new String[flowMods.size()][];
        for (int i = 0; i < result.length; i++) {
            result[i] = flowMods.get(i).toStrings();
        }
        return result;
    }

    @Override
    public Action buildAction() {
        return buildAction(getActionKey());
    }

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

    public interface FlowMod {
        FlowMods buildFlowMod();

        @Deprecated
        String[] toStrings();
    }

    public static class MatchFromField implements FlowMod {
        private final long sourceField;
        private final long destField;
        private final int bits;

        public MatchFromField(long sourceField, long destField, int bits) {
            this.sourceField = sourceField;
            this.destField = destField;
            this.bits = bits;
        }

        @Deprecated
        private MatchFromField(String[] actionValueMatrix) {
            this(Long.parseLong(actionValueMatrix[1]), Long.parseLong(actionValueMatrix[2]),
                Integer.parseInt(actionValueMatrix[3]));
        }

        @Override
        public String[] toStrings() {
            return new String[] {NwConstants.LearnFlowModsType.MATCH_FROM_FIELD.name(), Long.toString(
                sourceField), Long.toString(destField), Integer.toString(bits)};
        }

        @Override
        public FlowMods buildFlowMod() {
            FlowModAddMatchFromFieldBuilder builder = new FlowModAddMatchFromFieldBuilder();
            builder.setSrcField(sourceField);
            builder.setSrcOfs(0);
            builder.setDstField(destField);
            builder.setDstOfs(0);
            builder.setFlowModNumBits(bits);

            FlowModsBuilder flowModsBuilder = new FlowModsBuilder();
            FlowModAddMatchFromFieldCaseBuilder caseBuilder = new FlowModAddMatchFromFieldCaseBuilder();
            caseBuilder.setFlowModAddMatchFromField(builder.build());
            flowModsBuilder.setFlowModSpec(caseBuilder.build());
            return flowModsBuilder.build();
        }
    }

    public static class MatchFromValue implements FlowMod {
        private final int value;
        private final long sourceField;
        private final int bits;

        public MatchFromValue(int value, long sourceField, int bits) {
            this.value = value;
            this.sourceField = sourceField;
            this.bits = bits;
        }

        @Deprecated
        private MatchFromValue(String[] actionValueMatrix) {
            this(Integer.parseInt(actionValueMatrix[1]), Long.parseLong(actionValueMatrix[2]),
                Integer.parseInt(actionValueMatrix[3]));
        }

        @Override
        public String[] toStrings() {
            return new String[] {NwConstants.LearnFlowModsType.MATCH_FROM_VALUE.name(), Integer.toString(
                value), Long.toString(sourceField), Integer.toString(bits)};
        }

        @Override
        public FlowMods buildFlowMod() {
            FlowModAddMatchFromValueBuilder builder = new FlowModAddMatchFromValueBuilder();
            builder.setValue(value);
            builder.setSrcField(sourceField);
            builder.setSrcOfs(0);
            builder.setFlowModNumBits(bits);

            FlowModsBuilder flowModsBuilder = new FlowModsBuilder();
            FlowModAddMatchFromValueCaseBuilder caseBuilder = new FlowModAddMatchFromValueCaseBuilder();
            caseBuilder.setFlowModAddMatchFromValue(builder.build());
            flowModsBuilder.setFlowModSpec(caseBuilder.build());
            return flowModsBuilder.build();
        }
    }

    public static class CopyFromField implements FlowMod {
        private final long sourceField;
        private final long destField;
        private final int bits;

        public CopyFromField(long sourceField, long destField, int bits) {
            this.sourceField = sourceField;
            this.destField = destField;
            this.bits = bits;
        }

        @Deprecated
        private CopyFromField(String[] actionValueMatrix) {
            this(Long.parseLong(actionValueMatrix[1]), Long.parseLong(actionValueMatrix[2]),
                Integer.parseInt(actionValueMatrix[3]));
        }

        @Override
        public String[] toStrings() {
            return new String[] {NwConstants.LearnFlowModsType.COPY_FROM_FIELD.name(), Long.toString(
                sourceField), Long.toString(destField), Integer.toString(bits)};
        }

        @Override
        public FlowMods buildFlowMod() {
            FlowModCopyFieldIntoFieldBuilder builder = new FlowModCopyFieldIntoFieldBuilder();
            builder.setSrcField(sourceField);
            builder.setSrcOfs(0);
            builder.setDstField(destField);
            builder.setDstOfs(0);
            builder.setFlowModNumBits(bits);

            FlowModsBuilder flowModsBuilder = new FlowModsBuilder();
            FlowModCopyFieldIntoFieldCaseBuilder caseBuilder = new FlowModCopyFieldIntoFieldCaseBuilder();
            caseBuilder.setFlowModCopyFieldIntoField(builder.build());
            flowModsBuilder.setFlowModSpec(caseBuilder.build());
            return flowModsBuilder.build();
        }
    }

    public static class CopyFromValue implements FlowMod {
        private final int value;
        private final long destField;
        private final int bits;

        public CopyFromValue(int value, long destField, int bits) {
            this.value = value;
            this.destField = destField;
            this.bits = bits;
        }

        @Deprecated
        private CopyFromValue(String[] actionValueMatrix) {
            this(Integer.parseInt(actionValueMatrix[1]), Long.parseLong(actionValueMatrix[2]),
                Integer.parseInt(actionValueMatrix[3]));
        }

        @Override
        public String[] toStrings() {
            return new String[] {NwConstants.LearnFlowModsType.COPY_FROM_VALUE.name(), Integer.toString(
                value), Long.toString(destField), Integer.toString(bits)};
        }

        @Override
        public FlowMods buildFlowMod() {
            FlowModCopyValueIntoFieldBuilder builder = new FlowModCopyValueIntoFieldBuilder();
            builder.setValue(value);
            builder.setDstField(destField);
            builder.setDstOfs(0);
            builder.setFlowModNumBits(bits);

            FlowModsBuilder flowModsBuilder = new FlowModsBuilder();
            FlowModCopyValueIntoFieldCaseBuilder caseBuilder = new FlowModCopyValueIntoFieldCaseBuilder();
            caseBuilder.setFlowModCopyValueIntoField(builder.build());
            flowModsBuilder.setFlowModSpec(caseBuilder.build());
            return flowModsBuilder.build();
        }
    }

    public static class OutputToPort implements FlowMod {
        private final long sourceField;
        private final int bits;

        public OutputToPort(long sourceField, int bits) {
            this.sourceField = sourceField;
            this.bits = bits;
        }

        @Deprecated
        private OutputToPort(String[] actionValueMatrix) {
            this(Long.parseLong(actionValueMatrix[1]), Integer.parseInt(actionValueMatrix[2]));
        }

        @Override
        public String[] toStrings() {
            return new String[] {NwConstants.LearnFlowModsType.OUTPUT_TO_PORT.name(), Long.toString(
                sourceField), Integer.toString(bits)};
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
    }
}
