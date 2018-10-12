/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.math.BigInteger;

public final class MetaDataUtil {
    public static final BigInteger METADATA_MASK_LPORT_TAG =     new BigInteger("0FFFFF0000000000", 16);
    public static final BigInteger METADATA_MASK_SERVICE =       new BigInteger("000000FFFF000000", 16);
    public static final BigInteger METADATA_MASK_SERVICE_INDEX = new BigInteger("F000000000000000", 16);
    public static final BigInteger METADATA_MASK_VRFID =         new BigInteger("0000000000FFFFFE", 16);
    public static final BigInteger METADATA_MASK_REMOTE_ACL_TAG = new BigInteger("0000000000FFFFF0", 16);
    public static final BigInteger METADATA_MASK_POLICY_CLASSIFER_ID = new BigInteger("0000000000FFFFFE", 16);
    public static final BigInteger METADA_MASK_VALID_TUNNEL_ID_BIT_AND_TUNNEL_ID
        = new BigInteger("08000000FFFFFF00", 16);
    public static final BigInteger METADATA_MASK_LABEL_ITM =     new BigInteger("40FFFFFF000000FF", 16);
    public static final BigInteger METADA_MASK_TUNNEL_ID =       new BigInteger("00000000FFFFFF00", 16);
    public static final BigInteger METADATA_MASK_SERVICE_SH_FLAG = new BigInteger("000000FFFF000001", 16);
    public static final BigInteger METADATA_MASK_LPORT_TAG_SH_FLAG =     new BigInteger("0FFFFF0000000001", 16);
    public static final BigInteger METADATA_MASK_SH_FLAG = new BigInteger("0000000000000001", 16);
    public static final BigInteger METADATA_MASK_ELAN_SUBNET_ROUTE =    new BigInteger("000000FFFF000000", 16);
    public static final BigInteger METADATA_MASK_SUBNET_ROUTE =         new BigInteger("000000FFFFFFFFFE", 16);
    public static final BigInteger METADATA_MASK_ACL_CONNTRACK_CLASSIFIER_TYPE = new BigInteger("0000000000000002", 16);
    public static final BigInteger METADATA_MASK_ACL_DROP = new BigInteger("0000000000000004", 16);
    public static final BigInteger REG6_MASK_REMOTE_DPN =     new BigInteger("0FFFFF0000000000", 16);

    public static final int METADATA_LPORT_TAG_OFFSET = 40;
    public static final int METADATA_LPORT_TAG_BITLEN = 20;
    public static final int METADATA_ELAN_TAG_OFFSET = 24;
    public static final int METADATA_ELAN_TAG_BITLEN = 16;
    public static final int METADATA_VPN_ID_OFFSET = 1;
    public static final int METADATA_VPN_ID_BITLEN = 23;

    public static final int REG6_START_INDEX = 0;
    public static final int REG6_END_INDEX = 31;

    private MetaDataUtil() { }

    public static BigInteger getMetaDataForLPortDispatcher(int lportTag, short serviceIndex) {
        return getServiceIndexMetaData(serviceIndex).or(getLportTagMetaData(lportTag));
    }

    public static BigInteger getMetaDataForLPortDispatcher(int lportTag, short serviceIndex,
            BigInteger serviceMetaData) {
        return getMetaDataForLPortDispatcher(lportTag, serviceIndex, serviceMetaData, false);
    }

    public static BigInteger getMetaDataForLPortDispatcher(int lportTag, short serviceIndex,
                                                           BigInteger serviceMetaData, boolean isSHFlagSet) {
        int shBit = isSHFlagSet ? 1 : 0;
        return getServiceIndexMetaData(serviceIndex).or(getLportTagMetaData(lportTag)).or(serviceMetaData)
                .or(BigInteger.valueOf(shBit));
    }

    public static BigInteger getPolicyClassifierMetaData(long classifier) {
        return METADATA_MASK_POLICY_CLASSIFER_ID.and(BigInteger.valueOf(classifier).shiftLeft(1));
    }

    public static BigInteger getServiceIndexMetaData(int serviceIndex) {
        return new BigInteger("F", 16).and(BigInteger.valueOf(serviceIndex)).shiftLeft(60);
    }

    public static BigInteger getLportTagMetaData(int lportTag) {
        return new BigInteger("FFFFF", 16).and(BigInteger.valueOf(lportTag)).shiftLeft(METADATA_LPORT_TAG_OFFSET);
    }

    public static BigInteger getMetaDataMaskForLPortDispatcher() {
        return getMetaDataMaskForLPortDispatcher(METADATA_MASK_LPORT_TAG);
    }

    public static BigInteger getMetaDataMaskForLPortDispatcher(BigInteger metadataMaskForLPortTag) {
        return METADATA_MASK_SERVICE_INDEX.or(metadataMaskForLPortTag);
    }

    public static BigInteger getMetaDataMaskForLPortDispatcher(BigInteger metadataMaskForServiceIndex,
            BigInteger metadataMaskForLPortTag, BigInteger metadataMaskForService) {
        return metadataMaskForServiceIndex.or(metadataMaskForLPortTag).or(metadataMaskForService);
    }

