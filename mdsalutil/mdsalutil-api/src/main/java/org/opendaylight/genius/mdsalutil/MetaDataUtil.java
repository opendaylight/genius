/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import org.opendaylight.yangtools.yang.common.Uint64;

public final class MetaDataUtil {
    public static final Uint64 METADATA_MASK_LPORT_TAG = Uint64.valueOf("0FFFFF0000000000", 16).intern();
    public static final Uint64 METADATA_MASK_SERVICE = Uint64.valueOf("000000FFFF000000", 16).intern();
    public static final Uint64 METADATA_MASK_SERVICE_INDEX = Uint64.valueOf("F000000000000000", 16).intern();
    public static final Uint64 METADATA_MASK_VRFID = Uint64.valueOf("0000000000FFFFFE", 16).intern();
    public static final Uint64 METADATA_MASK_REMOTE_ACL_TAG = Uint64.valueOf("0000000000FFFFF0", 16).intern();
    public static final Uint64 METADATA_MASK_POLICY_CLASSIFER_ID = Uint64.valueOf("0000000000FFFFFE", 16).intern();
    public static final Uint64 METADA_MASK_VALID_TUNNEL_ID_BIT_AND_TUNNEL_ID = Uint64.valueOf("08000000FFFFFF00", 16)
            .intern();
    public static final Uint64 METADATA_MASK_LABEL_ITM = Uint64.valueOf("40FFFFFF000000FF", 16).intern();
    public static final Uint64 METADA_MASK_TUNNEL_ID = Uint64.valueOf("00000000FFFFFF00", 16).intern();
    public static final Uint64 METADATA_MASK_SERVICE_SH_FLAG = Uint64.valueOf("000000FFFF000001", 16).intern();
    public static final Uint64 METADATA_MASK_LPORT_TAG_SH_FLAG = Uint64.valueOf("0FFFFF0000000001", 16).intern();
    public static final Uint64 METADATA_MASK_SH_FLAG = Uint64.valueOf("0000000000000001", 16).intern();
    public static final Uint64 METADATA_MASK_ELAN_SUBNET_ROUTE = Uint64.valueOf("000000FFFF000000", 16).intern();
    public static final Uint64 METADATA_MASK_SUBNET_ROUTE = Uint64.valueOf("000000FFFFFFFFFE", 16).intern();
    public static final Uint64 METADATA_MASK_ACL_CONNTRACK_CLASSIFIER_TYPE = Uint64.valueOf("0000000000000002", 16)
            .intern();
    public static final Uint64 METADATA_MASK_ACL_DROP = Uint64.valueOf("0000000000000004", 16).intern();
    public static final Uint64 REG6_MASK_REMOTE_DPN = Uint64.valueOf("0FFFFF0000000000", 16).intern();

    public static final int METADATA_LPORT_TAG_OFFSET = 40;
    public static final int METADATA_LPORT_TAG_BITLEN = 20;
    public static final int METADATA_ELAN_TAG_OFFSET = 24;
    public static final int METADATA_ELAN_TAG_BITLEN = 16;
    public static final int METADATA_VPN_ID_OFFSET = 1;
    public static final int METADATA_VPN_ID_BITLEN = 23;

    public static final int REG6_START_INDEX = 0;
    public static final int REG6_END_INDEX = 31;

    private static final Uint64 MASK_FOR_DISPATCHER = Uint64.valueOf("FFFFFFFFFFFFFFFE", 16).intern();
    private static final Uint64 MASK_FOR_EGRESS_DISPATCHER = Uint64.valueOf("000000FFFFFFFFFE", 16).intern();
    private static final long MASK_FOR_EGRESS_TUNNEL = REG6_MASK_REMOTE_DPN.longValue() >>> 32;
    private static final long ELAN_MASK_FOR_REG = METADATA_MASK_SERVICE.longValue() >>> 24;
    private static final long VPNID_MASK_FOR_REG = METADATA_MASK_VRFID.longValue() >> 1;
    private static final long LPORT_TAG_MASK = METADATA_MASK_LPORT_TAG.longValue() >>> 32;

