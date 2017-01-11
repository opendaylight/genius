/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.counterframework.counternorthbound;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.opendaylight.genius.fcapsmanager.counterframework.model.NorthBoundBgpStatistics;
import org.opendaylight.genius.fcapsmanager.counterframework.model.NorthBoundControllersCounters;
import org.opendaylight.genius.fcapsmanager.counterframework.model.NorthBoundSwitchStatistics;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.HttpURLConnection;


@Path("/statistics")
public class NorthBoundBulkCounter {

    @GET
    @Path("/bgp")
    @Produces({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackSubnets.class)
    @StatusCodes({
            @ResponseCode(code = HttpURLConnection.HTTP_OK, condition = "Operation successful"),
            @ResponseCode(code = HttpURLConnection.HTTP_UNAUTHORIZED, condition = "Unauthorized"),
            @ResponseCode(code = HttpURLConnection.HTTP_NOT_IMPLEMENTED, condition = "Not Implemented"),
            @ResponseCode(code = HttpURLConnection.HTTP_UNAVAILABLE, condition = "No providers available") })
    public Response listBgpCounters () {

        RetreiveStatistics retreiveStatistics = new RetreiveStatistics();
        NorthBoundBgpStatistics northBoundBgpStatistics = retreiveStatistics.getBgpNeighborsStatistics();
        return Response.status(200).entity(northBoundBgpStatistics).build();
    }

    @GET
    @Path("/flow-capable-switches")
    @Produces({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackSubnets.class)
    @StatusCodes({
            @ResponseCode(code = HttpURLConnection.HTTP_OK, condition = "Operation successful"),
            @ResponseCode(code = HttpURLConnection.HTTP_UNAUTHORIZED, condition = "Unauthorized"),
            @ResponseCode(code = HttpURLConnection.HTTP_NOT_IMPLEMENTED, condition = "Not Implemented"),
            @ResponseCode(code = HttpURLConnection.HTTP_UNAVAILABLE, condition = "No providers available") })
    public Response listSwitchCounters () {

        RetreiveStatistics retreiveStatistics = new RetreiveStatistics();
        NorthBoundSwitchStatistics northBoundSwitchStatistics = retreiveStatistics.getSwitchStatistics();
        return Response.status(200).entity(northBoundSwitchStatistics).build();
    }

    @GET
    @Path("/controller-switch-mappings")
    @Produces({ MediaType.APPLICATION_JSON })
    //@TypeHint(OpenStackSubnets.class)
    @StatusCodes({
            @ResponseCode(code = HttpURLConnection.HTTP_OK, condition = "Operation successful"),
            @ResponseCode(code = HttpURLConnection.HTTP_UNAUTHORIZED, condition = "Unauthorized"),
            @ResponseCode(code = HttpURLConnection.HTTP_NOT_IMPLEMENTED, condition = "Not Implemented"),
            @ResponseCode(code = HttpURLConnection.HTTP_UNAVAILABLE, condition = "No providers available") })
    public Response listControllersCounters () {

        RetreiveStatistics retreiveStatistics = new RetreiveStatistics();
        NorthBoundControllersCounters controllersCounters = retreiveStatistics.getControllerNodeCounters();
        return Response.status(200).entity(controllersCounters).build();
    }
}