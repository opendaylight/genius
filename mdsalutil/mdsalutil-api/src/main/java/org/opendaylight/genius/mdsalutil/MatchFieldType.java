/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.math.BigInteger;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpSourceHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpTargetHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.MetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.ProtocolMatchFieldsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TcpFlagsMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.protocol.match.fields.PbbBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.reg.grouping.NxmNxRegBuilder;
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

import static org.opendaylight.openflowplugin.openflow.md.core.sal.convertor.common.OrderComparator.build;

public enum MatchFieldType {
    eth_src {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return EthSrc.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            EthernetMatchBuilder ethernetMatchBuilder = (EthernetMatchBuilder) mapMatchBuilder
                    .get(EthernetMatchBuilder.class);

            if (ethernetMatchBuilder == null) {
                ethernetMatchBuilder = new EthernetMatchBuilder();
                mapMatchBuilder.put(EthernetMatchBuilder.class, ethernetMatchBuilder);
            }

            ethernetMatchBuilder.setEthernetSource(new EthernetSourceBuilder().setAddress(
                    new MacAddress(matchInfo.getStringMatchValues()[0])).build());
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            EthernetMatchBuilder ethernetMatchBuilder = (EthernetMatchBuilder) mapMatchBuilder
                    .remove(EthernetMatchBuilder.class);

            if (ethernetMatchBuilder != null) {
                matchBuilderInOut.setEthernetMatch(ethernetMatchBuilder.build());
            }
        }
    },

    eth_dst {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return EthDst.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            EthernetMatchBuilder ethernetMatchBuilder = (EthernetMatchBuilder) mapMatchBuilder
                    .get(EthernetMatchBuilder.class);

            if (ethernetMatchBuilder == null) {
                ethernetMatchBuilder = new EthernetMatchBuilder();
                mapMatchBuilder.put(EthernetMatchBuilder.class, ethernetMatchBuilder);
            }

            ethernetMatchBuilder.setEthernetDestination(new EthernetDestinationBuilder().setAddress(
                    new MacAddress(matchInfo.getStringMatchValues()[0])).build());
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            EthernetMatchBuilder ethernetMatchBuilder = (EthernetMatchBuilder) mapMatchBuilder
                    .remove(EthernetMatchBuilder.class);

            if (ethernetMatchBuilder != null) {
                matchBuilderInOut.setEthernetMatch(ethernetMatchBuilder.build());
            }
        }
    },

    eth_type {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return EthType.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            EthernetMatchBuilder ethernetMatchBuilder = (EthernetMatchBuilder) mapMatchBuilder
                    .get(EthernetMatchBuilder.class);

            if (ethernetMatchBuilder == null) {
                ethernetMatchBuilder = new EthernetMatchBuilder();
                mapMatchBuilder.put(EthernetMatchBuilder.class, ethernetMatchBuilder);
            }

            ethernetMatchBuilder.setEthernetType(new EthernetTypeBuilder().setType(
                    new EtherType(matchInfo.getMatchValues()[0])).build());
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            EthernetMatchBuilder ethernetMatchBuilder = (EthernetMatchBuilder) mapMatchBuilder
                    .remove(EthernetMatchBuilder.class);

            if (ethernetMatchBuilder != null) {
                matchBuilderInOut.setEthernetMatch(ethernetMatchBuilder.build());
            }
        }
    },

    in_port {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return InPort.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {


            String nodeConnectorId = "openflow:" + matchInfo.getBigMatchValues()[0] +
                    ':' + matchInfo.getBigMatchValues()[1];
            matchBuilderInOut.setInPort(new NodeConnectorId(nodeConnectorId));
        }
    },

    ip_proto {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return IpProto.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            IpMatchBuilder ipMatchBuilder = (IpMatchBuilder) mapMatchBuilder.get(IpMatchBuilder.class);

            if (ipMatchBuilder == null) {
                ipMatchBuilder = new IpMatchBuilder();
                mapMatchBuilder.put(IpMatchBuilder.class, ipMatchBuilder);
            }

            ipMatchBuilder.setIpProtocol(Short.valueOf((short) matchInfo.getMatchValues()[0])).build();
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            IpMatchBuilder ipMatchBuilder = (IpMatchBuilder) mapMatchBuilder.remove(IpMatchBuilder.class);

            if (ipMatchBuilder != null) {
                matchBuilderInOut.setIpMatch(ipMatchBuilder.build());
            }
        }
    },

    ipv4_dst {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Ipv4Dst.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Ipv4MatchBuilder ipv4MatchBuilder = (Ipv4MatchBuilder) mapMatchBuilder.get(Ipv4MatchBuilder.class);

            if (ipv4MatchBuilder == null) {
                ipv4MatchBuilder = new Ipv4MatchBuilder();
                mapMatchBuilder.put(Ipv4MatchBuilder.class, ipv4MatchBuilder);
            }

            long[] prefix = matchInfo.getMatchValues();
            ipv4MatchBuilder.setIpv4Destination(new Ipv4Prefix(MDSALUtil.longToIp(prefix[0], prefix[1]))).build();
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Ipv4MatchBuilder ipv4MatchBuilder = (Ipv4MatchBuilder) mapMatchBuilder.remove(Ipv4MatchBuilder.class);

            if (ipv4MatchBuilder != null) {
                matchBuilderInOut.setLayer3Match(ipv4MatchBuilder.build());
            }
        }
    },

    ipv4_src {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Ipv4Src.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Ipv4MatchBuilder ipv4MatchBuilder = (Ipv4MatchBuilder) mapMatchBuilder.get(Ipv4MatchBuilder.class);

            if (ipv4MatchBuilder == null) {
                ipv4MatchBuilder = new Ipv4MatchBuilder();
                mapMatchBuilder.put(Ipv4MatchBuilder.class, ipv4MatchBuilder);
            }

            long[] prefix = matchInfo.getMatchValues();
            ipv4MatchBuilder.setIpv4Source(new Ipv4Prefix(MDSALUtil.longToIp(prefix[0], prefix[1]))).build();
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Ipv4MatchBuilder ipv4MatchBuilder = (Ipv4MatchBuilder) mapMatchBuilder.remove(Ipv4MatchBuilder.class);

            if (ipv4MatchBuilder != null) {
                matchBuilderInOut.setLayer3Match(ipv4MatchBuilder.build());
            }
        }
    },

    ipv4_destination {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Ipv4Dst.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Ipv4MatchBuilder ipv4MatchBuilder = (Ipv4MatchBuilder) mapMatchBuilder.get(Ipv4MatchBuilder.class);

            if (ipv4MatchBuilder == null) {
                ipv4MatchBuilder = new Ipv4MatchBuilder();
                mapMatchBuilder.put(Ipv4MatchBuilder.class, ipv4MatchBuilder);
            }

            String[] prefix = matchInfo.getStringMatchValues();
            ipv4MatchBuilder.setIpv4Destination(new Ipv4Prefix(prefix[0] + "/" + prefix[1])).build();
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Ipv4MatchBuilder ipv4MatchBuilder = (Ipv4MatchBuilder) mapMatchBuilder.remove(Ipv4MatchBuilder.class);

            if (ipv4MatchBuilder != null) {
                matchBuilderInOut.setLayer3Match(ipv4MatchBuilder.build());
            }
        }
    },

    ipv4_source {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Ipv4Src.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Ipv4MatchBuilder ipv4MatchBuilder = (Ipv4MatchBuilder) mapMatchBuilder.get(Ipv4MatchBuilder.class);

            if (ipv4MatchBuilder == null) {
                ipv4MatchBuilder = new Ipv4MatchBuilder();
                mapMatchBuilder.put(Ipv4MatchBuilder.class, ipv4MatchBuilder);
            }

            String[] prefix = matchInfo.getStringMatchValues();
            ipv4MatchBuilder.setIpv4Source(new Ipv4Prefix(prefix[0] + "/" + prefix[1])).build();
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Ipv4MatchBuilder ipv4MatchBuilder = (Ipv4MatchBuilder) mapMatchBuilder.remove(Ipv4MatchBuilder.class);

            if (ipv4MatchBuilder != null) {
                matchBuilderInOut.setLayer3Match(ipv4MatchBuilder.build());
            }
        }
    },

    ipv6_destination {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Ipv6Dst.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Ipv6MatchBuilder ipv6MatchBuilder = (Ipv6MatchBuilder) mapMatchBuilder.get(Ipv6MatchBuilder.class);

            if (ipv6MatchBuilder == null) {
                ipv6MatchBuilder = new Ipv6MatchBuilder();
                mapMatchBuilder.put(Ipv6MatchBuilder.class, ipv6MatchBuilder);
            }
            ipv6MatchBuilder.setIpv6Destination(new Ipv6Prefix(matchInfo.getStringMatchValues()[0])).build();
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Ipv6MatchBuilder ipv6MatchBuilder = (Ipv6MatchBuilder) mapMatchBuilder.remove(Ipv6MatchBuilder.class);

            if (ipv6MatchBuilder != null) {
                matchBuilderInOut.setLayer3Match(ipv6MatchBuilder.build());
            }
        }
    },

    ipv6_source {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Ipv6Src.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Ipv6MatchBuilder ipv6MatchBuilder = (Ipv6MatchBuilder) mapMatchBuilder.get(Ipv6MatchBuilder.class);

            if (ipv6MatchBuilder == null) {
                ipv6MatchBuilder = new Ipv6MatchBuilder();
                mapMatchBuilder.put(Ipv6MatchBuilder.class, ipv6MatchBuilder);
            }

            ipv6MatchBuilder.setIpv6Source(new Ipv6Prefix(matchInfo.getStringMatchValues()[0])).build();
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Ipv6MatchBuilder ipv6MatchBuilder = (Ipv6MatchBuilder) mapMatchBuilder.remove(Ipv6MatchBuilder.class);

            if (ipv6MatchBuilder != null) {
                matchBuilderInOut.setLayer3Match(ipv6MatchBuilder.build());
            }
        }
    },


    arp_op {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return ArpOp.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            ArpMatchBuilder arpMatchBuilder = (ArpMatchBuilder) mapMatchBuilder.get(ArpMatchBuilder.class);

            if (arpMatchBuilder == null) {
                arpMatchBuilder = new ArpMatchBuilder();
                mapMatchBuilder.put(ArpMatchBuilder.class, arpMatchBuilder);
            }

            arpMatchBuilder.setArpOp(Integer.valueOf((int) matchInfo.getMatchValues()[0]));
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            ArpMatchBuilder arpMatchBuilder = (ArpMatchBuilder) mapMatchBuilder.remove(ArpMatchBuilder.class);

            if (arpMatchBuilder != null) {
                matchBuilderInOut.setLayer3Match(arpMatchBuilder.build());
            }
        }
    },

    arp_tpa {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return ArpTpa.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            ArpMatchBuilder arpMatchBuilder = (ArpMatchBuilder) mapMatchBuilder.get(ArpMatchBuilder.class);

            if (arpMatchBuilder == null) {
                arpMatchBuilder = new ArpMatchBuilder();
                mapMatchBuilder.put(ArpMatchBuilder.class, arpMatchBuilder);
            }
            String arpTpa;
            if (matchInfo.getStringMatchValues() != null) {
                arpTpa = matchInfo.getStringMatchValues()[0] + "/" + matchInfo.getStringMatchValues()[1];
            }else{
                long[] prefix = matchInfo.getMatchValues();
                arpTpa = NWUtil.longToIpv4(prefix[0], prefix[1]);
            }
            arpMatchBuilder.setArpTargetTransportAddress(new Ipv4Prefix(arpTpa));
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            ArpMatchBuilder arpMatchBuilder = (ArpMatchBuilder) mapMatchBuilder.remove(ArpMatchBuilder.class);

            if (arpMatchBuilder != null) {
                matchBuilderInOut.setLayer3Match(arpMatchBuilder.build());
            }
        }
    },

    arp_spa {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return ArpSpa.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            ArpMatchBuilder arpMatchBuilder = (ArpMatchBuilder) mapMatchBuilder.get(ArpMatchBuilder.class);

            if (arpMatchBuilder == null) {
                arpMatchBuilder = new ArpMatchBuilder();
                mapMatchBuilder.put(ArpMatchBuilder.class, arpMatchBuilder);
            }

            long[] prefix = matchInfo.getMatchValues();
            arpMatchBuilder.setArpSourceTransportAddress(new Ipv4Prefix(MDSALUtil.longToIp(prefix[0], prefix[1])));
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            ArpMatchBuilder arpMatchBuilder = (ArpMatchBuilder) mapMatchBuilder.remove(ArpMatchBuilder.class);

            if (arpMatchBuilder != null) {
                matchBuilderInOut.setLayer3Match(arpMatchBuilder.build());
            }
        }
    },

    arp_tha {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return ArpTha.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            ArpMatchBuilder arpMatchBuilder = (ArpMatchBuilder) mapMatchBuilder.get(ArpMatchBuilder.class);

            if (arpMatchBuilder == null) {
                arpMatchBuilder = new ArpMatchBuilder();
                mapMatchBuilder.put(ArpMatchBuilder.class, arpMatchBuilder);
            }

            ArpTargetHardwareAddressBuilder arpSrc = new ArpTargetHardwareAddressBuilder();
            arpSrc.setAddress(new MacAddress(matchInfo.getStringMatchValues()[0]));
            arpMatchBuilder.setArpTargetHardwareAddress(arpSrc.build());
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            ArpMatchBuilder arpMatchBuilder = (ArpMatchBuilder) mapMatchBuilder.remove(ArpMatchBuilder.class);

            if (arpMatchBuilder != null) {
                matchBuilderInOut.setLayer3Match(arpMatchBuilder.build());
            }
        }
    },

    arp_sha {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return ArpSha.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            ArpMatchBuilder arpMatchBuilder = (ArpMatchBuilder) mapMatchBuilder.get(ArpMatchBuilder.class);

            if (arpMatchBuilder == null) {
                arpMatchBuilder = new ArpMatchBuilder();
                mapMatchBuilder.put(ArpMatchBuilder.class, arpMatchBuilder);
            }

            ArpSourceHardwareAddressBuilder arpSrc = new ArpSourceHardwareAddressBuilder();
            arpSrc.setAddress(new MacAddress(matchInfo.getStringMatchValues()[0]));
            arpMatchBuilder.setArpSourceHardwareAddress(arpSrc.build());
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            ArpMatchBuilder arpMatchBuilder = (ArpMatchBuilder) mapMatchBuilder.remove(ArpMatchBuilder.class);

            if (arpMatchBuilder != null) {
                matchBuilderInOut.setLayer3Match(arpMatchBuilder.build());
            }
        }
    },

    metadata {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Metadata.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            MetadataBuilder metadataBuilder = (MetadataBuilder) mapMatchBuilder.get(MetadataBuilder.class);

            if (metadataBuilder == null) {
                metadataBuilder = new MetadataBuilder();
                mapMatchBuilder.put(MetadataBuilder.class, metadataBuilder);
            }

            BigInteger[] metadataValues = matchInfo.getBigMatchValues();
            metadataBuilder.setMetadata(metadataValues[0]).setMetadataMask(metadataValues[1]).build();
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            MetadataBuilder metadataBuilder = (MetadataBuilder) mapMatchBuilder.remove(MetadataBuilder.class);

            if (metadataBuilder != null) {
                matchBuilderInOut.setMetadata(metadataBuilder.build());
            }
        }
    },

    mpls_label {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return MplsLabel.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            ProtocolMatchFieldsBuilder protocolMatchFieldsBuilder = (ProtocolMatchFieldsBuilder) mapMatchBuilder
                    .get(ProtocolMatchFieldsBuilder.class);

            if (protocolMatchFieldsBuilder == null) {
                protocolMatchFieldsBuilder = new ProtocolMatchFieldsBuilder();
                mapMatchBuilder.put(ProtocolMatchFieldsBuilder.class, protocolMatchFieldsBuilder);
            }

            protocolMatchFieldsBuilder.setMplsLabel(Long.valueOf(matchInfo.getStringMatchValues()[0])).build();
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            ProtocolMatchFieldsBuilder protocolMatchFieldsBuilder = (ProtocolMatchFieldsBuilder) mapMatchBuilder
                    .remove(ProtocolMatchFieldsBuilder.class);

            if (protocolMatchFieldsBuilder != null) {
                matchBuilderInOut.setProtocolMatchFields(protocolMatchFieldsBuilder.build());
            }
        }
    },

    pbb_isid {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return PbbIsid.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            ProtocolMatchFieldsBuilder protocolMatchFieldsBuilder = (ProtocolMatchFieldsBuilder) mapMatchBuilder
                    .get(ProtocolMatchFieldsBuilder.class);

            if (protocolMatchFieldsBuilder == null) {
                protocolMatchFieldsBuilder = new ProtocolMatchFieldsBuilder();
                mapMatchBuilder.put(ProtocolMatchFieldsBuilder.class, protocolMatchFieldsBuilder);
            }

            protocolMatchFieldsBuilder.setPbb(new PbbBuilder().setPbbIsid(Long.valueOf(matchInfo.getMatchValues()[0]))
                    .build());
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            ProtocolMatchFieldsBuilder protocolMatchFieldsBuilder = (ProtocolMatchFieldsBuilder) mapMatchBuilder
                    .remove(ProtocolMatchFieldsBuilder.class);

            if (protocolMatchFieldsBuilder != null) {
                matchBuilderInOut.setProtocolMatchFields(protocolMatchFieldsBuilder.build());
            }
        }
    },

    tcp_dst {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return TcpDst.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            TcpMatchBuilder tcpMatchBuilder = (TcpMatchBuilder) mapMatchBuilder.get(TcpMatchBuilder.class);

            if (tcpMatchBuilder == null) {
                tcpMatchBuilder = new TcpMatchBuilder();
                mapMatchBuilder.put(TcpMatchBuilder.class, tcpMatchBuilder);
            }

            tcpMatchBuilder.setTcpDestinationPort(new PortNumber(Integer.valueOf((int) matchInfo.getMatchValues()[0])));
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            TcpMatchBuilder tcpMatchBuilder = (TcpMatchBuilder) mapMatchBuilder.remove(TcpMatchBuilder.class);

            if (tcpMatchBuilder != null) {
                matchBuilderInOut.setLayer4Match(tcpMatchBuilder.build());
            }
        }
    },

    tcp_src {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return TcpSrc.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            TcpMatchBuilder tcpMatchBuilder = (TcpMatchBuilder) mapMatchBuilder.get(TcpMatchBuilder.class);

            if (tcpMatchBuilder == null) {
                tcpMatchBuilder = new TcpMatchBuilder();
                mapMatchBuilder.put(TcpMatchBuilder.class, tcpMatchBuilder);
            }

            tcpMatchBuilder.setTcpSourcePort(new PortNumber(Integer.valueOf((int) matchInfo.getMatchValues()[0])));
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            TcpMatchBuilder tcpMatchBuilder = (TcpMatchBuilder) mapMatchBuilder.remove(TcpMatchBuilder.class);

            if (tcpMatchBuilder != null) {
                matchBuilderInOut.setLayer4Match(tcpMatchBuilder.build());
            }
        }
    },

    tcp_flags {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return TcpFlags.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            TcpFlagsMatchBuilder TcpFlagsMatchBuilder = (TcpFlagsMatchBuilder) mapMatchBuilder
                    .get(TcpFlagsMatchBuilder.class);
            if (matchInfo == null || matchInfo.getMatchValues() == null || matchInfo.getMatchValues().length == 0) {
                return;
            }

            if (TcpFlagsMatchBuilder == null) {
                TcpFlagsMatchBuilder = new TcpFlagsMatchBuilder();
                mapMatchBuilder.put(TcpFlagsMatchBuilder.class, TcpFlagsMatchBuilder);
            }
            TcpFlagsMatchBuilder.setTcpFlags((int) matchInfo.getMatchValues()[0]);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>,
                Object> mapMatchBuilder) {
            TcpFlagsMatchBuilder TcpFlagsMatchBuilder = (TcpFlagsMatchBuilder) mapMatchBuilder
                    .remove(TcpFlagsMatchBuilder.class);

            if (TcpFlagsMatchBuilder != null) {
                matchBuilderInOut.setTcpFlagsMatch(TcpFlagsMatchBuilder.build());
            }
        }
    },
    udp_dst {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return UdpDst.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            UdpMatchBuilder udpMatchBuilder = (UdpMatchBuilder) mapMatchBuilder.get(UdpMatchBuilder.class);

            if (udpMatchBuilder == null) {
                udpMatchBuilder = new UdpMatchBuilder();
                mapMatchBuilder.put(UdpMatchBuilder.class, udpMatchBuilder);
            }

            udpMatchBuilder.setUdpDestinationPort(new PortNumber(Integer.valueOf((int) matchInfo.getMatchValues()[0])));
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            UdpMatchBuilder udpMatchBuilder = (UdpMatchBuilder) mapMatchBuilder.remove(UdpMatchBuilder.class);

            if (udpMatchBuilder != null) {
                matchBuilderInOut.setLayer4Match(udpMatchBuilder.build());
            }
        }
    },

    udp_src {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return UdpSrc.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            UdpMatchBuilder udpMatchBuilder = (UdpMatchBuilder) mapMatchBuilder.get(UdpMatchBuilder.class);

            if (udpMatchBuilder == null) {
                udpMatchBuilder = new UdpMatchBuilder();
                mapMatchBuilder.put(UdpMatchBuilder.class, udpMatchBuilder);
            }

            udpMatchBuilder.setUdpSourcePort(new PortNumber(Integer.valueOf((int) matchInfo.getMatchValues()[0])));
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            UdpMatchBuilder udpMatchBuilder = (UdpMatchBuilder) mapMatchBuilder.remove(UdpMatchBuilder.class);

            if (udpMatchBuilder != null) {
                matchBuilderInOut.setLayer4Match(udpMatchBuilder.build());
            }
        }
    },
    tunnel_id {
        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            TunnelBuilder tunnelBuilder = (TunnelBuilder) mapMatchBuilder.get(TunnelBuilder.class);

            if (tunnelBuilder == null) {
                tunnelBuilder = new TunnelBuilder();
                mapMatchBuilder.put(TunnelBuilder.class, tunnelBuilder);
            }

            BigInteger[] tunnelIdValues = matchInfo.getBigMatchValues();
            tunnelBuilder.setTunnelId(tunnelIdValues[0]);
            if(tunnelIdValues.length > 1){
                tunnelBuilder.setTunnelMask(tunnelIdValues[1]);
            }
            tunnelBuilder.build();
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            TunnelBuilder tunnelBuilder = (TunnelBuilder) mapMatchBuilder.remove(TunnelBuilder.class);

            if (tunnelBuilder != null) {
                matchBuilderInOut.setTunnel(tunnelBuilder.build());
            }
        }

        @Override
        protected Class<? extends MatchField> getMatchType() {
            return TunnelId.class;
        }

    },

    vlan_vid {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return VlanVid.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            VlanMatchBuilder vlanMatchBuilder = (VlanMatchBuilder) mapMatchBuilder.get(VlanMatchBuilder.class);

            if (vlanMatchBuilder == null) {
                vlanMatchBuilder = new VlanMatchBuilder();
                mapMatchBuilder.put(VlanMatchBuilder.class, vlanMatchBuilder);
            }

            vlanMatchBuilder.setVlanId(new VlanIdBuilder()
            .setVlanId(new VlanId(Integer.valueOf((int) matchInfo.getMatchValues()[0])))
            .setVlanIdPresent(((int) matchInfo.getMatchValues()[0] == 0) ? Boolean.FALSE : Boolean.TRUE)
            .build());
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            VlanMatchBuilder vlanMatchBuilder = (VlanMatchBuilder) mapMatchBuilder.remove(VlanMatchBuilder.class);

            if (vlanMatchBuilder != null) {
                matchBuilderInOut.setVlanMatch(vlanMatchBuilder.build());
            }
        }
    },

    icmp_v4 {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Icmpv4Type.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Icmpv4MatchBuilder icmpv4MatchBuilder = (Icmpv4MatchBuilder) mapMatchBuilder.get(Icmpv4MatchBuilder.class);

            if (icmpv4MatchBuilder == null) {
                icmpv4MatchBuilder = new Icmpv4MatchBuilder();
                mapMatchBuilder.put(Icmpv4MatchBuilder.class, icmpv4MatchBuilder);
            }

            icmpv4MatchBuilder.setIcmpv4Type((short) matchInfo.getMatchValues()[0]);
            icmpv4MatchBuilder.setIcmpv4Code((short) matchInfo.getMatchValues()[1]);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Icmpv4MatchBuilder icmpv4MatchBuilder = (Icmpv4MatchBuilder) mapMatchBuilder.remove(Icmpv4MatchBuilder.class);

            if (icmpv4MatchBuilder != null) {
                matchBuilderInOut.setIcmpv4Match(icmpv4MatchBuilder.build());
            }
        }
    },

    icmp_v6 {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Icmpv6Type.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Icmpv6MatchBuilder icmpv6MatchBuilder = (Icmpv6MatchBuilder) mapMatchBuilder.get(Icmpv6MatchBuilder.class);

            if (icmpv6MatchBuilder == null) {
                icmpv6MatchBuilder = new Icmpv6MatchBuilder();
                mapMatchBuilder.put(Icmpv6MatchBuilder.class, icmpv6MatchBuilder);
            }

            icmpv6MatchBuilder.setIcmpv6Type((short) matchInfo.getMatchValues()[0]);
            icmpv6MatchBuilder.setIcmpv6Code((short) matchInfo.getMatchValues()[1]);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Icmpv6MatchBuilder icmpv6MatchBuilder = (Icmpv6MatchBuilder) mapMatchBuilder.remove(Icmpv6MatchBuilder.class);

            if (icmpv6MatchBuilder != null) {
                matchBuilderInOut.setIcmpv6Match(icmpv6MatchBuilder.build());
            }
        }
    },

    ipv6_nd_target {
        @Override
        protected Class<? extends MatchField> getMatchType() {
            return Ipv6NdTarget.class;
        }

        @Override
        public void createInnerMatchBuilder(MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Ipv6MatchBuilder ipv6MatchBuilder = (Ipv6MatchBuilder) mapMatchBuilder.get(Ipv6MatchBuilder.class);

            if (ipv6MatchBuilder == null) {
                ipv6MatchBuilder = new Ipv6MatchBuilder();
                mapMatchBuilder.put(Ipv6MatchBuilder.class, ipv6MatchBuilder);
            }
            ipv6MatchBuilder.setIpv6NdTarget(new Ipv6Address(matchInfo.getStringMatchValues()[0])).build();
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, MatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            Ipv6MatchBuilder ipv6MatchBuilder = (Ipv6MatchBuilder) mapMatchBuilder.remove(Ipv6MatchBuilder.class);

            if (ipv6MatchBuilder != null) {
                matchBuilderInOut.setLayer3Match(ipv6MatchBuilder.build());
            }
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
