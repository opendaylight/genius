/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.validator;

import java.util.HashMap;
import java.util.Map;

public enum ValidatorErrorCode {

    ERROR(0, "Error Occurred"),
    NO_ERROR(1, "No Error Occurred");

    private int value;
    private String message;
    private static Map<Integer, String> ERROR_CODE_MAP;

    static {
        ERROR_CODE_MAP = new HashMap<>();
        for (ValidatorErrorCode errorCode : ValidatorErrorCode.values()) {
            ERROR_CODE_MAP.put(errorCode.getValue(), errorCode.getErrorMessage());

        }
    }

    public int getValue() {
        return value;
    }

    public String getErrorMessage() {
        return message;
    }

    ValidatorErrorCode(int val, String msg) {
        this.value = val;
        this.message = msg;
    }

    public static String forValue(int value) {
        return ERROR_CODE_MAP.get(value);
    }
}
