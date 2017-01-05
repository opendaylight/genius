/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.util.Map;
import org.opendaylight.genius.mdsalutil.matches.MatchArpOp;
import org.opendaylight.genius.mdsalutil.matches.MatchArpSha;
import org.opendaylight.genius.mdsalutil.matches.MatchArpSpa;
import org.opendaylight.genius.mdsalutil.matches.MatchArpTha;
import org.opendaylight.genius.mdsalutil.matches.MatchArpTpa;
import org.opendaylight.genius.mdsalutil.matches.MatchEthernetDestination;
import org.opendaylight.genius.mdsalutil.matches.MatchEthernetSource;
import org.opendaylight.genius.mdsalutil.matches.MatchEthernetType;
import org.opendaylight.genius.mdsalutil.matches.MatchIcmpv4;
import org.opendaylight.genius.mdsalutil.matches.MatchIcmpv6;
import org.opendaylight.genius.mdsalutil.matches.MatchInPort;
import org.opendaylight.genius.mdsalutil.matches.MatchIpProtocol;
import org.opendaylight.genius.mdsalutil.matches.MatchIpv4Destination;
import org.opendaylight.genius.mdsalutil.matches.MatchIpv4Source;
import org.opendaylight.genius.mdsalutil.matches.MatchIpv6Destination;
import org.opendaylight.genius.mdsalutil.matches.MatchIpv6NdTarget;
import org.opendaylight.genius.mdsalutil.matches.MatchIpv6Source;
import org.opendaylight.genius.mdsalutil.matches.MatchMetadata;
import org.opendaylight.genius.mdsalutil.matches.MatchMplsLabel;
import org.opendaylight.genius.mdsalutil.matches.MatchPbbIsid;
import org.opendaylight.genius.mdsalutil.matches.MatchTcpDestinationPort;
import org.opendaylight.genius.mdsalutil.matches.MatchTcpFlags;
import org.opendaylight.genius.mdsalutil.matches.MatchTcpSourcePort;
import org.opendaylight.genius.mdsalutil.matches.MatchTunnelId;
import org.opendaylight.genius.mdsalutil.matches.MatchUdpDestinationPort;
import org.opendaylight.genius.mdsalutil.matches.MatchUdpSourcePort;
import org.opendaylight.genius.mdsalutil.matches.MatchVlanVid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.ArpOp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.ArpSha;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.ArpSpa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.ArpTha;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.ArpTpa;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.EthDst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.EthSrc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.EthType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Icmpv4Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Icmpv6Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.InPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.IpProto;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Ipv4Dst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Ipv4Src;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Ipv6Dst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Ipv6NdTarget;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Ipv6Src;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.MatchField;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.Metadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.MplsLabel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.PbbIsid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TcpDst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TcpFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TcpSrc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TunnelId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.UdpDst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.UdpSrc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.VlanVid;

