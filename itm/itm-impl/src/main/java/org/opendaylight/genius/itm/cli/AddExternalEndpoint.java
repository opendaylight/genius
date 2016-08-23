/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cli;

/**
 * Created by echiapt on 7/5/2016.
 */
/*
{"input" :
    {
"dpn-id" : ["1", "2"],
"destination-ip" : "192.168.56.105",
"tunnel-type" : "odl-interface:tunnel-type-vxlan"
    }
}
 */
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeMplsOverGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "tep", name = "add-external-endpoint", description = "adding an external endpoint")
public class AddExternalEndpoint extends OsgiCommandSupport {
    @Argument(index = 0, name = "destination-ip", description = "Destination-IP", required = true, multiValued = false)
    private String destinationIp;
    @Argument(index = 1, name = "TunnelType", description = "Tunnel-Type", required = true, multiValued = false)
    private String tunnelType;

    private static final Logger LOG = LoggerFactory.getLogger(AddExternalEndpoint.class);
    private IITMProvider itmProvider;

    public void setItmProvider(IITMProvider itmProvider) {
        this.itmProvider = itmProvider;
    }

    @Override
    protected Object doExecute() {
        try {
            LOG.debug("AddExternalEndpoint: destinationIP {} with tunnelType {}", destinationIp, tunnelType);
            Class<? extends TunnelTypeBase> tunType = TunnelTypeVxlan.class;
            if( tunnelType.toUpperCase().equals(ITMConstants.TUNNEL_TYPE_VXLAN))
                tunType = TunnelTypeVxlan.class ;
            else if( tunnelType.toUpperCase().equals(ITMConstants.TUNNEL_TYPE_GRE) )
                tunType = TunnelTypeGre.class ;
            else if (tunnelType.toUpperCase().equals(ITMConstants.TUNNEL_TYPE_MPLSoGRE)) {
                tunType = TunnelTypeMplsOverGre.class;
            } else {
                System.out.println("Invalid tunnel-type " + tunnelType);
                return null;
            }

            if (!itmProvider.validateIP(destinationIp)) {
                System.out.println("Invalid IpAddress " + destinationIp);
                return null;
            }

            IpAddress dcgwIPAddr = new IpAddress(destinationIp.toCharArray());
            itmProvider.addExternalEndpoint(tunType, dcgwIPAddr);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            LOG.error("Exception occurred during execution of command \"tep:configure-tunnelType\": ", e);
        }
        return null;
    }
}
