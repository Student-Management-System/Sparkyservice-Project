package net.ssehub.sparkyservice.api.conf;

public final class ControllerPath {
    public static final String GLOBAL_PREFIX = "/api/v0";

    public static final String SWAGGER = "swagger-ui.html";

    public static final String MANAGEMENT_PREFIX = GLOBAL_PREFIX + "/management";

    public static final String USERS_PREFIX = GLOBAL_PREFIX + "/users";
    public static final String USERS_PATCH = USERS_PREFIX;
    public static final String USERS_PUT = USERS_PREFIX;
    public static final String USERS_DELETE = USERS_PREFIX + "/{realm}/{username}";
    public static final String USERS_GET_SINGLE = USERS_DELETE;
    public static final String USERS_GET_ALL = USERS_PREFIX;

    public static final String AUTHENTICATION_AUTH = GLOBAL_PREFIX + "/authenticate";
    public static final String AUTHENTICATION_CHECK = AUTHENTICATION_AUTH + "/check";
}
