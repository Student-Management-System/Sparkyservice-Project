package net.ssehub.sparkyservice.api.config;

/**
 * Provides controller paths. They define a path where a controller or resource is located at. 
 * 
 * @author marcel
 */
public final class ControllerPath {
    public static final String GLOBAL_PREFIX = "/api/v2";
    public static final String HEARTBEAT = GLOBAL_PREFIX + "/heartbeat"; 

    public static final String SWAGGER = "swagger-ui.html";

    public static final String MANAGEMENT_PREFIX = GLOBAL_PREFIX + "/management";

    public static final String USERS_PREFIX = GLOBAL_PREFIX + "/users";
    public static final String USERS_PATCH = USERS_PREFIX;
    public static final String USERS_PUT = USERS_PREFIX;
    public static final String USERS_DELETE = USERS_PREFIX + "/{username}";
    public static final String USERS_GET_SINGLE = USERS_DELETE;
    public static final String USERS_GET_ALL = USERS_PREFIX;

    public static final String AUTHENTICATION_AUTH = GLOBAL_PREFIX + "/login";
    public static final String AUTHENTICATION_CHECK = GLOBAL_PREFIX + "/whoAmI";
    public static final String AUTHENTICATION_VERIFY = GLOBAL_PREFIX + "/verify";
    public static final String RENEW_JWT = GLOBAL_PREFIX + "/renew";
       
}
