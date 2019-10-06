/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.tests;

import ch.vorburger.xtendbeans.XtendBeanGenerator;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

public class UintXtendBeanGenerator extends XtendBeanGenerator {
    @Override
    protected CharSequence stringify(Object object) {
        if (object instanceof Uint8) {
            return "(u8)" + object;
        } else if (object instanceof Uint16) {
            return "(u16)" + object;
        } else if (object instanceof Uint32) {
            return "(u32)" + object;
        } else if (object instanceof Uint64) {
            return "(u64)" + object;
        } else {
            return super.stringify(object);
        }
    }
}
