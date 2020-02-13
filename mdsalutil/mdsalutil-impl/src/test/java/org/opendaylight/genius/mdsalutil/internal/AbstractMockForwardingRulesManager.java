/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.mdsalutil.internal;

import java.util.Collection;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yangtools.yang.binding.DataObject;

public abstract class AbstractMockForwardingRulesManager<D extends DataObject> implements DataTreeChangeListener<D> {

    public AbstractMockForwardingRulesManager() {
        // Do Nothing
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<D>> changes) {
        // TODO Auto-generated method stub
    }
}
