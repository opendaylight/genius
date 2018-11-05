/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.cli;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.genius.itm.api.IITMProvider;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeGre;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.vtep.config.schemas.VtepConfigSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class which implements karaf command "vtep:schema-show".
 */
@Command(scope = "vtep", name = "schema-show", description = "Show all VTEP schemas.")
public class VtepSchemaShow extends OsgiCommandSupport {

    /** The schema name. */
    @Argument(index = 0, name = "schemaName", description = "Schema name", required = false, multiValued = false)
    private String schemaName;

    private static String VTEP_CONFIG_SCHEMA_CLI_FORMAT = "%-14s %-12s %-8s %-16s %-13s %-14s %-11s %-20s %-32s";
    public static final String HEADER_UNDERLINE = "------------------------------------------------------------------"
            + "---------------------------------------------------------------------";

    /** The Constant logger. */
    private static final Logger LOG = LoggerFactory.getLogger(VtepSchemaShow.class);

    /** The itm provider. */
    private IITMProvider itmProvider;

    /**
     * Sets the itm provider.
     *
     * @param itmProvider
     *            the new itm provider
     */
    public void setItmProvider(IITMProvider itmProvider) {
        this.itmProvider = itmProvider;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.karaf.shell.console.AbstractAction#doExecute()
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    @Override
    protected Object doExecute() {
        LOG.debug("Executing command: schema-show {} ", this.schemaName);
        try {
            if (this.schemaName != null) {
                VtepConfigSchema schema = this.itmProvider.getVtepConfigSchema(this.schemaName);
                if (schema == null) {
                    session.getConsole().println("No VTEP schema present with name: " + this.schemaName);
                } else {
                    session.getConsole().println(getHeaderOutput());
                    printSchema(schema);
                }
            } else {
                printAllVtepSchemas();
            }
        } catch (Exception e) {
            LOG.error("Exception occurred during execution of command \"vtep:schema-show\": ", e);
        }
        return null;
    }

    /**
     * Prints all vtep schemas.
     */
    private void printAllVtepSchemas() {
        List<VtepConfigSchema> schemas = this.itmProvider.getAllVtepConfigSchemas();
        if (schemas == null || schemas.isEmpty()) {
            session.getConsole().println("No VTEP schemas present.");
            return;
        }
        session.getConsole().println(getHeaderOutput());
        for (VtepConfigSchema schema : schemas) {
            printSchema(schema);
        }
    }

    /**
     * Prints the schema.
     *
     * @param schema
     *            the schema
     */
    private void printSchema(VtepConfigSchema schema) {
        List<BigInteger> lstDpnIds = (schema.getDpnIds() == null) ? Collections.emptyList()
                : ItmUtils.getDpnIdList(schema.getDpnIds());
        List<String> lstIpFilter = getExcludeIpFilterAsList(schema.getExcludeIpFilter());

        Iterator<BigInteger> dpnIterator = lstDpnIds.iterator();
        Iterator<String> ipFilterIterator = lstIpFilter.iterator();

        String portName = StringUtils.defaultString(schema.getPortName());
        String vlanId = String.valueOf(schema.getVlanId());
        String subnetCIDR = (schema.getSubnet() == null) ? StringUtils.EMPTY
                : schema.getSubnet().stringValue();
        String gatewayIp = (schema.getGatewayIp() == null) ? StringUtils.EMPTY
                : schema.getGatewayIp().stringValue();
        String transportZone = StringUtils.defaultString(schema.getTransportZoneName());
        String strTunnelType ;

        Class<? extends TunnelTypeBase> tunType = schema.getTunnelType();

        if (TunnelTypeGre.class.equals(tunType)) {
            strTunnelType = ITMConstants.TUNNEL_TYPE_GRE;
        } else {
            strTunnelType = ITMConstants.TUNNEL_TYPE_VXLAN;
        }
        String dpnId = (dpnIterator.hasNext() ? String.valueOf(dpnIterator.next()) : StringUtils.EMPTY);
        String excludeIpFilter = (ipFilterIterator.hasNext() ? String.valueOf(ipFilterIterator.next())
                : StringUtils.EMPTY);

        // Print first row
        session.getConsole().println(String.format(VTEP_CONFIG_SCHEMA_CLI_FORMAT, schema.getSchemaName(),
                portName, vlanId, subnetCIDR, gatewayIp, transportZone, strTunnelType, dpnId, excludeIpFilter));
        while (dpnIterator.hasNext() || ipFilterIterator.hasNext()) {
            dpnId = (dpnIterator.hasNext() ? String.valueOf(dpnIterator.next()) : StringUtils.EMPTY);
            excludeIpFilter = (ipFilterIterator.hasNext() ? String.valueOf(ipFilterIterator.next())
                    : StringUtils.EMPTY);
            session.getConsole().println(String.format(VTEP_CONFIG_SCHEMA_CLI_FORMAT, StringUtils.EMPTY,
                    StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY,
                    StringUtils.EMPTY, dpnId, excludeIpFilter));
        }
        session.getConsole().println(System.lineSeparator());
    }

    /**
     * Gets the exclude ip filter as list.
     *
     * @param excludeIpFilter
     *            the exclude ip filter
     * @return the exclude ip filter as list
     */
    private List<String> getExcludeIpFilterAsList(String excludeIpFilter) {
        if (StringUtils.isBlank(excludeIpFilter)) {
            return Collections.emptyList();
        }
        final String[] arrIpsOrRange = StringUtils.split(excludeIpFilter, ',');
        return Arrays.asList(arrIpsOrRange);
    }

    /**
     * Gets the vtep config schema header output.
     *
     * @return the vtep config schema header output
     */
    private String getHeaderOutput() {
        String headerBuilder =
                String.format(VTEP_CONFIG_SCHEMA_CLI_FORMAT, "SchemaName", "PortName", "VlanID", "Subnet", "GatewayIP",
                        "TransportZone", "TunnelType", "DPN-IDS", "ExcludeIpFilter") + '\n' + HEADER_UNDERLINE;
        return headerBuilder;
    }
}
