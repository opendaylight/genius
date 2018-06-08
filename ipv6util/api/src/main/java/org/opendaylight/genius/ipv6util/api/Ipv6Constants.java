/*
 * Copyright (c) 2018 Alten Calsoft Labs India Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.ipv6util.api;

public interface Ipv6Constants {

    short IPV6_VERSION = 6;

    int ETHTYPE_START = 96;
    int ONE_BYTE = 8;
    int TWO_BYTES = 16;
    int IP_V6_HDR_START = 112;
    int IP_V6_NEXT_HDR = 48;
    int ICMPV6_HDR_START = 432;

    int ICMPV6_RA_LENGTH_WO_OPTIONS = 16;
    int ICMPV6_OPTION_SOURCE_LLA_LENGTH = 8;
    int ICMPV6_OPTION_PREFIX_LENGTH = 32;

    int ICMPV6_NA_LENGTH_WO_OPTIONS = 24;

    int IPV6_DEFAULT_HOP_LIMIT = 64;
    int IPV6_ROUTER_LIFETIME = 4500;
    int IPV6_RA_VALID_LIFETIME = 2592000;
    int IPV6_RA_PREFERRED_LIFETIME = 604800;
    int IPV6_RA_REACHABLE_TIME = 120000;

    short ICMP_V6_MAX_HOP_LIMIT = 255;
    int ICMPV6_OFFSET = 54;

    short ICMP_V6_OPTION_SOURCE_LLA = 1;
    short ICMP_V6_OPTION_TARGET_LLA = 2;

    String ALL_NODES_MCAST_MAC = "33:33:00:00:00:01";
    String ALL_ROUTERS_MCAST_MAC = "33:33:00:00:00:02";

    String ALL_NODES_MCAST_ADDRESS = "FF02::1";
    String ALL_ROUTERS_MCAST_ADDRESS = "FF02::2";

    String IPV6_LINK_LOCAL_PREFIX = "FE80::/10";

    enum Ipv6RouterAdvertisementType {
        UNSOLICITED_ADVERTISEMENT,
        SOLICITED_ADVERTISEMENT,
        CEASE_ADVERTISEMENT
    }
}
