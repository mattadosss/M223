package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/calculator")
public class CalculatorResource {

    @GET
    @Path("/add/{first}/{second}")
    @Produces(MediaType.TEXT_PLAIN)
    public String add(@PathParam("first") int first, @PathParam("second") int second) {
        return Integer.toString(first + second);
    }

    @GET
    @Path("/minus/{first}/{second}")
    @Produces(MediaType.TEXT_PLAIN)
    public String minus(@PathParam("first") int first, @PathParam("second") int second) {
        return Integer.toString(first - second);
    }
}