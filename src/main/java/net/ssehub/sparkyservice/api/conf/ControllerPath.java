package net.ssehub.sparkyservice.api.conf;

public class ControllerPath {
    public static final String GLOBAL_PREFIX = "/api/v0";

    public static final String SWAGGER = "swagger-ui.html";

    public static final String MANAGEMENT_PREFIX = GLOBAL_PREFIX + "/management";

    public static final String USERS_PREFIX = GLOBAL_PREFIX + "/users";

    public static final String AUTHENTICATION_AUTH = GLOBAL_PREFIX + "/authenticate";
    public static final String AUTHENTICATION_CHECK = AUTHENTICATION_AUTH + "/check";
}
