package org.opendaylight.genius.itm.cache;

import java.math.BigInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Created by enedant on 3/14/2018.
 */
@Singleton
public class DpnsTepsKeyedCache extends DataObjectCache<BigInteger, DPNTEPsInfo>{

    @Inject
    public DpnsTepsKeyedCache(DataBroker dataBroker, CacheProvider cacheProvider) {
        super(DPNTEPsInfo.class, dataBroker, LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class).build(), cacheProvider,
                (iid, obj) -> obj.getDPNID(), iid -> iid);
    }
}
