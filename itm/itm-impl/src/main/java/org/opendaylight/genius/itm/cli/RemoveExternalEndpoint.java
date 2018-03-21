/*
 * Copyright (c) 2015, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/*
 * Created by Chintan Apte on 7/5/2016.
 */

@Command(scope = "tep", name = "rem-external-endpoint", description = "removing an external endpoint")
public class RemoveExternalEndpoint extends OsgiCommandSupport {
    @Argument(index = 0, name = "destination-ip", description = "Destination-IP", required = true, multiValued = false)
    private String destinationIp;
    @Argument(index = 1, name = "TunnelType", description = "Tunnel-Type", required = true, multiValued = false)
    private String tunnelType;

    private static final Logger LOG = LoggerFactory.getLogger(RemoveExternalEndpoint.class);
    private IITMProvider itmProvider;

    public void setItmProvider(IITMProvider itmProvider) {
        this.itmProvider = itmProvider;
    }

    @SuppressWarnings({"checkstyle:IllegalCatch", "checkstyle:RegexpSinglelineJava"})
    @Override
    protected Object doExecute() {
        try {
            LOG.debug("RemoveExternalEndpoint: destinationIP {} with tunnelType {}", destinationIp, tunnelType);
            Class<? extends TunnelTypeBase> tunType;
            if (tunnelType.equalsIgnoreCase(ITMConstants.TUNNEL_TYPE_VXLAN)) {
                tunType = TunnelTypeVxlan.class;
            } else if (tunnelType.equalsIgnoreCase(ITMConstants.TUNNEL_TYPE_GRE)) {
                tunType = TunnelTypeGre.class;
            } else if (tunnelType.equalsIgnoreCase(ITMConstants.TUNNEL_TYPE_MPLSoGRE)) {
                tunType = TunnelTypeMplsOverGre.class;
            } else {
                System.out.println("Invalid tunnel-type " + tunnelType);
                return null;
            }

            if (!itmProvider.validateIP(destinationIp)) {
                System.out.println("Invalid IpAddress " + destinationIp);
                return null;
            }

            IpAddress dcgwIPAddr = IpAddressBuilder.getDefaultInstance(destinationIp);
            itmProvider.remExternalEndpoint(tunType, dcgwIPAddr);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            LOG.error("Exception occurred during execution of command \"tep:configure-tunnelType\": ", e);
        }
        return null;
    }
}