    private MetaDataUtil() {

    }

    public static Uint64 getMetaDataForLPortDispatcher(int lportTag, short serviceIndex) {
        return Uint64.fromLongBits(getServiceIndexMetaData(serviceIndex).longValue()
            | getLportTagMetaData(lportTag).longValue());
    }

    public static Uint64 getMetaDataForLPortDispatcher(int lportTag, short serviceIndex, Uint64 serviceMetaData) {
        return getMetaDataForLPortDispatcher(lportTag, serviceIndex, serviceMetaData, false);
    }

    public static Uint64 getMetaDataForLPortDispatcher(int lportTag, short serviceIndex,
                                                       Uint64 serviceMetaData, boolean isSHFlagSet) {
        return Uint64.fromLongBits(getServiceIndexMetaData(serviceIndex).longValue()
            | getLportTagMetaData(lportTag).longValue()
            | serviceMetaData.longValue()
            | (isSHFlagSet ? 1 : 0));
    }

    public static Uint64 getPolicyClassifierMetaData(long classifier) {
        return Uint64.valueOf((METADATA_MASK_POLICY_CLASSIFER_ID.longValue() & classifier) << 1);
    }

    public static Uint64 getServiceIndexMetaData(int serviceIndex) {
        return Uint64.fromLongBits((serviceIndex & 0xFL) << 60);
    }

    public static Uint64 getLportTagMetaData(int lportTag) {
        return Uint64.fromLongBits((lportTag & 0xFFFFFL) << METADATA_LPORT_TAG_OFFSET);
    }

    public static Uint64 getMetaDataMaskForLPortDispatcher() {
        return getMetaDataMaskForLPortDispatcher(METADATA_MASK_LPORT_TAG);
    }

    public static Uint64 getMetaDataMaskForLPortDispatcher(Uint64 metadataMaskForLPortTag) {
        return Uint64.fromLongBits(METADATA_MASK_SERVICE_INDEX.longValue() | metadataMaskForLPortTag.longValue());
    }

    public static Uint64 getMetaDataMaskForLPortDispatcher(Uint64 metadataMaskForServiceIndex,
            Uint64 metadataMaskForLPortTag, Uint64 metadataMaskForService) {
        return Uint64.fromLongBits(metadataMaskForServiceIndex.longValue() | metadataMaskForLPortTag.longValue()
            | metadataMaskForService.longValue());
    }

    public static Uint64 getMetadataLPort(int portTag) {
        return Uint64.valueOf((portTag & 0xFFFFL) << METADATA_LPORT_TAG_OFFSET);
    }

    public static Uint64 getLportFromMetadata(Uint64 metadata) {
        return Uint64.fromLongBits(
            (metadata.longValue() & METADATA_MASK_LPORT_TAG.longValue()) >>> METADATA_LPORT_TAG_OFFSET);
    }

    public static int getElanTagFromMetadata(Uint64 metadata) {
        return (int) ((metadata.longValue() & MetaDataUtil.METADATA_MASK_SERVICE.longValue()) >>> 24);
    }

    public static long getPolicyClassifierFromMetadata(Uint64 metadata) {
        return (metadata.longValue() & METADATA_MASK_POLICY_CLASSIFER_ID.longValue()) >>> 1;
    }

    public static Uint64 getElanTagMetadata(long elanTag) {
        return Uint64.fromLongBits(elanTag << 24);
    }

    public static int getServiceTagFromMetadata(Uint64 metadata) {
        return (int) ((metadata.longValue() & MetaDataUtil.METADATA_MASK_SERVICE_INDEX.longValue()) >>> 60);
    }

    /**
     * For the tunnel id with VNI and valid-vni-flag set, the most significant byte
     * should have 08. So, shifting 08 to 7 bytes (56 bits) and the result is OR-ed with
     * VNI being shifted to 1 byte.
     */
    public static Uint64 getTunnelIdWithValidVniBitAndVniSet(int vni) {
        return Uint64.valueOf(8L << 56 | vni << 8);
    }

