/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.api;
import java.math.BigInteger;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.interfacemanager.exceptions.InterfaceNotFoundException;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.itm.config.rev151102.vtep.config.schemas.VtepConfigSchema;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.VtepConfigSchema;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList ;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;

public interface IITMProvider {
    // APIs used by i
    public void createLocalCache(BigInteger dpnId, String portName, Integer vlanId, String ipAddress, String subnetMask, String gatewayIp, String transportZone);

    public void commitTeps();

    public DataBroker getDataBroker();

    public void showTeps();

    public void showState(TunnelList tunnels);

    public void deleteVtep(BigInteger dpnId, String portName, Integer vlanId, String ipAddress, String subnetMask,
                           String gatewayIp, String transportZone);
    // public void showState(TunnelsState tunnelsState);
    public void configureTunnelType(String transportZone, String tunnelType);


    /**
     * Adds the vtep config schema.
     *
     * @param vtepConfigSchema
     *            the vtep config schema
     */
    public void addVtepConfigSchema(VtepConfigSchema vtepConfigSchema);

    /**
     * Gets the vtep config schema.
     *
     * @param schemaName
     *            the schema name
     * @return the vtep config schema
     */
    public VtepConfigSchema getVtepConfigSchema(String schemaName);

    /**
     * Gets the all vtep config schemas.
     *
     * @return the all vtep config schemas
     */
    public List<VtepConfigSchema> getAllVtepConfigSchemas();

    /**
     * Update VTEP schema.
     *
     * @param schemaName
     *            the schema name
     * @param lstDpnsForAdd
     *            the lst dpns for add
     * @param lstDpnsForDelete
     *            the lst dpns for delete
     */
    public void updateVtepSchema(String schemaName, List<BigInteger> lstDpnsForAdd, List<BigInteger> lstDpnsForDelete);

    /**
     * Delete all vtep schemas.
     */
    public void deleteAllVtepSchemas();

    public void configureTunnelMonitorParams(boolean monitorEnabled, String monitorProtocol);

    public void configureTunnelMonitorInterval(int interval);

    public void addExternalEndpoint(java.lang.Class<? extends TunnelTypeBase> tunType, IpAddress dcgwIP);

    public void remExternalEndpoint(java.lang.Class<? extends TunnelTypeBase> tunType, IpAddress dcgwIP);

    public boolean validateIP (final String ip);
}
