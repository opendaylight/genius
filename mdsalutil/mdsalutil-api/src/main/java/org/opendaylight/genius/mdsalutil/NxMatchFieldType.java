/*
 * Copyright (c) 2016 RedHat Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.MatchField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.ExtensionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.grouping.ExtensionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxCtStateKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxCtZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg4Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg5Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg6Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxTunIpv4DstKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxTunIpv4SrcKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmOfTcpDstKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmOfTcpSrcKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmOfUdpDstKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmOfUdpSrcKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.ct.state.grouping.NxmNxCtStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.ct.zone.grouping.NxmNxCtZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.reg.grouping.NxmNxRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.tun.ipv4.dst.grouping.NxmNxTunIpv4DstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.tun.ipv4.src.grouping.NxmNxTunIpv4SrcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.tun.ipv4.src.grouping.NxmNxTunIpv4Src;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.of.tcp.dst.grouping.NxmOfTcpDstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.of.tcp.src.grouping.NxmOfTcpSrcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.of.udp.dst.grouping.NxmOfUdpDstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.of.udp.src.grouping.NxmOfUdpSrcBuilder;

public enum NxMatchFieldType {

    ct_state {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return NxmNxReg.class;
        }

        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            NxmNxCtStateBuilder ctStateBuilder = (NxmNxCtStateBuilder) mapMatchBuilder.get(NxmNxCtStateBuilder.class);

            if (ctStateBuilder == null) {
                ctStateBuilder = new NxmNxCtStateBuilder();
                mapMatchBuilder.put(NxmNxCtStateBuilder.class, ctStateBuilder);
            }

            ctStateBuilder.setCtState(matchInfo.getMatchValues()[0]);
            ctStateBuilder.setMask(matchInfo.getMatchValues()[1]);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfoBase matchInfo,
                Map<Class<?>, Object> mapMatchBuilder) {
            NxmNxCtStateBuilder ctStateBuilder = (NxmNxCtStateBuilder) mapMatchBuilder
                    .remove(NxmNxCtStateBuilder.class);

            if (ctStateBuilder != null) {
                NxAugMatchNodesNodeTableFlow nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder()
                        .setNxmNxCtState(ctStateBuilder.build()).build();
                GeneralAugMatchNodesNodeTableFlow existingAugmentations = matchBuilderInOut
                        .getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
                GeneralAugMatchNodesNodeTableFlow genAugMatch = generalAugMatchBuilder(existingAugmentations,
                        nxAugMatch, NxmNxCtStateKey.class);
                matchBuilderInOut.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, genAugMatch);
            }
        }
    },

    ct_zone {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return NxmNxReg.class;
        }

        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            NxmNxCtZoneBuilder ctZoneBuilder = (NxmNxCtZoneBuilder) mapMatchBuilder.get(NxmNxCtZoneBuilder.class);

            if (ctZoneBuilder == null) {
                ctZoneBuilder = new NxmNxCtZoneBuilder();
                mapMatchBuilder.put(NxmNxCtZoneBuilder.class, ctZoneBuilder);
            }

            ctZoneBuilder.setCtZone((int) matchInfo.getMatchValues()[0]);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfoBase matchInfo,
                Map<Class<?>, Object> mapMatchBuilder) {
            NxmNxCtZoneBuilder ctZoneBuilder = (NxmNxCtZoneBuilder) mapMatchBuilder.remove(NxmNxCtZoneBuilder.class);

            if (ctZoneBuilder != null) {
                NxAugMatchNodesNodeTableFlow nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder()
                        .setNxmNxCtZone(ctZoneBuilder.build()).build();
                GeneralAugMatchNodesNodeTableFlow existingAugmentations = matchBuilderInOut
                        .getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
                GeneralAugMatchNodesNodeTableFlow genAugMatch = generalAugMatchBuilder(existingAugmentations,
                        nxAugMatch, NxmNxCtZoneKey.class);
                matchBuilderInOut.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, genAugMatch);
            }
        }

    },
    nx_tcp_src_with_mask {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return NxmNxReg.class;
        }

        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            NxmOfTcpSrcBuilder tcpSrcBuilder = (NxmOfTcpSrcBuilder) mapMatchBuilder.get(NxmOfTcpSrcBuilder.class);

            if (tcpSrcBuilder == null) {
                tcpSrcBuilder = new NxmOfTcpSrcBuilder();
                mapMatchBuilder.put(NxmOfTcpSrcBuilder.class, tcpSrcBuilder);
            }
            tcpSrcBuilder.setPort(new PortNumber((int) matchInfo.getMatchValues()[0]));
            tcpSrcBuilder.setMask((int) matchInfo.getMatchValues()[1]);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfoBase matchInfo,
                Map<Class<?>, Object> mapMatchBuilder) {
            NxmOfTcpSrcBuilder tcpSrcBuilder = (NxmOfTcpSrcBuilder) mapMatchBuilder.remove(NxmOfTcpSrcBuilder.class);

            if (tcpSrcBuilder != null) {
                NxAugMatchNodesNodeTableFlow nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder()
                        .setNxmOfTcpSrc(tcpSrcBuilder.build()).build();
                GeneralAugMatchNodesNodeTableFlow existingAugmentations = matchBuilderInOut
                        .getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
                GeneralAugMatchNodesNodeTableFlow genAugMatch = generalAugMatchBuilder(existingAugmentations,
                        nxAugMatch, NxmOfTcpSrcKey.class);
                matchBuilderInOut.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, genAugMatch);
            }
        }

    },
    nx_tcp_dst_with_mask {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return NxmNxReg.class;
        }

        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            NxmOfTcpDstBuilder tcpDstBuilder = (NxmOfTcpDstBuilder) mapMatchBuilder.get(NxmOfTcpDstBuilder.class);

            if (tcpDstBuilder == null) {
                tcpDstBuilder = new NxmOfTcpDstBuilder();
                mapMatchBuilder.put(NxmOfTcpDstBuilder.class, tcpDstBuilder);
            }
            tcpDstBuilder.setPort(new PortNumber((int) matchInfo.getMatchValues()[0]));
            tcpDstBuilder.setMask((int) matchInfo.getMatchValues()[1]);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfoBase matchInfo,
                Map<Class<?>, Object> mapMatchBuilder) {
            NxmOfTcpDstBuilder tcpDstBuilder = (NxmOfTcpDstBuilder) mapMatchBuilder.remove(NxmOfTcpDstBuilder.class);

            if (tcpDstBuilder != null) {
                NxAugMatchNodesNodeTableFlow nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder()
                        .setNxmOfTcpDst(tcpDstBuilder.build()).build();
                GeneralAugMatchNodesNodeTableFlow existingAugmentations = matchBuilderInOut
                        .getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
                GeneralAugMatchNodesNodeTableFlow genAugMatch = generalAugMatchBuilder(existingAugmentations,
                        nxAugMatch, NxmOfTcpDstKey.class);
                matchBuilderInOut.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, genAugMatch);
            }
        }

    },
    nx_udp_src_with_mask {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return NxmNxReg.class;
        }

        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            NxmOfUdpSrcBuilder udpSrcBuilder = (NxmOfUdpSrcBuilder) mapMatchBuilder.get(NxmOfUdpSrcBuilder.class);

            if (udpSrcBuilder == null) {
                udpSrcBuilder = new NxmOfUdpSrcBuilder();
                mapMatchBuilder.put(NxmOfUdpSrcBuilder.class, udpSrcBuilder);
            }
            udpSrcBuilder.setPort(new PortNumber((int) matchInfo.getMatchValues()[0]));
            udpSrcBuilder.setMask((int) matchInfo.getMatchValues()[1]);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfoBase matchInfo,
                Map<Class<?>, Object> mapMatchBuilder) {
            NxmOfUdpSrcBuilder udpSrcBuilder = (NxmOfUdpSrcBuilder) mapMatchBuilder.remove(NxmOfUdpSrcBuilder.class);

            if (udpSrcBuilder != null) {
                NxAugMatchNodesNodeTableFlow nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder()
                        .setNxmOfUdpSrc(udpSrcBuilder.build()).build();
                GeneralAugMatchNodesNodeTableFlow existingAugmentations = matchBuilderInOut
                        .getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
                GeneralAugMatchNodesNodeTableFlow genAugMatch = generalAugMatchBuilder(existingAugmentations,
                        nxAugMatch, NxmOfUdpSrcKey.class);
                matchBuilderInOut.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, genAugMatch);
            }
        }

    },
    nx_udp_dst_with_mask {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return NxmNxReg.class;
        }

        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            NxmOfUdpDstBuilder udpDstBuilder = (NxmOfUdpDstBuilder) mapMatchBuilder.get(NxmOfUdpDstBuilder.class);

            if (udpDstBuilder == null) {
                udpDstBuilder = new NxmOfUdpDstBuilder();
                mapMatchBuilder.put(NxmOfUdpDstBuilder.class, udpDstBuilder);
            }
            udpDstBuilder.setPort(new PortNumber((int) matchInfo.getMatchValues()[0]));
            udpDstBuilder.setMask((int) matchInfo.getMatchValues()[1]);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfoBase matchInfo,
                Map<Class<?>, Object> mapMatchBuilder) {
            NxmOfUdpDstBuilder udpDstBuilder = (NxmOfUdpDstBuilder) mapMatchBuilder.remove(NxmOfUdpDstBuilder.class);

            if (udpDstBuilder != null) {
                NxAugMatchNodesNodeTableFlow nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder()
                        .setNxmOfUdpDst(udpDstBuilder.build()).build();
                GeneralAugMatchNodesNodeTableFlow existingAugmentations = matchBuilderInOut
                        .getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
                GeneralAugMatchNodesNodeTableFlow genAugMatch = generalAugMatchBuilder(existingAugmentations,
                        nxAugMatch, NxmOfUdpDstKey.class);
                matchBuilderInOut.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, genAugMatch);
            }
        }
    },
    tun_src_ip {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return NxmNxReg.class;
        }

        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            NxmNxTunIpv4SrcBuilder nxmNxTunIpv4SrcBuilder =
                (NxmNxTunIpv4SrcBuilder) mapMatchBuilder.get(NxmNxTunIpv4SrcBuilder.class);

            if (nxmNxTunIpv4SrcBuilder == null) {
                nxmNxTunIpv4SrcBuilder = new NxmNxTunIpv4SrcBuilder();
                mapMatchBuilder.put(NxmNxTunIpv4SrcBuilder.class, nxmNxTunIpv4SrcBuilder);
            }
            String[] address = matchInfo.getStringMatchValues();
            nxmNxTunIpv4SrcBuilder.setIpv4Address(new Ipv4Address(address[0]));
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfoBase matchInfo,
                             Map<Class<?>, Object> mapMatchBuilder) {
            NxmNxTunIpv4SrcBuilder nxmNxTunIpv4SrcBuilder =
                (NxmNxTunIpv4SrcBuilder) mapMatchBuilder.remove(NxmNxTunIpv4SrcBuilder.class);

            if (nxmNxTunIpv4SrcBuilder != null) {
                NxAugMatchNodesNodeTableFlow nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder()
                    .setNxmNxTunIpv4Src(nxmNxTunIpv4SrcBuilder.build()).build();
                GeneralAugMatchNodesNodeTableFlow existingAugmentations = matchBuilderInOut
                    .getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
                GeneralAugMatchNodesNodeTableFlow genAugMatch = generalAugMatchBuilder(existingAugmentations,
                    nxAugMatch, NxmNxTunIpv4SrcKey.class);
                matchBuilderInOut.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, genAugMatch);
            }
        }
    },
    tun_dst_ip {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return NxmNxReg.class;
        }

        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            NxmNxTunIpv4DstBuilder nxmNxTunIpv4DstBuilder =
                (NxmNxTunIpv4DstBuilder) mapMatchBuilder.get(NxmNxTunIpv4DstBuilder.class);

            if (nxmNxTunIpv4DstBuilder == null) {
                nxmNxTunIpv4DstBuilder = new NxmNxTunIpv4DstBuilder();
                mapMatchBuilder.put(NxmNxTunIpv4DstBuilder.class, nxmNxTunIpv4DstBuilder);
            }
            String[] address = matchInfo.getStringMatchValues();
            nxmNxTunIpv4DstBuilder.setIpv4Address(new Ipv4Address(address[0]));
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfoBase matchInfo,
                             Map<Class<?>, Object> mapMatchBuilder) {
            NxmNxTunIpv4DstBuilder nxmNxTunIpv4DstBuilder =
                (NxmNxTunIpv4DstBuilder) mapMatchBuilder.remove(NxmNxTunIpv4DstBuilder.class);

            if (nxmNxTunIpv4DstBuilder != null) {
                NxAugMatchNodesNodeTableFlow nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder()
                    .setNxmNxTunIpv4Dst(nxmNxTunIpv4DstBuilder.build()).build();
                GeneralAugMatchNodesNodeTableFlow existingAugmentations = matchBuilderInOut
                    .getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
                GeneralAugMatchNodesNodeTableFlow genAugMatch = generalAugMatchBuilder(existingAugmentations,
                    nxAugMatch, NxmNxTunIpv4DstKey.class);
                matchBuilderInOut.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, genAugMatch);
            }
        }
    },
    nxm_reg_4 {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return NxmNxReg.class;
        }

        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            NxmNxRegBuilder nxmNxRegBuilder = (NxmNxRegBuilder) mapMatchBuilder.get(NxmNxRegBuilder.class);

            if (nxmNxRegBuilder == null) {
                nxmNxRegBuilder = new NxmNxRegBuilder();
                mapMatchBuilder.put(NxmNxRegBuilder.class, nxmNxRegBuilder);
            }

            nxmNxRegBuilder.setReg(NxmNxReg4.class).setValue(matchInfo.getMatchValues()[0]).build();
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfoBase matchInfo,
                             Map<Class<?>, Object> mapMatchBuilder) {
            NxmNxRegBuilder nxmNxRegBuilder = (NxmNxRegBuilder) mapMatchBuilder.remove(NxmNxRegBuilder.class);

            if (nxmNxRegBuilder != null) {
                NxAugMatchNodesNodeTableFlow nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder()
                        .setNxmNxReg(nxmNxRegBuilder.build()).build();
                GeneralAugMatchNodesNodeTableFlow existingAugmentations = matchBuilderInOut
                        .getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
                GeneralAugMatchNodesNodeTableFlow genAugMatch = generalAugMatchBuilder(existingAugmentations,
                        nxAugMatch, NxmNxReg4Key.class);
                matchBuilderInOut.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, genAugMatch);
            }
        }
    },
    nxm_reg_5 {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return NxmNxReg.class;
        }

        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            NxmNxRegBuilder nxmNxRegBuilder = (NxmNxRegBuilder) mapMatchBuilder.get(NxmNxRegBuilder.class);

            if (nxmNxRegBuilder == null) {
                nxmNxRegBuilder = new NxmNxRegBuilder();
                mapMatchBuilder.put(NxmNxRegBuilder.class, nxmNxRegBuilder);
            }

            nxmNxRegBuilder.setReg(NxmNxReg5.class).setValue(matchInfo.getMatchValues()[0]).build();
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfoBase matchInfo,
                             Map<Class<?>, Object> mapMatchBuilder) {
            NxmNxRegBuilder nxmNxRegBuilder = (NxmNxRegBuilder) mapMatchBuilder.remove(NxmNxRegBuilder.class);

            if (nxmNxRegBuilder != null) {
                NxAugMatchNodesNodeTableFlow nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder()
                        .setNxmNxReg(nxmNxRegBuilder.build()).build();
                GeneralAugMatchNodesNodeTableFlow existingAugmentations = matchBuilderInOut
                        .getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
                GeneralAugMatchNodesNodeTableFlow genAugMatch = generalAugMatchBuilder(existingAugmentations,
                        nxAugMatch, NxmNxReg5Key.class);
                matchBuilderInOut.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, genAugMatch);
            }
        }
    },
    nxm_reg_6 {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return NxmNxReg.class;
        }

        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            NxmNxRegBuilder nxmNxRegBuilder = (NxmNxRegBuilder) mapMatchBuilder.get(NxmNxRegBuilder.class);

            if (nxmNxRegBuilder == null) {
                nxmNxRegBuilder = new NxmNxRegBuilder();
                mapMatchBuilder.put(NxmNxRegBuilder.class, nxmNxRegBuilder);
            }

            nxmNxRegBuilder.setReg(NxmNxReg6.class).setValue(matchInfo.getMatchValues()[0]).build();
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfoBase matchInfo,
                             Map<Class<?>, Object> mapMatchBuilder) {
            NxmNxRegBuilder nxm = (NxmNxRegBuilder) mapMatchBuilder.remove(NxmNxRegBuilder.class);
            List<ExtensionList> extensions = new ArrayList<>();
            if (nxm != null) {
                NxAugMatchNodesNodeTableFlow am =
                        new NxAugMatchNodesNodeTableFlowBuilder()
                                .setNxmNxReg(nxm.build())
                                .build();
                extensions.add(new ExtensionListBuilder()
                        .setExtensionKey(NxmNxReg6Key.class)
                        .setExtension(new ExtensionBuilder()
                                .addAugmentation(NxAugMatchNodesNodeTableFlow.class, am)
                                .build())
                        .build());
                GeneralAugMatchNodesNodeTableFlow m = new GeneralAugMatchNodesNodeTableFlowBuilder()
                        .setExtensionList(extensions)
                        .build();
                matchBuilderInOut.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
            }
        }
    };

    /**
     * Creates the match builder object and add it to the map.
     *
     * @param matchInfo
     *            the match info object
     * @param mapMatchBuilder
     *            the match builder object
     */
    public abstract void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder);

    /**
     * Retrieves the match from the map and set in the matchBuilder.
     *
     * @param matchBuilderInOut
     *            the match builder
     * @param matchInfo
     *            the match info
     * @param mapMatchBuilder
     *            the map containing the matches
     */
    public abstract void setMatch(MatchBuilder matchBuilderInOut, MatchInfoBase matchInfo,
            Map<Class<?>, Object> mapMatchBuilder);

    protected abstract Class<? extends MatchField> getMatchType();

    protected boolean hasMatchFieldMask() {
        // Override this to return true
        return false;
    }

    protected GeneralAugMatchNodesNodeTableFlow generalAugMatchBuilder(
            GeneralAugMatchNodesNodeTableFlow existingAugmentations, NxAugMatchNodesNodeTableFlow nxAugMatch,
            Class<? extends ExtensionKey> extentionKey) {
        List<ExtensionList> extensions = null;
        if (existingAugmentations != null) {
            extensions = existingAugmentations.getExtensionList();
        }
        if (extensions == null) {
            extensions = Lists.newArrayList();
        }
        extensions.add(new ExtensionListBuilder().setExtensionKey(extentionKey)
                .setExtension(
                        new ExtensionBuilder().addAugmentation(NxAugMatchNodesNodeTableFlow.class, nxAugMatch).build())
                .build());
        return new GeneralAugMatchNodesNodeTableFlowBuilder().setExtensionList(extensions).build();
    }
}