    public static long getNatRouterIdFromMetadata(Uint64 metadata) {
        return getVpnIdFromMetadata(metadata);
    }

    /**
     * Gets the ACL conntrack classifier type from meta data.<br>
     * Second bit in metadata is used for this purpose.<br>
     *
     * <p>
     * Conntrack supported traffic is identified by value 0 (0000 in binary)
     * i.e., 0x0/0x2<br>
     * Non-conntrack supported traffic is identified by value 2 (0010 in binary)
     * i.e., 0x2/0x2
     *
     * @param conntrackClassifierType the conntrack classifier flag
     * @return the acl conntrack classifier flag from meta data
     */
    public static Uint64 getAclConntrackClassifierTypeFromMetaData(Uint64 conntrackClassifierType) {
        return Uint64.fromLongBits(
            (METADATA_MASK_ACL_CONNTRACK_CLASSIFIER_TYPE.longValue() & conntrackClassifierType.longValue()) << 1);
    }

    public static Uint64 getAclDropMetaData(Uint64 dropFlag) {
        return Uint64.fromLongBits((METADATA_MASK_ACL_DROP.longValue() & dropFlag.longValue()) << 2);
    }

    public static Uint64 getVpnIdMetadata(long vrfId) {
        return Uint64.valueOf(METADATA_MASK_VRFID.longValue() & vrfId << 1);
    }

    public static long getVpnIdFromMetadata(Uint64 metadata) {
        return (metadata.longValue() & METADATA_MASK_VRFID.longValue()) >>> 1;
    }

    public static Uint64 getWriteMetaDataMaskForDispatcherTable() {
        return MASK_FOR_DISPATCHER;
    }

    public static Uint64 getWriteMetaDataMaskForEgressDispatcherTable() {
        return MASK_FOR_EGRESS_DISPATCHER;
    }

    public static Uint64 getLportTagForReg6(int lportTag) {
        return Uint64.valueOf((lportTag & 0xFFFFF) << 8);
    }

    public static Uint64 getServiceIndexForReg6(int serviceIndex) {
        return Uint64.valueOf((0xFL & serviceIndex) << 28);
    }

    public static Uint64 getInterfaceTypeForReg6(int tunnelType) {
        return Uint64.valueOf((0xF & tunnelType) << 4);
    }

    public static long getReg6ValueForLPortDispatcher(int lportTag, short serviceIndex) {
        return getServiceIndexForReg6(serviceIndex).longValue() | getLportTagForReg6(lportTag).longValue();
    }

    /** Utility to fetch the register value for lport dispatcher table.
     * Register6 used for service binding will have first 4 bits of service-index, next 20 bits for lportTag,
     * and next 4 bits for interface-type
     */
    public static long getReg6ValueForLPortDispatcher(int lportTag, short serviceIndex, short interfaceType) {
        return getServiceIndexForReg6(serviceIndex).longValue() | getLportTagForReg6(lportTag).longValue()
                | getInterfaceTypeForReg6(interfaceType).longValue();
    }

    public static long getRemoteDpnMetadatForEgressTunnelTable(long remoteDpnId) {
        return (remoteDpnId & 0xFFFFFF) << 8;
    }

    public static long getRemoteDpnMaskForEgressTunnelTable() {
        return MASK_FOR_EGRESS_TUNNEL;
    }

    public static long getLportTagMaskForReg6() {
        return LPORT_TAG_MASK;
    }

    public static long getElanMaskForReg() {
        return ELAN_MASK_FOR_REG;
    }

    public static long getVpnIdMaskForReg() {
        return VPNID_MASK_FOR_REG;
    }

    public static Uint64 mergeMetadataValues(Uint64 metadata, Uint64 metadata2) {
        return Uint64.fromLongBits(metadata.longValue() | metadata2.longValue());
    }

    public static Uint64 mergeMetadataMask(Uint64 mask, Uint64 mask2) {
        return Uint64.fromLongBits(mask.longValue() | mask2.longValue());
    }
}
