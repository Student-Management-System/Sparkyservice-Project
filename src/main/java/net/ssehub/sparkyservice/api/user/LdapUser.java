package net.ssehub.sparkyservice.api.user;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.ldap.userdetails.LdapUserDetails;

import net.ssehub.sparkyservice.api.jpa.user.PersonalSettings;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.dto.UserDto.ChangePasswordDto;

/**
 * Represents a user which is in the ldap domain of university hildesheim.
 * 
 * @author marcel
 */
public class LdapUser extends AbstractSparkyUser implements SparkyUser, LdapUserDetails {

    public @Nonnull static final UserRealm ASSOCIATED_REALM = UserRealm.UNIHI;
    private static final long serialVersionUID = -2155556837850826196L;
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private String dn;

    /**
     * Creates a user with necessary fields.
     * 
     * @param nickname The nickname ("real" username without realm information)
     * @param role
     * @param isEnabled
     * @see #create(String, UserRole, boolean)
     */
    LdapUser(@Nonnull String nickname, @Nonnull UserRole role, boolean isEnabled) {
        super(new Identity(nickname, ASSOCIATED_REALM), role);
        setEnabled(isEnabled);
    }
    
    /**
     * Creates an user with necessary fields. It preserves the old identity. 
     * 
     * @param ident
     * @param role
     * @param isEnabled
     */
    private LdapUser(@Nonnull Identity ident, @Nonnull UserRole role, boolean isEnabled) {
        super(ident, role);
        setEnabled(isEnabled);
        if (ident.realm() != ASSOCIATED_REALM) {
            log.debug("Preserving the identity of one user {}", ident.asUsername());
        }
    }
    
    /**
     * Copy constructor of another SparkyUser.
     * 
     * @param copyMe
     * @see #create(String, UserRole, boolean)
     */
    // TODO this is currently unused ?
    LdapUser(SparkyUser copyMe) {
        this(copyMe.getIdentity(), copyMe.getRole(), copyMe.isEnabled());
        if (copyMe instanceof LdapUser) {
            dn = ((LdapUser) copyMe).getDn();
        }
        this.setExpireDate(copyMe.getExpireDate().orElse(null));
        this.setSettings(copyMe.getSettings());
    }

    /**
     * Creates an instance from a JPA user. 
     * 
     * @param jpaUser
     * @see #create(String, UserRole, boolean)
     */
    LdapUser(User jpaUser) {
        this(new Identity(jpaUser.getNickname(), jpaUser.getRealm()), jpaUser.getRole(), jpaUser.isActive());
        this.setExpireDate(jpaUser.getExpirationDate());
        this.setSettings(jpaUser.getProfileConfiguration());
        this.setFullname(jpaUser.getFullName());
        super.databaseId = jpaUser.getId();
    }

    @Override
    public String getPassword() {
        throw new UnsupportedOperationException("LdapUsers don't contain passwords in the current version.");
    }

    @Override
    public boolean isAccountNonLocked() {
        return isEnabled();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public void eraseCredentials() {
        // not necessary
        log.debug("Ignore attempt to erase credentials on ldap user {}", getUsername());
    }

    @Override
    public String getDn() {
        return dn;
    }

    @Override
    @Nonnull
    public User getJpa() {
        var jpaUser = new User(ident.nickname(), UserRealm.UNIHI, isEnabled(), getRole());
        jpaUser.setProfileConfiguration(new PersonalSettings(getSettings()));
        jpaUser.setExpirationDate(getExpireDate());
        jpaUser.setId(super.databaseId);
        jpaUser.setFullName(fullname);
        return jpaUser;
    }

    @Override
    public void updatePassword(@Nonnull ChangePasswordDto passwordDto, @Nonnull UserRole role) {
        //nothing
        log.debug("Ignore attempt to change password on ldapuser {}", getUsername());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof LdapUser)) {
            return false;
        }
        LdapUser other = (LdapUser) obj;
        return Objects.equals(dn, other.dn);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(dn);
        return result;
    }
}
