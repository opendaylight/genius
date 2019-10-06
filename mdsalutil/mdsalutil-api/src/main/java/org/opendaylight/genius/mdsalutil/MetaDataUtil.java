/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.math.BigInteger;
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

    private MetaDataUtil() {

    }

    public static Uint64 getMetaDataForLPortDispatcher(int lportTag, short serviceIndex) {
        // FIXME: this can be done more efficiently
        return Uint64.valueOf(getServiceIndexMetaData(serviceIndex).toJava().or(
            getLportTagMetaData(lportTag).toJava()));
    }

    public static Uint64 getMetaDataForLPortDispatcher(int lportTag, short serviceIndex, Uint64 serviceMetaData) {
        return getMetaDataForLPortDispatcher(lportTag, serviceIndex, serviceMetaData, false);
    }

    public static Uint64 getMetaDataForLPortDispatcher(int lportTag, short serviceIndex,
                                                       Uint64 serviceMetaData, boolean isSHFlagSet) {
        // FIXME: this can be done more efficiently
        return Uint64.valueOf(getServiceIndexMetaData(serviceIndex).toJava().or(getLportTagMetaData(lportTag).toJava())
                .or(serviceMetaData.toJava()).or(isSHFlagSet ? BigInteger.ONE : BigInteger.ZERO));
    }

    public static Uint64 getPolicyClassifierMetaData(long classifier) {
        // FIXME: this can be done more efficiently
        return Uint64.valueOf(METADATA_MASK_POLICY_CLASSIFER_ID.toJava().and(BigInteger.valueOf(classifier)
            .shiftLeft(1)));
    }

    public static Uint64 getServiceIndexMetaData(int serviceIndex) {
        // FIXME: this can be done more efficiently
        return Uint64.valueOf(new BigInteger("F", 16).and(BigInteger.valueOf(serviceIndex)).shiftLeft(60));
    }

    public static Uint64 getLportTagMetaData(int lportTag) {
        // FIXME: this can be done more efficiently
        return Uint64.valueOf(new BigInteger("FFFFF", 16).and(BigInteger.valueOf(lportTag))
            .shiftLeft(METADATA_LPORT_TAG_OFFSET));
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
        // FIXME: this can be done more efficiently
        return Uint64.valueOf(new BigInteger("FFFF", 16).and(BigInteger.valueOf(portTag))
            .shiftLeft(METADATA_LPORT_TAG_OFFSET));
    }

    public static Uint64 getLportFromMetadata(Uint64 metadata) {
        // FIXME: this can be done more efficiently
        return Uint64.valueOf(metadata.toJava().and(METADATA_MASK_LPORT_TAG.toJava())
            .shiftRight(METADATA_LPORT_TAG_OFFSET));
    }

    public static int getElanTagFromMetadata(Uint64 metadata) {
        // FIXME: this can be done more efficiently
        return metadata.toJava().and(MetaDataUtil.METADATA_MASK_SERVICE.toJava()).shiftRight(24).intValue();
    }

    public static long getPolicyClassifierFromMetadata(Uint64 metadata) {
        // FIXME: this can be done more efficiently
        return metadata.toJava().and(METADATA_MASK_POLICY_CLASSIFER_ID.toJava()).shiftRight(1).longValue();
    }

    public static Uint64 getElanTagMetadata(long elanTag) {
        // FIXME: this can be done more efficiently
        return Uint64.valueOf(BigInteger.valueOf(elanTag).shiftLeft(24));
    }

    public static int getServiceTagFromMetadata(Uint64 metadata) {
        // FIXME: this can be done more efficiently
        return metadata.toJava().and(MetaDataUtil.METADATA_MASK_SERVICE_INDEX.toJava())
                .shiftRight(60).intValue();
    }

    /**
     * For the tunnel id with VNI and valid-vni-flag set, the most significant byte
     * should have 08. So, shifting 08 to 7 bytes (56 bits) and the result is OR-ed with
     * VNI being shifted to 1 byte.
     */
    public static Uint64 getTunnelIdWithValidVniBitAndVniSet(int vni) {
        // FIXME: this can be done more efficiently
        return Uint64.valueOf(BigInteger.valueOf(0X08).shiftLeft(56).or(BigInteger.valueOf(vni).shiftLeft(8)));
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
        // FIXME: this can be done more efficiently
        return Uint64.valueOf(METADATA_MASK_ACL_CONNTRACK_CLASSIFIER_TYPE.toJava().and(conntrackClassifierType.toJava()
            .shiftLeft(1)));
    }

    public static Uint64 getAclDropMetaData(Uint64 dropFlag) {
        // FIXME: this can be done more efficiently
        return Uint64.valueOf(METADATA_MASK_ACL_DROP.toJava().and(dropFlag.toJava().shiftLeft(2)));
    }

    public static Uint64 getVpnIdMetadata(long vrfId) {
        // FIXME: this can be done more efficiently
        return Uint64.valueOf(METADATA_MASK_VRFID.toJava().and(BigInteger.valueOf(vrfId).shiftLeft(1)));
    }

    public static long getVpnIdFromMetadata(Uint64 metadata) {
        // FIXME: this can be done more efficiently
        return metadata.toJava().and(METADATA_MASK_VRFID.toJava()).shiftRight(1).longValue();
    }

    public static Uint64 getWriteMetaDataMaskForDispatcherTable() {
        // FIXME: make this an interned constant
        return Uint64.valueOf("FFFFFFFFFFFFFFFE", 16);
    }

    public static Uint64 getWriteMetaDataMaskForEgressDispatcherTable() {
        // FIXME: make this an interned constant
        return Uint64.valueOf("000000FFFFFFFFFE", 16);
    }

    public static Uint64 getLportTagForReg6(int lportTag) {
        // FIXME: this can be done more efficiently
        return Uint64.valueOf(new BigInteger("FFFFF", 16).and(BigInteger.valueOf(lportTag)).shiftLeft(8));
    }

    public static Uint64 getServiceIndexForReg6(int serviceIndex) {
        // FIXME: this can be done more efficiently
        return Uint64.valueOf(new BigInteger("F", 16).and(BigInteger.valueOf(serviceIndex)).shiftLeft(28));
    }

    public static Uint64 getInterfaceTypeForReg6(int tunnelType) {
        // FIXME: this can be done more efficiently
        return Uint64.valueOf(new BigInteger("F", 16).and(BigInteger.valueOf(tunnelType)).shiftLeft(4));
    }

    public static long getReg6ValueForLPortDispatcher(int lportTag, short serviceIndex) {
        // FIXME: this can be done more efficiently
        return getServiceIndexForReg6(serviceIndex).toJava().or(getLportTagForReg6(lportTag).toJava()).longValue();
    }

    /** Utility to fetch the register value for lport dispatcher table.
     * Register6 used for service binding will have first 4 bits of service-index, next 20 bits for lportTag,
     * and next 4 bits for interface-type
     */
    public static long getReg6ValueForLPortDispatcher(int lportTag, short serviceIndex, short interfaceType) {
        // FIXME: this can be done more efficiently
        return getServiceIndexForReg6(serviceIndex).toJava().or(getLportTagForReg6(lportTag).toJava()
            .or(getInterfaceTypeForReg6(interfaceType).toJava())).longValue();
    }

    public static long getRemoteDpnMetadatForEgressTunnelTable(long remoteDpnId) {
        // FIXME: this can be done more efficiently
        return new BigInteger("FFFFF", 16).and(BigInteger.valueOf(remoteDpnId)).shiftLeft(8).longValue();
    }

    public static long getRemoteDpnMaskForEgressTunnelTable() {
        // FIXME: this can be done more efficiently
        return REG6_MASK_REMOTE_DPN.toJava().shiftRight(32).longValue();
    }

    public static long getLportTagMaskForReg6() {
        // FIXME: this can be done more efficiently
        return METADATA_MASK_LPORT_TAG.toJava().shiftRight(32).longValue();
    }

    public static long getElanMaskForReg() {
        // FIXME: this can be done more efficiently
        return METADATA_MASK_SERVICE.toJava().shiftRight(24).longValue();
    }

    public static long getVpnIdMaskForReg() {
        // FIXME: this can be done more efficiently
        return METADATA_MASK_VRFID.toJava().shiftRight(1).longValue();
    }

    public static Uint64 mergeMetadataValues(Uint64 metadata, Uint64 metadata2) {
        // FIXME: this can be done more efficiently
        return Uint64.valueOf(metadata.toJava().or(metadata2.toJava()));
    }

    public static Uint64 mergeMetadataMask(Uint64 mask, Uint64 mask2) {
        // FIXME: this can be done more efficiently
        return Uint64.valueOf(mask.toJava().or(mask2.toJava()));
    }
}
