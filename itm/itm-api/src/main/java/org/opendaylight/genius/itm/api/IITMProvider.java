/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.api;

import com.google.common.base.Optional;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import org.apache.felix.service.command.CommandSession;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.VtepConfigSchema;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;

public interface IITMProvider {
    // APIs used by i
    void createLocalCache(BigInteger dpnId, String portName, Integer vlanId, String ipAddress, String subnetMask,
            String gatewayIp, String transportZone, CommandSession session);

    void commitTeps();

    DataBroker getDataBroker();

    void showTeps(CommandSession session);

    void showState(Collection<StateTunnelList> tunnels);

    void showBridges(BigInteger dpnId, Collection<OvsdbBridgeRef> bridges);

    void showCache(String cacheName);

    void deleteVtep(BigInteger dpnId, String portName, Integer vlanId, String ipAddress, String subnetMask,
            String gatewayIp, String transportZone, CommandSession session);

    void configureTunnelType(String transportZone, String tunnelType);

    /**
     * Adds the vtep config schema.
     *
     * @param vtepConfigSchema
     *            the vtep config schema
     */
    void addVtepConfigSchema(VtepConfigSchema vtepConfigSchema);

    /**
     * Gets the vtep config schema.
     *
     * @param schemaName
     *            the schema name
     * @return the vtep config schema
     */
    VtepConfigSchema getVtepConfigSchema(String schemaName);

    /**
     * Gets the all vtep config schemas.
     *
     * @return the all vtep config schemas
     */
    List<VtepConfigSchema> getAllVtepConfigSchemas();

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
    void updateVtepSchema(String schemaName, List<BigInteger> lstDpnsForAdd, List<BigInteger> lstDpnsForDelete);

    /**
     * Delete all vtep schemas.
     */
    void deleteAllVtepSchemas();

    void configureTunnelMonitorParams(boolean monitorEnabled, String monitorProtocol);

    void configureTunnelMonitorInterval(int interval);

    void addExternalEndpoint(java.lang.Class<? extends TunnelTypeBase> tunType, IpAddress dcgwIP);

    void remExternalEndpoint(java.lang.Class<? extends TunnelTypeBase> tunType, IpAddress dcgwIP);

    boolean validateIP(String ip);

    Interface getInterface(String tunnelName);

    Optional<StateTunnelList> getTunnelState(String interfaceName) throws ReadFailedException;
}
