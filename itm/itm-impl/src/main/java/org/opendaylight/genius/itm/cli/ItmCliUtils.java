/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.cli;

import com.google.common.base.Preconditions;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * The Utility class for ITM CLI.
 */
public final class ItmCliUtils {

    private ItmCliUtils() { }

    /**
     * Construct dpn id list.
     *
     * @param dpnIds
     *            the dpn ids
     * @return the list
     */
    public static List<BigInteger> constructDpnIdList(final String dpnIds) {
        final List<BigInteger> lstDpnIds = new ArrayList<>();
        if (StringUtils.isNotBlank(dpnIds)) {
            final String[] arrDpnIds = StringUtils.split(dpnIds, ',');
            for (String dpn : arrDpnIds) {
                String trimmedDpn = StringUtils.trim(dpn);
                Preconditions.checkArgument(StringUtils.isNumeric(trimmedDpn),
                    "DPN ID [" + dpn + "] is not a numeric value.");
                lstDpnIds.add(new BigInteger(trimmedDpn));
            }
        }
        return lstDpnIds;
    }
}
