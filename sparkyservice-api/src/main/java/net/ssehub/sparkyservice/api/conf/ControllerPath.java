package net.ssehub.sparkyservice.api.conf;

public class ControllerPath {
    public static final String GLOBAL_PREFIX = "/api";
    
    public static final String SWAGGER = "swagger-ui.html";
    
    public static final String MANAGEMENT_PREFIX = GLOBAL_PREFIX + "/management";
    public static final String MANAGEMENT_ADD_USER = MANAGEMENT_PREFIX + "/user/add";
    public static final String MANAGEMENT_EDIT_USER = MANAGEMENT_PREFIX + "/user/edit";
    
    public static final String AUTHENTICATION_AUTH = GLOBAL_PREFIX + "/authentication";
}
