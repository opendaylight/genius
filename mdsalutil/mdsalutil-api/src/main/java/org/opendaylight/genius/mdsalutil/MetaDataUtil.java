/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil;

import java.math.BigInteger;

public class MetaDataUtil {
    public static final BigInteger METADATA_MASK_VRFID =         new BigInteger("00000000FFFFFFFE", 16);
    public static final BigInteger METADATA_MASK_LPORT_TAG =     new BigInteger("0FFFFF0000000000", 16);
    public static final BigInteger METADATA_MASK_SERVICE =       new BigInteger("000000FFFF000000", 16);
    public static final BigInteger METADATA_MASK_SERVICE_INDEX = new BigInteger("F000000000000000", 16);
    public static final BigInteger METADA_MASK_VALID_TUNNEL_ID_BIT_AND_TUNNEL_ID = new BigInteger("08000000FFFFFF00", 16);
    public static final BigInteger METADATA_MASK_LABEL_ITM =     new BigInteger("40FFFFFF000000FF", 16);
    public static final BigInteger METADA_MASK_TUNNEL_ID =       new BigInteger("00000000FFFFFF00", 16);
    public static final BigInteger METADATA_MASK_SERVICE_SH_FLAG = new BigInteger("000000FFFF000001", 16);
    public static final BigInteger METADATA_MASK_LPORT_TAG_SH_FLAG =     new BigInteger("0FFFFF0000000001", 16);
    public static final BigInteger METADATA_MASK_ELAN_SUBNET_ROUTE =    new BigInteger("0000FFFF00000000", 16);
    public static final BigInteger METADATA_MASK_SUBNET_ROUTE =         new BigInteger("0000FFFFFFFFFFFE", 16);

    public static BigInteger getMetaDataForLPortDispatcher(int lportTag, short serviceIndex) {
        return getServiceIndexMetaData(serviceIndex).or(getLportTagMetaData(lportTag));
    }

    public static BigInteger getMetaDataForLPortDispatcher(int lportTag, short serviceIndex,
            BigInteger serviceMetaData) {
        return getMetaDataForLPortDispatcher(lportTag, serviceIndex, serviceMetaData, false);
    }

    public static BigInteger getMetaDataForLPortDispatcher(int lportTag, short serviceIndex,
                                                           BigInteger serviceMetaData, boolean isSHFlagSet) {
        int shBit = (isSHFlagSet) ? 1 : 0;
        return getServiceIndexMetaData(serviceIndex).or(getLportTagMetaData(lportTag)).or(serviceMetaData)
                .or(BigInteger.valueOf(shBit));
    }

    public static BigInteger getServiceIndexMetaData(int serviceIndex) {
        return new BigInteger("F", 16).and(BigInteger.valueOf(serviceIndex)).shiftLeft(60);
    }

    public static BigInteger getLportTagMetaData(int lportTag) {
        return new BigInteger("FFFFF", 16).and(BigInteger.valueOf(lportTag)).shiftLeft(40);
    }

    public static BigInteger getMetaDataMaskForLPortDispatcher() {
        return getMetaDataMaskForLPortDispatcher(METADATA_MASK_LPORT_TAG);
    }

    public static BigInteger getMetaDataMaskForLPortDispatcher(BigInteger metadataMaskForLPortTag) {
        return METADATA_MASK_SERVICE_INDEX.or(metadataMaskForLPortTag);
    }

    public static BigInteger getMetadataLPort(int lPortTag) {
        return (new BigInteger("FFFF", 16).and(BigInteger.valueOf(lPortTag))).shiftLeft(40);
    }

    public static BigInteger getLportFromMetadata(BigInteger metadata) {
        return (metadata.and(METADATA_MASK_LPORT_TAG)).shiftRight(40);
    }

    public static int getElanTagFromMetadata(BigInteger metadata) {
        return (((metadata.and(MetaDataUtil.METADATA_MASK_SERVICE)).
                shiftRight(24))).intValue();
    }

    public static int getServiceTagFromMetadata(BigInteger metadata) {
        return (((metadata.and(MetaDataUtil.METADATA_MASK_SERVICE_INDEX)).
                shiftRight(60))).intValue();
    }

    public static BigInteger getMetaDataMaskForLPortDispatcher(BigInteger metadataMaskForServiceIndex,
                                                               BigInteger metadataMaskForLPortTag, BigInteger metadataMaskForService) {
        return metadataMaskForServiceIndex.or(metadataMaskForLPortTag).or(metadataMaskForService);
    }

    /**
     * For the tunnel id with VNI and valid-vni-flag set, the most significant byte
     * should have 08. So, shifting 08 to 7 bytes (56 bits) and the result is OR-ed with
     * VNI being shifted to 1 byte.
     */
    public static BigInteger getTunnelIdWithValidVniBitAndVniSet(int vni) {
        return BigInteger.valueOf(0X08).shiftLeft(56).or(BigInteger.valueOf(vni).shiftLeft(8));
    }

    public static long getNatRouterIdFromMetadata(BigInteger metadata){
        return getVpnIdFromMetadata(metadata);
    }

    public static BigInteger getVpnIdMetadata(long vrfId) {
        return METADATA_MASK_VRFID.and(BigInteger.valueOf(vrfId).shiftLeft(1));
    }

    public static long getVpnIdFromMetadata(BigInteger metadata) {
        return (metadata.and(METADATA_MASK_VRFID).shiftRight(1)).longValue();
    }
    public static BigInteger getWriteMetaDataMaskForDispatcherTable() {
        return new BigInteger("FFFFFFFFFFFFFFFE", 16);
    }

    public static BigInteger getLportTagForReg6(int lportTag) {
        return new BigInteger("FFFFF", 16).and(BigInteger.valueOf(lportTag)).shiftLeft(8);
    }
    public static BigInteger getServiceIndexForReg6(int serviceIndex) {
        return new BigInteger("F", 16).and(BigInteger.valueOf(serviceIndex)).shiftLeft(28);
    }
    public static long getReg6ValueForLPortDispatcher(int lportTag, short serviceIndex) {
        return getServiceIndexForReg6(serviceIndex).or(getLportTagForReg6(lportTag)).longValue();
    }

}
