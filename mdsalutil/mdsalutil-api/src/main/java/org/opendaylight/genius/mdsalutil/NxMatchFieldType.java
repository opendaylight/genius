/*
 * Copyright © 2016, 2017 RedHat Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.util.Map;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchCtState;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchCtZone;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchRegister;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchTcpDestinationPort;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchTcpSourcePort;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchTunnelDestinationIp;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchTunnelSourceIp;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchUdpDestinationPort;
import org.opendaylight.genius.mdsalutil.nxmatches.NxMatchUdpSourcePort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;

public enum NxMatchFieldType {
    @Deprecated
    ct_state {
        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchCtState(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, NxMatchInfo matchInfo,
                Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchCtState(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private NxMatchCtState getNxMatchCtState(NxMatchInfo matchInfo) {
            return matchInfo instanceof NxMatchCtState ? (NxMatchCtState) matchInfo
                    : new NxMatchCtState(matchInfo.getMatchValues()[0], matchInfo.getMatchValues()[1]);
        }
    },

    @Deprecated
    ct_zone {
        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchCtZone(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, NxMatchInfo matchInfo,
                Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchCtZone(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private NxMatchCtZone getNxMatchCtZone(NxMatchInfo matchInfo) {
            return matchInfo instanceof NxMatchCtZone ? (NxMatchCtZone) matchInfo
                    : new NxMatchCtZone((int) matchInfo.getMatchValues()[0]);
        }
    },

    @Deprecated
    nx_tcp_src_with_mask {
        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchTcpSourcePort(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, NxMatchInfo matchInfo,
                Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchTcpSourcePort(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private NxMatchTcpSourcePort getNxMatchTcpSourcePort(NxMatchInfo matchInfo) {
            return matchInfo instanceof NxMatchTcpSourcePort ? (NxMatchTcpSourcePort) matchInfo
                    : new NxMatchTcpSourcePort((int) matchInfo.getMatchValues()[0],
                            (int) matchInfo.getMatchValues()[1]);
        }
    },

    @Deprecated
    nx_tcp_dst_with_mask {
        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchTcpDestinationPort(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, NxMatchInfo matchInfo,
                Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchTcpDestinationPort(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private NxMatchTcpDestinationPort getNxMatchTcpDestinationPort(NxMatchInfo matchInfo) {
            return matchInfo instanceof NxMatchTcpDestinationPort ? (NxMatchTcpDestinationPort) matchInfo
                    : new NxMatchTcpDestinationPort((int) matchInfo.getMatchValues()[0],
                            (int) matchInfo.getMatchValues()[1]);
        }
    },

    @Deprecated
    nx_udp_src_with_mask {
        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchUdpSourcePort(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, NxMatchInfo matchInfo,
                Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchUdpSourcePort(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private NxMatchUdpSourcePort getNxMatchUdpSourcePort(NxMatchInfo matchInfo) {
            return matchInfo instanceof NxMatchUdpSourcePort ? (NxMatchUdpSourcePort) matchInfo
                    : new NxMatchUdpSourcePort((int) matchInfo.getMatchValues()[0],
                            (int) matchInfo.getMatchValues()[1]);
        }
    },

    @Deprecated
    nx_udp_dst_with_mask {
        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchUdpDestinationPort(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, NxMatchInfo matchInfo,
                Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchUdpDestinationPort(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private NxMatchUdpDestinationPort getNxMatchUdpDestinationPort(NxMatchInfo matchInfo) {
            return matchInfo instanceof NxMatchUdpDestinationPort ? (NxMatchUdpDestinationPort) matchInfo
                    : new NxMatchUdpDestinationPort((int) matchInfo.getMatchValues()[0],
                            (int) matchInfo.getMatchValues()[1]);
        }
    },

    @Deprecated
    tun_src_ip {
        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchTunnelSourceIp(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, NxMatchInfo matchInfo,
                             Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchTunnelSourceIp(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private NxMatchTunnelSourceIp getNxMatchTunnelSourceIp(NxMatchInfo matchInfo) {
            return matchInfo instanceof NxMatchTunnelSourceIp ? (NxMatchTunnelSourceIp) matchInfo
                    : new NxMatchTunnelSourceIp(matchInfo.getStringMatchValues()[0]);
        }
    },

    @Deprecated
    tun_dst_ip {
        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchTunnelDestinationIp(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, NxMatchInfo matchInfo,
                             Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchTunnelDestinationIp(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private NxMatchTunnelDestinationIp getNxMatchTunnelDestinationIp(NxMatchInfo matchInfo) {
            return matchInfo instanceof NxMatchTunnelDestinationIp ? (NxMatchTunnelDestinationIp) matchInfo
                    : new NxMatchTunnelDestinationIp(matchInfo.getStringMatchValues()[0]);
        }
    },

    @Deprecated
    nxm_reg_4 {
        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchRegister(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, NxMatchInfo matchInfo,
                             Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchRegister(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private NxMatchRegister getNxMatchRegister(NxMatchInfo matchInfo) {
            return matchInfo instanceof NxMatchRegister ? (NxMatchRegister) matchInfo
                    : new NxMatchRegister(NxmNxReg4.class, matchInfo.getMatchValues()[0]);
        }
    },

    @Deprecated
    nxm_reg_5 {
        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchRegister(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, NxMatchInfo matchInfo,
                             Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchRegister(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private NxMatchRegister getNxMatchRegister(NxMatchInfo matchInfo) {
            return matchInfo instanceof NxMatchRegister ? (NxMatchRegister) matchInfo
                    : new NxMatchRegister(NxmNxReg5.class, matchInfo.getMatchValues()[0]);
        }
    },

    @Deprecated
    nxm_reg_6 {
        @Override
        public void createInnerMatchBuilder(NxMatchInfo matchInfo, Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchRegister(matchInfo).createInnerMatchBuilder(mapMatchBuilder);
        }

        @Override
        public void setMatch(MatchBuilder matchBuilderInOut, NxMatchInfo matchInfo,
                             Map<Class<?>, Object> mapMatchBuilder) {
            getNxMatchRegister(matchInfo).setMatch(matchBuilderInOut, mapMatchBuilder);
        }

        private NxMatchRegister getNxMatchRegister(NxMatchInfo matchInfo) {
            return matchInfo instanceof NxMatchRegister ? (NxMatchRegister) matchInfo
                    : new NxMatchRegister(NxmNxReg6.class, matchInfo.getMatchValues()[0]);
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
    public abstract void setMatch(MatchBuilder matchBuilderInOut, NxMatchInfo matchInfo,
            Map<Class<?>, Object> mapMatchBuilder);
}
