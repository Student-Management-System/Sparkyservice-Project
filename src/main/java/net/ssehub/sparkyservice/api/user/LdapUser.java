package net.ssehub.sparkyservice.api.user;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.springframework.security.ldap.userdetails.LdapUserDetails;

import net.ssehub.sparkyservice.api.jpa.user.PersonalSettings;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.dto.UserDto.ChangePasswordDto;

/**
 * Represents a user which is in the local realm.
 * 
 * @author marcel
 */
public class LdapUser extends AbstractSparkyUser implements SparkyUser, LdapUserDetails {

    private static final long serialVersionUID = -2155556837850826196L;

    private String dn;

    /**
     * Creates a user with necessary fields.
     * 
     * @param username
     * @param role
     * @param isEnabled
     * @see #create(String, UserRole, boolean)
     */
    LdapUser(@Nonnull String username, @Nonnull UserRole role, boolean isEnabled) {
        super(username, role);
        setEnabled(isEnabled);
    }

    /**
     * Copy constructor of another SparkyUser.
     * 
     * @param copyMe
     * @see #create(String, UserRole, boolean)
     */
    LdapUser(SparkyUser copyMe) {
        this(copyMe.getUsername(), copyMe.getRole(), copyMe.isEnabled());
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
        this(jpaUser.getUserName(), jpaUser.getRole(), jpaUser.isActive());
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
    }

    @Override
    public String getDn() {
        return dn;
    }

    @Override
    @Nonnull
    public User getJpa() {
        var jpaUser = new User(getUsername(), UserRealm.LDAP, isEnabled(), getRole());
        jpaUser.setProfileConfiguration(new PersonalSettings(getSettings()));
        jpaUser.setExpirationDate(getExpireDate());
        jpaUser.setId(super.databaseId);
        jpaUser.setFullName(fullname);
        return jpaUser;
    }

    @Override
    @Nonnull
    public UserRealm getRealm() {
        return UserRealm.LDAP;
    }

    @Override
    public void updatePassword(@Nonnull ChangePasswordDto passwordDto, @Nonnull UserRole role) {
        //nothing
    }

    @Override
    public boolean equals(Object object) {
        Optional<LdapUser> optDn = Optional.ofNullable(object).flatMap(obj -> super.equalsCheck(obj, this));
        if (dn == null) {
            return optDn.map(u -> u.getDn() == null).orElse(false);
        } else {
            return optDn.map(u -> u.getDn()).filter(dn::equals).isPresent();
        }
    }

    @Override
    public int hashCode() {
        return getHashCodeBuilder().append(dn).toHashCode();
    }
}
