/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import java.math.BigInteger;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "tep", name = "add", description = "adding a tunnel end point")
public class TepAdd extends OsgiCommandSupport {

    @Argument(index = 0, name = "dpnId", description = "DPN-ID", required = false, multiValued = false)
    private BigInteger dpnId;
    @Argument(index = 1, name = "portNo", description = "port-name", required = false, multiValued = false)
    private String portName;
    @Argument(index = 2, name = "vlanId", description = "vlan-id", required = false, multiValued = false)
    private Integer vlanId;
    @Argument(index = 3, name = "ipAddress", description = "ip-address", required = false, multiValued = false)
    private String ipAddress;
    @Argument(index = 4, name = "subnetMask", description = "subnet-Mask", required = false, multiValued = false)
    private String subnetMask;
    @Argument(index = 5, name = "gatewayIp", description = "gateway-ip", required = false, multiValued = false)
    private String gatewayIp;
    @Argument(index = 6, name = "transportZone", description = "transport_zone", required = false, multiValued = false)
    private String transportZone;
    private static final Logger LOG = LoggerFactory.getLogger(TepAdd.class);
    private IITMProvider itmProvider;

    public void setItmProvider(IITMProvider itmProvider) {
        this.itmProvider = itmProvider;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    protected Object doExecute() {
        try {
            if (dpnId == null || portName == null || vlanId == null || ipAddress == null || subnetMask == null
                    || transportZone == null) {
                session.getConsole().println("Insufficient Arguments");
                session.getConsole().println("Correct Usage : exec tep-add dpnId portName vlanId ipAddress subnetMask "
                        + "gatewayIp transportZone");
                return null;
            }
            LOG.debug("Executing create TEP command dpnId={}, portName={}, vlanId={}, ipAddress={},"
                            + " subnetMask={}, gatewayIp={}, transportZone={}", dpnId, portName, vlanId, ipAddress,
                    subnetMask, gatewayIp, transportZone);
            itmProvider.createLocalCache(dpnId, portName, vlanId, ipAddress, subnetMask, gatewayIp,
                    transportZone);
        } catch (Exception e) {
            LOG.error("Exception occurred during execution of command \"tep-add\"", e);
            throw e;
        }
        return null;
    }
}
