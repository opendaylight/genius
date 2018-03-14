/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import com.google.common.base.Optional;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Ints;
import com.google.common.primitives.UnsignedBytes;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NWUtil {

    private static final Logger LOG = LoggerFactory.getLogger(NWUtil.class);

    private NWUtil() { }

    public static  long convertInetAddressToLong(InetAddress address) {
        byte[] ipAddressRaw = address.getAddress();
        return ((ipAddressRaw[0] & 0xFF) << 3 * 8)
                + ((ipAddressRaw[1] & 0xFF) << 2 * 8)
                + ((ipAddressRaw[2] & 0xFF) << 1 * 8)
                + (ipAddressRaw[3] & 0xFF)
                & 0xffffffffL;
    }

    /**
    * Converts IPv4 Address in long to String.
    * {@link #longToIpv4(long, long)} fixes the issue of {@link MDSALUtil#longToIp(long, long)}
    * not handling IP address greater than byte.
    *
    * @param ipAddress IP Address to be converted to String
    * @param mask Network mask to be appended
    * @return IP Address converted to String
    */
    public static String longToIpv4(final long ipAddress, final long mask) {
        final StringBuilder builder = new StringBuilder(20);
        final Inet4Address address = InetAddresses.fromInteger((int)ipAddress);
        builder.append(address.toString());
        if (mask != 0) {
            builder.append("/").append(mask);
        }
        return builder.toString();
    }

    public static int ipAddressToInt(String ipAddr) throws UnknownHostException {
        InetAddress subnetAddress = InetAddress.getByName(ipAddr);
        return Ints.fromByteArray(subnetAddress.getAddress());
    }

    public static byte[] parseMacAddress(String macAddress) {
        byte cur;

        String[] addressPart = macAddress.split(NwConstants.MACADDR_SEP);
        int size = addressPart.length;

        byte[] part = new byte[size];
        for (int i = 0; i < size; i++) {
            cur = UnsignedBytes.parseUnsignedByte(addressPart[i], 16);
            part[i] = cur;
        }

        return part;
    }

    public static String toStringIpAddress(byte[] ipAddress) {
        String ip = "";
        if (ipAddress == null) {
            return ip;
        }

        try {
            ip = InetAddress.getByAddress(ipAddress).getHostAddress();
        } catch (UnknownHostException e) {
            final String msg = "UnknownHostException while converting ip to string";
            LOG.error(msg, e);
            throw new RuntimeException(msg, e);
        }

        return ip;
    }

    /**
     * Accepts a MAC address and returns the corresponding long, where the MAC
     * bytes are set on the lower order bytes of the long.
     *
     * @return a long containing the mac address bytes
     */
    public static long macByteToLong(byte[] macAddress) {
        long mac = 0;
        for (int i = 0; i < 6; i++) {
            long temp = (macAddress[i] & 0xffL) << (5 - i) * 8;
            mac |= temp;
        }
        return mac;
    }

    /**
     * Accepts a MAC address of the form 00:aa:11:bb:22:cc, case does not
     * matter, and returns the corresponding long, where the MAC bytes are set
     * on the lower order bytes of the long.
     *
     * @param macAddress
     *            in String format
     * @return a long containing the mac address bytes
     */
    public static long macToLong(MacAddress macAddress) {
        return macByteToLong(parseMacAddress(macAddress.getValue()));
    }

    public static String toStringMacAddress(byte[] macAddress) {
        if (macAddress == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder(18);

        for (byte macAddres : macAddress) {
            String tmp = UnsignedBytes.toString(macAddres, 16).toUpperCase(Locale.getDefault());
            if (tmp.length() == 1 || macAddres == (byte) 0) {
                sb.append("0");
            }
            sb.append(tmp);
            sb.append(NwConstants.MACADDR_SEP);
        }

        sb.setLength(17);
        return sb.toString();
    }

    /**
     * Returns the ids of the currently operative DPNs.
     */
    public static List<BigInteger> getOperativeDPNs(DataBroker dataBroker) {
        List<BigInteger> result = new LinkedList<>();
        InstanceIdentifier<Nodes> nodesInstanceIdentifier = InstanceIdentifier.builder(Nodes.class).build();
        Optional<Nodes> nodesOptional = MDSALUtil.read(dataBroker, LogicalDatastoreType.OPERATIONAL,
                                                       nodesInstanceIdentifier);
        if (!nodesOptional.isPresent()) {
            return result;
        }
        Nodes nodes = nodesOptional.get();
        List<Node> nodeList = nodes.getNode();
        for (Node node : nodeList) {
            NodeId nodeId = node.getId();
            if (nodeId != null) {
                BigInteger dpnId = MDSALUtil.getDpnIdFromNodeName(nodeId);
                result.add(dpnId);
            }
        }
        return result;
    }

    /**
     * Utility API to check if the supplied ipAddress is IPv4 Address.
     *
     * @param ipAddress string-ified text of a possible IP address
     * @return true if ipAddress is an IPv4Address and false otherwise
     */
    public static Boolean isIpv4Address(String ipAddress) {
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            return address instanceof Inet4Address;
        } catch (UnknownHostException e) {
            final String msg = "UnknownHostException while checking whether '" + ipAddress + "' is an IPv4 address";
            // Double LOG & re-throw anti pattern usually bad, exceptionally OK here, just to be sure this is seen:
            LOG.error("UnknownHostException while checking whether {} is an IPv4 address",ipAddress,e);
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Checks if a given ipAddress belongs to a specific subnet.
     *
     * @param ipAddress The Ip Address to check
     * @param subnetCidr Subnet represented as string with CIDR
     * @return true if the ipAddress belongs to the Subnet, or false if it
     *     doesnt belong or the IpAddress string cannot be converted to an
     *     InetAddress
     */
    public static boolean isIpInSubnet(int ipAddress, String subnetCidr) {
        String[] subSplit = subnetCidr.split("/");
        if (subSplit.length < 2) {
            return false;
        }

        String subnetStr = subSplit[0];
        int prefixLength = Integer.parseInt(subSplit[1]);
        try {
            int subnet = ipAddressToInt(subnetStr);
            int mask = -1 << 32 - prefixLength;

            return (subnet & mask) == (ipAddress & mask);

        } catch (UnknownHostException ex) {
            LOG.error("Subnet string {} not convertible to InetAdddress ", subnetStr, ex);
            return false;
        }
    }

    /**
     * Utility API that returns the corresponding ipPrefix based on the ipAddress.
     *
     * @param ipAddress string text of an IP address
     * @return ipAddress appended with a "/32" prefix (if IPv4), else "/128" prefix (for IPv6)
     */
    public static String toIpPrefix(String ipAddress) {
        return isIpv4Address(ipAddress) ? ipAddress + NwConstants.IPV4PREFIX
                                        : ipAddress + NwConstants.IPV6PREFIX;
    }
}
