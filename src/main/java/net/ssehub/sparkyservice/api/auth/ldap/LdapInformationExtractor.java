package net.ssehub.sparkyservice.api.auth.ldap;

import java.time.LocalDate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.ldap.ppolicy.PasswordPolicyControl;
import org.springframework.security.ldap.ppolicy.PasswordPolicyResponseControl;

/**
 * Class for extracting information from provided ldap information.
 * 
 * @author marcel
 */
class LdapInformationExtractor {

    @Nonnull
    private final DirContextOperations ldapContext;

    /**
     * LDAP Context mapper. It extracts information from a given ldap context. This is not thread safe!
     * 
     * @param ldapContext - May holds information
     */
    public LdapInformationExtractor(@Nonnull DirContextOperations ldapContext) {
        this.ldapContext = ldapContext;
    }

    /**
     * The extracted fullname of an ldap user which should match with the desired information at 
     * {@link LdapUser#setFullname(String)}.
     * <br>
     * 
     * @return Example "Max Musterman" but may be null or empty
     */
    @Nullable
    String getFullname() {
        Attribute fullnameAttr = ldapContext.getAttributes().get("displayname");
        return getValue(fullnameAttr);
    }

    /**
     * Email from LDAP response.
     * 
     * @return Mail field from ldap
     */
    @Nullable
    String getEmail() {
        Attribute emailAttr = ldapContext.getAttributes().get("mail");
        return getValue(emailAttr);
        
    }

    /**
     * Extracts a value from a given attribute.
     * 
     * @param attribute Attribute which holds a value
     * @return The content of the attribute
     */
    @Nullable
    private String getValue(Attribute attribute) {
        String entry = null;
        try {
            if (attribute != null) {
                entry = (String) attribute.get();
            } 
        } catch (NamingException e) {
            e.printStackTrace();
            entry = null;
        }
        return entry; 
    }

    /**
     * The extracted expiration date of the account which was authenticated. Matches with the disired information
     * at {@link LdapUser#setExpireDate(LocalDate)}.
     * 
     * @return Date of expiration of the account present in the context
     */
    @Nullable
    LocalDate getExpirationDate() {
        PasswordPolicyResponseControl ppolicy = (PasswordPolicyResponseControl) ldapContext
                .getObjectAttribute(PasswordPolicyControl.OID);
        LocalDate expDate = null;
        if (ppolicy != null) {
            //expDAte // TODO 
            //ldapuser.setTimeBeforeExpiration(ppolicy.getTimeBeforeExpiration());
        }
        return expDate;
    }
}