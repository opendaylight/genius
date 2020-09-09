/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.impl;

import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GeniusString {

    private static final Logger LOG = LoggerFactory.getLogger(GeniusString.class);

    private GeniusString() {
    }

    public static String[] stringSplit(String string2Split, char delimiter) {
        LOG.debug("String to split:{}",string2Split);
        int previousIndex = -1;
        int stringLength = string2Split.length();
        List<String> stringList = new LinkedList<>();
        for (int i = 0; i < stringLength; i++) {
            if (string2Split.charAt(i) == delimiter) {
                stringList.add(string2Split.substring(++previousIndex, i));
                previousIndex = i;
            }
        }
        if (previousIndex != (stringLength - 1)) {
            stringList.add(string2Split.substring(++previousIndex, stringLength));
        }
        LOG.debug("List after split:{}",stringList);
        String[] result = stringList.toArray(new String[0]);
        return result;
    }
}
