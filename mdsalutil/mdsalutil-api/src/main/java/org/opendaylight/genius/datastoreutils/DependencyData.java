package org.opendaylight.genius.datastoreutils;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Created by esiperu on 1/11/2017.
 */
public class DependencyData {

    public InstanceIdentifier<?> iid;
    public InstanceIdentifier<?> wildCardPath;
    public boolean expectData;
    public LogicalDatastoreType dsType;

    public DependencyData(InstanceIdentifier<?> iid, boolean expectData, LogicalDatastoreType dsType,
                          InstanceIdentifier<?> wildCardPath) {
        this.iid = iid;
        this.expectData = expectData;
        this.dsType = dsType;
        this.wildCardPath = wildCardPath;
    }
    public LogicalDatastoreType getDsType() {
        return dsType;
    }

}
