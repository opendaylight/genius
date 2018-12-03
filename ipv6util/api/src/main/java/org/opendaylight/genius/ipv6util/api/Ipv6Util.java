/*
 * Copyright (c) 2018 Alten Calsoft Labs India Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.ipv6util.api;

import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.packet.rev160620.EthernetHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.packet.rev160620.Ipv6Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Ipv6Util {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv6Util.class);

    private Ipv6Util() {

    }

    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                buf.append(":");
            }
            short u8byte = (short) (bytes[i] & 0xff);
            String tmp = Integer.toHexString(u8byte);
            if (tmp.length() == 1) {
                buf.append("0");
            }
            buf.append(tmp);
        }
        return buf.toString();
    }

    public static byte[] bytesFromHexString(String values) {
        String target = "";
        if (values != null) {
            target = values;
        }
        String[] octets = target.split(":");

        byte[] ret = new byte[octets.length];
        for (int i = 0; i < octets.length; i++) {
            ret[i] = Integer.valueOf(octets[i], 16).byteValue();
        }
        return ret;
    }

    public static int calculateIcmpv6Checksum(byte[] packet, Ipv6Header ip6Hdr) {
        long checksum = getSummation(ip6Hdr.getSourceIpv6());
        checksum += getSummation(ip6Hdr.getDestinationIpv6());
        checksum = normalizeChecksum(checksum);

        checksum += ip6Hdr.getIpv6Length();
        checksum += ip6Hdr.getNextHeader();

        int icmp6Offset = Ipv6Constants.ICMPV6_OFFSET;
        long value = (packet[icmp6Offset] & 0xff) << 8 | packet[icmp6Offset + 1] & 0xff;
        checksum += value;
        checksum = normalizeChecksum(checksum);
        icmp6Offset += 2;

        // move to icmp6 payload skipping the checksum field
        icmp6Offset += 2;
        int length = packet.length - icmp6Offset;
        while (length > 1) {
            value = (packet[icmp6Offset] & 0xff) << 8 | packet[icmp6Offset + 1] & 0xff;
            checksum += value;
            checksum = normalizeChecksum(checksum);
            icmp6Offset += 2;
            length -= 2;
        }

        if (length > 0) {
            checksum += packet[icmp6Offset];
            checksum = normalizeChecksum(checksum);
        }

        int finalChecksum = (int) (~checksum & 0xffff);
        return finalChecksum;
    }

    public static boolean validateChecksum(byte[] packet, Ipv6Header ip6Hdr, int recvChecksum) {
        return calculateIcmpv6Checksum(packet, ip6Hdr) == recvChecksum;
    }

    private static long getSummation(Ipv6Address addr) {
        byte[] baddr = null;
        try {
            baddr = InetAddress.getByName(addr.getValue()).getAddress();
        } catch (UnknownHostException e) {
            LOG.error("getSummation: Failed to deserialize address {}", addr.getValue(), e);
            return 0;
        }

        long sum = 0;
        int len = 0;
        long value = 0;
        while (len < baddr.length) {
            value = (baddr[len] & 0xff) << 8 | baddr[len + 1] & 0xff;
            sum += value;
            sum = normalizeChecksum(sum);
            len += 2;
        }
        return sum;
    }

    private static long normalizeChecksum(long value) {
        if ((value & 0xffff0000) != 0) {
            value = value & 0xffff;
            value += 1;
        }
        return value;
    }

    public static byte[] convertEthernetHeaderToByte(EthernetHeader ethPdu) {
        byte[] data = new byte[16];
        Arrays.fill(data, (byte) 0);

        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.put(bytesFromHexString(ethPdu.getDestinationMac().getValue()));
        buf.put(bytesFromHexString(ethPdu.getSourceMac().getValue()));
        buf.putShort((short) ethPdu.getEthertype().intValue());
        return data;
    }

    public static byte[] convertIpv6HeaderToByte(Ipv6Header ip6Pdu) {
        byte[] data = new byte[128];
        Arrays.fill(data, (byte) 0);

        ByteBuffer buf = ByteBuffer.wrap(data);
        long flowLabel = (long) (ip6Pdu.getVersion() & 0x0f) << 28 | ip6Pdu.getFlowLabel() & 0x0fffffff;
        buf.putInt((int) flowLabel);
        buf.putShort((short) ip6Pdu.getIpv6Length().intValue());
        buf.put((byte) ip6Pdu.getNextHeader().shortValue());
        buf.put((byte) ip6Pdu.getHopLimit().shortValue());
        try {
            byte[] baddr = InetAddress.getByName(ip6Pdu.getSourceIpv6().getValue()).getAddress();
            buf.put(baddr);
            baddr = InetAddress.getByName(ip6Pdu.getDestinationIpv6().getValue()).getAddress();
            buf.put(baddr);
        } catch (UnknownHostException e) {
            LOG.error("convertIpv6HeaderToByte: Failed to serialize src, dest address", e);
        }
        return data;
    }

    public static Ipv6Address getIpv6LinkLocalAddressFromMac(MacAddress mac) {
        byte[] octets = bytesFromHexString(mac.getValue());

        /*
         * As per the RFC2373, steps involved to generate a LLA include 1. Convert the 48 bit MAC address to
         * 64 bit value by inserting 0xFFFE between OUI and NIC Specific part. 2. Invert the Universal/Local
         * flag in the OUI portion of the address. 3. Use the prefix "FE80::/10" along with the above 64 bit
         * Interface identifier to generate the IPv6 LLA.
         */

        StringBuilder interfaceID = new StringBuilder();
        short u8byte = (short) (octets[0] & 0xff);
        u8byte ^= 1 << 1;
        interfaceID.append(Integer.toHexString(0xFF & u8byte));
        interfaceID.append(StringUtils.leftPad(Integer.toHexString(0xFF & octets[1]), 2, "0"));
        interfaceID.append(":");
        interfaceID.append(Integer.toHexString(0xFF & octets[2]));
        interfaceID.append("ff:fe");
        interfaceID.append(StringUtils.leftPad(Integer.toHexString(0xFF & octets[3]), 2, "0"));
        interfaceID.append(":");
        interfaceID.append(Integer.toHexString(0xFF & octets[4]));
        interfaceID.append(StringUtils.leftPad(Integer.toHexString(0xFF & octets[5]), 2, "0"));

        // Return the address in its fully expanded format.
        Ipv6Address ipv6LLA =
                new Ipv6Address(InetAddresses.forString("fe80:0:0:0:" + interfaceID.toString()).getHostAddress());
        return ipv6LLA;
    }

    public static Ipv6Address getIpv6SolicitedNodeMcastAddress(Ipv6Address ipv6Address) {

        /*
         * According to RFC 4291, a Solicited Node Multicast Address is derived by adding the 24 lower order
         * bits with the Solicited Node multicast prefix (i.e., FF02::1:FF00:0/104). Example: For
         * IPv6Address of FE80::2AA:FF:FE28:9C5A, the corresponding solicited node multicast address would
         * be FF02::1:FF28:9C5A
         */

        byte[] octets;
        try {
            octets = InetAddress.getByName(ipv6Address.getValue()).getAddress();
        } catch (UnknownHostException e) {
            LOG.error("getIpv6SolicitedNodeMcastAddress: Failed to serialize ipv6Address ", e);
            return null;
        }

        // Return the address in its fully expanded format.
        Ipv6Address solictedV6Address =
                new Ipv6Address(InetAddresses
                        .forString("ff02::1:ff" + StringUtils.leftPad(Integer.toHexString(0xFF & octets[13]), 2, "0")
                                + ":" + StringUtils.leftPad(Integer.toHexString(0xFF & octets[14]), 2, "0")
                                + StringUtils.leftPad(Integer.toHexString(0xFF & octets[15]), 2, "0"))
                        .getHostAddress());

        return solictedV6Address;
    }

    public static MacAddress getIpv6MulticastMacAddress(Ipv6Address ipv6Address) {

        /*
         * According to RFC 2464, a Multicast MAC address is derived by concatenating 32 lower order bits of
         * IPv6 Multicast Address with the multicast prefix (i.e., 33:33). Example: For Multicast
         * IPv6Address of FF02::1:FF28:9C5A, the corresponding L2 multicast address would be 33:33:28:9C:5A
         */
        byte[] octets;
        try {
            octets = InetAddress.getByName(ipv6Address.getValue()).getAddress();
        } catch (UnknownHostException e) {
            LOG.error("getIpv6MulticastMacAddress: Failed to serialize ipv6Address ", e);
            return null;
        }

        String macAddress = "33:33:" + StringUtils.leftPad(Integer.toHexString(0xFF & octets[12]), 2, "0") + ":"
                + StringUtils.leftPad(Integer.toHexString(0xFF & octets[13]), 2, "0") + ":"
                + StringUtils.leftPad(Integer.toHexString(0xFF & octets[14]), 2, "0") + ":"
                + StringUtils.leftPad(Integer.toHexString(0xFF & octets[15]), 2, "0");

        return new MacAddress(macAddress);
    }

    /**
     * Gets the formatted IP address. <br>
     * e.g., <br>
     * 1. input = "1001:db8:0:2::1", return = "1001:db8:0:2:0:0:0:1" <br>
     * 2. input = "2607:f0d0:1002:51::4", return = "2607:f0d0:1002:51:0:0:0:4" <br>
     * 3. input = "1001:db8:0:2:0:0:0:1", return = "1001:db8:0:2:0:0:0:1" <br>
     * 4. input = "10.0.0.10", return = "10.0.0.10"
     *
     * @param ipAddress the IP address
     * @return the formatted IP address
     */
    public static String getFormattedIpAddress(IpAddress ipAddress) {
        Preconditions.checkNotNull(ipAddress, "ipAddress is null");
        if (ipAddress.getIpv4Address() != null) {
            // No formatting required for IPv4 address.
            return ipAddress.getIpv4Address().getValue();
        } else {
            // IPv6 case
            return getFormattedIpv6Address(ipAddress.getIpv6Address());
        }
    }

    /**
     * Gets the formatted IPv6 address. <br>
     * e.g., <br>
     * 1. input = "1001:db8:0:2::1", return = "1001:db8:0:2:0:0:0:1" <br>
     * 2. input = "2607:f0d0:1002:51::4", return = "2607:f0d0:1002:51:0:0:0:4" <br>
     * 3. input = "1001:db8:0:2:0:0:0:1", return = "1001:db8:0:2:0:0:0:1" <br>
     *
     * @param ipv6Address the IPv6 address
     * @return the formatted IPv6 address
     */
    public static String getFormattedIpv6Address(Ipv6Address ipv6Address) {
        try {
            return InetAddress.getByName(ipv6Address.getValue()).getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid ipv6Address=" + ipv6Address, e);
        }
    }
}
