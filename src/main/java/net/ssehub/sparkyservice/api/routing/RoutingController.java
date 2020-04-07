package net.ssehub.sparkyservice.api.routing;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
public class RoutingController {

    /**
     * Only for swagger in order to generate a proper client.
     * 
     * @param path
     */
    @Operation(security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping(value = "{path}")
    @Secured("TODO")
    @ResponseStatus(code = HttpStatus.NOT_IMPLEMENTED)
    public void route(@PathVariable("path") String path) {
        
    }
}