public enum MatchFieldType {
    @Deprecated
    eth_src {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return EthSrc.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchEthernetSource(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchEthernetSource(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchEthernetSource getMatchEthernetSource(MatchInfo matchInfo) {
            if (matchInfo instanceof MatchEthernetSource) {
                return (MatchEthernetSource) matchInfo;
            }
            if (matchInfo.getStringMatchValues().length > 1) {
                return new MatchEthernetSource(new MacAddress(matchInfo.getStringMatchValues()[0]),
                        new MacAddress(matchInfo.getStringMatchValues()[1]));
            }
            return new MatchEthernetSource(new MacAddress(matchInfo.getStringMatchValues()[0]));
        }
    },

    @Deprecated
    eth_dst {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return EthDst.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchEthernetDestination(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchEthernetDestination(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchEthernetDestination getMatchEthernetDestination(MatchInfo matchInfo) {
            if (matchInfo instanceof MatchEthernetDestination) {
                return (MatchEthernetDestination) matchInfo;
            }
            if (matchInfo.getStringMatchValues().length > 1) {
                return new MatchEthernetDestination(new MacAddress(matchInfo.getStringMatchValues()[0]),
                        new MacAddress(matchInfo.getStringMatchValues()[1]));
            }
            return new MatchEthernetDestination(new MacAddress(matchInfo.getStringMatchValues()[0]));
        }
    },

    @Deprecated
    eth_type {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return EthType.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchEthernetType(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchEthernetType(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchEthernetType getMatchEthernetType(MatchInfo matchInfo) {
            return matchInfo instanceof MatchEthernetType ? (MatchEthernetType) matchInfo : new MatchEthernetType(
                    matchInfo.getMatchValues()[0]);
        }
    },

    @Deprecated
    in_port {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return InPort.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchInPort(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchInPort(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchInPort getMatchInPort(MatchInfo matchInfo) {
            return matchInfo instanceof MatchInPort ? (MatchInPort) matchInfo : new MatchInPort(
                    matchInfo.getBigMatchValues()[0], matchInfo.getBigMatchValues()[1].longValue());
        }
    },

    @Deprecated
    ip_proto {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return IpProto.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIpProtocol(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIpProtocol(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchIpProtocol getMatchIpProtocol(MatchInfo matchInfo) {
            return matchInfo instanceof MatchIpProtocol ? (MatchIpProtocol) matchInfo : new MatchIpProtocol(
                    (short) matchInfo.getMatchValues()[0]);
        }
    },

    @Deprecated
    ipv4_dst {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Ipv4Dst.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIpv4Destination(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIpv4Destination(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchIpv4Destination getMatchIpv4Destination(MatchInfo matchInfo) {
            return matchInfo instanceof MatchIpv4Destination ? (MatchIpv4Destination) matchInfo : new
                    MatchIpv4Destination(matchInfo.getMatchValues()[0], matchInfo.getMatchValues()[1]);
        }
    },

    @Deprecated
    ipv4_src {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Ipv4Src.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIpv4Source(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIpv4Source(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchIpv4Source getMatchIpv4Source(MatchInfo matchInfo) {
            return matchInfo instanceof MatchIpv4Source ? (MatchIpv4Source) matchInfo : new MatchIpv4Source(
                    matchInfo.getMatchValues()[0], matchInfo.getMatchValues()[1]);
        }
    },

    @Deprecated
    ipv4_destination {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Ipv4Dst.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIpv4Destination(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIpv4Destination(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchIpv4Destination getMatchIpv4Destination(MatchInfo matchInfo) {
            return matchInfo instanceof MatchIpv4Destination ? (MatchIpv4Destination) matchInfo : new
                    MatchIpv4Destination(matchInfo.getStringMatchValues()[0], matchInfo.getStringMatchValues()[1]);
        }
    },

    @Deprecated
    ipv4_source {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Ipv4Src.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIpv4Source(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIpv4Source(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchIpv4Source getMatchIpv4Source(MatchInfo matchInfo) {
            return matchInfo instanceof MatchIpv4Source ? (MatchIpv4Source) matchInfo : new MatchIpv4Source(
                    matchInfo.getStringMatchValues()[0], matchInfo.getStringMatchValues()[1]);
        }
    },

    @Deprecated
    ipv6_destination {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Ipv6Dst.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIpv6Destination(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIpv6Destination(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchIpv6Destination getMatchIpv6Destination(MatchInfo matchInfo) {
            return matchInfo instanceof MatchIpv6Destination ? (MatchIpv6Destination) matchInfo : new
                    MatchIpv6Destination(matchInfo.getStringMatchValues()[0]);
        }
    },

    @Deprecated
    ipv6_source {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Ipv6Src.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIpv6Source(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIpv6Source(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchIpv6Source getMatchIpv6Source(MatchInfo matchInfo) {
            return matchInfo instanceof MatchIpv6Source ? (MatchIpv6Source) matchInfo : new MatchIpv6Source(
                    new Ipv6Prefix(matchInfo.getStringMatchValues()[0]));
        }
    },

    @Deprecated
    arp_op {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return ArpOp.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchArpOp(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchArpOp(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchArpOp getMatchArpOp(MatchInfo matchInfo) {
            return matchInfo instanceof MatchArpOp ? (MatchArpOp) matchInfo : new MatchArpOp(
                    (int) matchInfo.getMatchValues()[0]);
        }
    },

    @Deprecated
    arp_tpa {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return ArpTpa.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchArpTpa(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchArpTpa(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchArpTpa getMatchArpTpa(MatchInfo matchInfo) {
            if (matchInfo instanceof MatchArpTpa) {
                return (MatchArpTpa) matchInfo;
            }
            if (matchInfo.getStringMatchValues() != null) {
                return new MatchArpTpa(matchInfo.getStringMatchValues()[0], matchInfo.getStringMatchValues()[1]);
            }
            return new MatchArpTpa(matchInfo.getMatchValues()[0], matchInfo.getMatchValues()[1]);
        }
    },

    @Deprecated
    arp_spa {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return ArpSpa.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchArpSpa(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchArpSpa(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchArpSpa getMatchArpSpa(MatchInfo matchInfo) {
            return matchInfo instanceof MatchArpSpa ? (MatchArpSpa) matchInfo : new MatchArpSpa(
                    matchInfo.getMatchValues()[0], matchInfo.getMatchValues()[1]);
        }
    },

    @Deprecated
    arp_tha {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return ArpTha.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchArpTha(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchArpTha(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchArpTha getMatchArpTha(MatchInfo matchInfo) {
            return matchInfo instanceof MatchArpTha ? (MatchArpTha) matchInfo : new MatchArpTha(
                    new MacAddress(matchInfo.getStringMatchValues()[0]));
        }
    },

    @Deprecated
    arp_sha {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return ArpSha.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchArpSha(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchArpSha(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchArpSha getMatchArpSha(MatchInfo matchInfo) {
            return matchInfo instanceof MatchArpSha ? (MatchArpSha) matchInfo : new MatchArpSha(
                    new MacAddress(matchInfo.getStringMatchValues()[0]));
        }
    },

    @Deprecated
    metadata {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Metadata.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchMetadata(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchMetadata(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchMetadata getMatchMetadata(MatchInfo matchInfo) {
            return matchInfo instanceof MatchMetadata ? (MatchMetadata) matchInfo : new MatchMetadata(
                    matchInfo.getBigMatchValues()[0], matchInfo.getBigMatchValues()[1]);
        }
    },

    @Deprecated
    mpls_label {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return MplsLabel.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchMplsLabel(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchMplsLabel(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchMplsLabel getMatchMplsLabel(MatchInfo matchInfo) {
            return matchInfo instanceof MatchMplsLabel ? (MatchMplsLabel) matchInfo : new MatchMplsLabel(
                    Long.parseLong(matchInfo.getStringMatchValues()[0]));
        }
    },

    @Deprecated
    pbb_isid {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return PbbIsid.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchPbbIsid(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchPbbIsid(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchPbbIsid getMatchPbbIsid(MatchInfo matchInfo) {
            return matchInfo instanceof MatchPbbIsid ? (MatchPbbIsid) matchInfo : new MatchPbbIsid(
                    matchInfo.getMatchValues()[0]);
        }
    },

    @Deprecated
    tcp_dst {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return TcpDst.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchTcpDestinationPort(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchTcpDestinationPort(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchTcpDestinationPort getMatchTcpDestinationPort(MatchInfo matchInfo) {
            return matchInfo instanceof MatchTcpDestinationPort ? (MatchTcpDestinationPort) matchInfo : new MatchTcpDestinationPort(
                    (int) matchInfo.getMatchValues()[0]);
        }
    },

    @Deprecated
    tcp_src {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return TcpSrc.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchTcpSourcePort(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchTcpSourcePort(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchTcpSourcePort getMatchTcpSourcePort(MatchInfo matchInfo) {
            return matchInfo instanceof MatchTcpSourcePort ? (MatchTcpSourcePort) matchInfo : new MatchTcpSourcePort(
                    (int) matchInfo.getMatchValues()[0]);
        }
    },

    @Deprecated
    tcp_flags {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return TcpFlags.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchTcpFlags(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>,
                Object> mapMatchBuilder) {
            getMatchTcpFlags(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchTcpFlags getMatchTcpFlags(MatchInfo matchInfo) {
            return matchInfo instanceof MatchTcpFlags ? (MatchTcpFlags) matchInfo : new MatchTcpFlags(
                    (int) matchInfo.getMatchValues()[0]);
        }
    },

    @Deprecated
    udp_dst {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return UdpDst.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchUdpDestinationPort(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchUdpDestinationPort(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchUdpDestinationPort getMatchUdpDestinationPort(MatchInfo matchInfo) {
            return matchInfo instanceof MatchUdpDestinationPort ? (MatchUdpDestinationPort) matchInfo : new
                    MatchUdpDestinationPort((int) matchInfo.getMatchValues()[0]);
        }
    },

    @Deprecated
    udp_src {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return UdpSrc.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchUdpSourcePort(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchUdpSourcePort(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchUdpSourcePort getMatchUdpSourcePort(MatchInfo matchInfo) {
            return matchInfo instanceof MatchUdpSourcePort ? (MatchUdpSourcePort) matchInfo : new MatchUdpSourcePort(
                    (int) matchInfo.getMatchValues()[0]);
        }
    },

    @Deprecated
    tunnel_id {
        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchTunnelId(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchTunnelId(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        @Override
        protected Class<? extends MatchField> getMatchType() {
            return TunnelId.class;
        }

        private MatchTunnelId getMatchTunnelId(MatchInfo matchInfo) {
            if (matchInfo instanceof MatchTunnelId) {
                return (MatchTunnelId) matchInfo;
            }
            if (matchInfo.getBigMatchValues().length > 1) {
                return new MatchTunnelId(matchInfo.getBigMatchValues()[0], matchInfo.getBigMatchValues()[1]);
            }
            return new MatchTunnelId(matchInfo.getBigMatchValues()[0]);
        }
    },

    @Deprecated
    vlan_vid {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return VlanVid.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchVlanVid(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchVlanVid(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchVlanVid getMatchVlanVid(MatchInfo matchInfo) {
            return matchInfo instanceof MatchVlanVid ? (MatchVlanVid) matchInfo : new MatchVlanVid(
                    (int) matchInfo.getMatchValues()[0]);
        }
    },

    @Deprecated
    icmp_v4 {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Icmpv4Type.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIcmpv4(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIcmpv4(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchIcmpv4 getMatchIcmpv4(MatchInfo matchInfo) {
            return matchInfo instanceof MatchIcmpv4 ? (MatchIcmpv4) matchInfo : new MatchIcmpv4(
                    (short) matchInfo.getMatchValues()[0], (short) matchInfo.getMatchValues()[1]);
        }
    },

    @Deprecated
    icmp_v6 {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Icmpv6Type.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIcmpv6(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIcmpv6(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchIcmpv6 getMatchIcmpv6(MatchInfo matchInfo) {
            return matchInfo instanceof MatchIcmpv6 ? (MatchIcmpv6) matchInfo : new MatchIcmpv6(
                    (short) matchInfo.getMatchValues()[0], (short) matchInfo.getMatchValues()[1]);
        }
    },

    @Deprecated
    ipv6_nd_target {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Ipv6NdTarget.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIpv6NdTarget(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getMatchIpv6NdTarget(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private MatchIpv6NdTarget getMatchIpv6NdTarget(MatchInfo matchInfo) {
            return matchInfo instanceof MatchIpv6NdTarget ? (MatchIpv6NdTarget) matchInfo : new MatchIpv6NdTarget(
                    new Ipv6Address(matchInfo.getStringMatchValues()[0]));
        }
    };


    public abstract void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder);

    public abstract void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo,
            Map<Class<?>, Object> mapMatchBuilder);

    protected abstract Class<? extends MatchField> getMatchType();

    protected boolean hasMatchFieldMask() {
        // Override this to return true
                return false;
    }
}
