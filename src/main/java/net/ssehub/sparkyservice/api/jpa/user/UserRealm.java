package net.ssehub.sparkyservice.api.jpa.user;

/**
 * Authentication realm of a user.
 * 
 * @author marcel
 */
public enum UserRealm {
    LOCAL,
    LDAP,
    MEMORY,
    UNKNOWN
}
