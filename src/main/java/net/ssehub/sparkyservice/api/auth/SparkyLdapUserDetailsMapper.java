package net.ssehub.sparkyservice.api.auth;

import java.util.Collection;
import java.util.Optional;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.ppolicy.PasswordPolicyControl;
import org.springframework.security.ldap.ppolicy.PasswordPolicyResponseControl;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.LdapUser;

/**
 * {@link UserDetails} mapper. It creates a {@link LdapUser} object from a successful LDAP login.
 * 
 * @author marcel
 */
public final class SparkyLdapUserDetailsMapper implements UserDetailsContextMapper {

    /**
     * https://ldap.com/ldap-oid-reference-guide/
     */
    public static final String DISPLAY_NAME_OID = "2.16.840.1.113730.3.1.241";

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
            Collection<? extends GrantedAuthority> authorities) {
        
        // we don't want LDAP Authroties here so we don't use them and load 
        var ldapuser = new LdapUser(username, UserRole.DEFAULT /*maybe change later*/, true);
        
        Attribute fullnameAttr = ctx.getAttributes().get("displayname");
        try {
            if (fullnameAttr != null) {
                ldapuser.setFullName((String) fullnameAttr.get()) ;
            }
//            fullnameAttr.map(String.class::cast).ifPresent(ldapuser::setFullName);
        } catch (NamingException e) {
            e.printStackTrace();
        }
        PasswordPolicyResponseControl ppolicy = (PasswordPolicyResponseControl) ctx
                .getObjectAttribute(PasswordPolicyControl.OID);
        if (ppolicy != null) {
            ldapuser.setTimeBeforeExpiration(ppolicy.getTimeBeforeExpiration());
        }
        return ldapuser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException(
                "SparkyLdapUserDetailsMapper only supports reading from a context. Please"
                        + " use a subclass if mapUserToContext() is required.");
    }
}