    public static BigInteger getMetadataLPort(int portTag) {
        return new BigInteger("FFFF", 16).and(BigInteger.valueOf(portTag)).shiftLeft(METADATA_LPORT_TAG_OFFSET);
    }

    public static BigInteger getLportFromMetadata(BigInteger metadata) {
        return metadata.and(METADATA_MASK_LPORT_TAG).shiftRight(METADATA_LPORT_TAG_OFFSET);
    }

    public static int getElanTagFromMetadata(BigInteger metadata) {
        return metadata.and(MetaDataUtil.METADATA_MASK_SERVICE).shiftRight(24).intValue();
    }

    public static long getPolicyClassifierFromMetadata(BigInteger metadata) {
        return metadata.and(METADATA_MASK_POLICY_CLASSIFER_ID).shiftRight(1).longValue();
    }

    public static BigInteger getElanTagMetadata(long elanTag) {
        return BigInteger.valueOf(elanTag).shiftLeft(24);
    }

    public static int getServiceTagFromMetadata(BigInteger metadata) {
        return metadata.and(MetaDataUtil.METADATA_MASK_SERVICE_INDEX)
                .shiftRight(60).intValue();
    }

    /**
     * For the tunnel id with VNI and valid-vni-flag set, the most significant byte
     * should have 08. So, shifting 08 to 7 bytes (56 bits) and the result is OR-ed with
     * VNI being shifted to 1 byte.
     */
    public static BigInteger getTunnelIdWithValidVniBitAndVniSet(int vni) {
        return BigInteger.valueOf(0X08).shiftLeft(56).or(BigInteger.valueOf(vni).shiftLeft(8));
    }

    public static long getNatRouterIdFromMetadata(BigInteger metadata) {
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
    public static BigInteger getAclConntrackClassifierTypeFromMetaData(BigInteger conntrackClassifierType) {
        return METADATA_MASK_ACL_CONNTRACK_CLASSIFIER_TYPE.and(conntrackClassifierType.shiftLeft(1));
    }

    public static BigInteger getAclDropMetaData(BigInteger dropFlag) {
        return METADATA_MASK_ACL_DROP.and(dropFlag.shiftLeft(2));
    }

    public static BigInteger getVpnIdMetadata(long vrfId) {
        return METADATA_MASK_VRFID.and(BigInteger.valueOf(vrfId).shiftLeft(1));
    }

    public static long getVpnIdFromMetadata(BigInteger metadata) {
        return metadata.and(METADATA_MASK_VRFID).shiftRight(1).longValue();
    }

    public static BigInteger getWriteMetaDataMaskForDispatcherTable() {
        return new BigInteger("FFFFFFFFFFFFFFFE", 16);
    }

    public static BigInteger getWriteMetaDataMaskForEgressDispatcherTable() {
        return new BigInteger("000000FFFFFFFFFE", 16);
    }

    public static BigInteger getLportTagForReg6(int lportTag) {
        return new BigInteger("FFFFF", 16).and(BigInteger.valueOf(lportTag)).shiftLeft(8);
    }

    public static BigInteger getServiceIndexForReg6(int serviceIndex) {
        return new BigInteger("F", 16).and(BigInteger.valueOf(serviceIndex)).shiftLeft(28);
    }

    public static BigInteger getInterfaceTypeForReg6(int tunnelType) {
        return new BigInteger("F", 16).and(BigInteger.valueOf(tunnelType)).shiftLeft(4);
    }

    public static long getReg6ValueForLPortDispatcher(int lportTag, short serviceIndex) {
        return getServiceIndexForReg6(serviceIndex).or(getLportTagForReg6(lportTag)).longValue();
    }

    /** Utility to fetch the register value for lport dispatcher table.
     * Register6 used for service binding will have first 4 bits of service-index, next 20 bits for lportTag,
     * and next 4 bits for interface-type
     */
    public static long getReg6ValueForLPortDispatcher(int lportTag, short serviceIndex, short interfaceType) {
        return getServiceIndexForReg6(serviceIndex).or(getLportTagForReg6(lportTag)
                .or(getInterfaceTypeForReg6(interfaceType))).longValue();
    }

    public static long getRemoteDpnMetadatForEgressTunnelTable(long remoteDpnId) {
        return new BigInteger("FFFFF", 16).and(BigInteger.valueOf(remoteDpnId)).shiftLeft(8).longValue();
    }

    public static long getRemoteDpnMaskForEgressTunnelTable() {
        return REG6_MASK_REMOTE_DPN.shiftRight(32).longValue();
    }

    public static long getLportTagMaskForReg6() {
        return METADATA_MASK_LPORT_TAG.shiftRight(32).longValue();
    }

    public static long getElanMaskForReg() {
        return METADATA_MASK_SERVICE.shiftRight(24).longValue();
    }

    public static long getVpnIdMaskForReg() {
        return METADATA_MASK_VRFID.shiftRight(1).longValue();
    }

    public static BigInteger mergeMetadataValues(BigInteger metadata, BigInteger metadata2) {
        return metadata.or(metadata2);
    }

    public static BigInteger mergeMetadataMask(BigInteger mask, BigInteger mask2) {
        return mask.or(mask2);
    }
}
