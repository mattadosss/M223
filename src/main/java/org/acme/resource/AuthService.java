package org.acme.resource;

import java.util.Set;

import io.smallrye.jwt.build.Jwt;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Produces(MediaType.TEXT_PLAIN)
public class AuthService {

    @POST
    @Path("/token")
    public Response generateToken() {
        String token = Jwt.upn("jdoe@quarkus.io")
                .issuer("https://example.com/issuer")
                .groups(Set.of("User", "Admin"))
                .sign();

        return Response.ok(token).build();
    }
}
