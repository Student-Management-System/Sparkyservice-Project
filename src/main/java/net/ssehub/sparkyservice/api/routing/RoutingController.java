package net.ssehub.sparkyservice.api.routing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.ssehub.sparkyservice.api.config.ControllerPath;

/**
 * Informational controller for routing purposes.
 * 
 * @author marcel
 */
@RestController
public class RoutingController {

    /**
     * Only for swagger in order to generate a proper client.
     * 
     * @param path
     */
    @Operation(security = { @SecurityRequirement(name = "bearer-key") })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "2XX", description = "forwarded to the target"),
        @ApiResponse(responseCode = "403", description = "User is not authorized to access path"),
        @ApiResponse(responseCode = "401", description = "This path is protected. User needs to authenticate ") 
    })
    @GetMapping(value = "{path}")
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    public void route(@PathVariable("path") String path) {}

    /**
     * Hearbeat - gives a simple life sign.
     */
    @Operation(description = "Checks if the and API is reachable under /api/v0")
    @ApiResponse(responseCode = "204", description = "Status is up - No content to send")
    @GetMapping(value = ControllerPath.HEARTBEAT)
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void isAlive() {}
}
