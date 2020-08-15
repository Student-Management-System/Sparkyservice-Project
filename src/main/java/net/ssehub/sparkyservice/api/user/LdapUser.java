package net.ssehub.sparkyservice.api.user;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nonnull;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapUserDetails;

import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;

public class LdapUser extends User implements LdapUserDetails {
    
    private static final long serialVersionUID = -2155556837850826196L;

    private String dn; 

    public LdapUser(@Nonnull String userName, @Nonnull UserRole role, boolean isEnabled, int timeUntilExpiration) {
        super(userName, null, UserRealm.LDAP, isEnabled, role);
        // TODO Exp time
    }

    public LdapUser(@Nonnull String userName, @Nonnull UserRole role, boolean isEnabled) {
        super(userName, null, UserRealm.LDAP, isEnabled, role);
    }

    public LdapUser(@Nonnull User user) {
        super(user);
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return super.getUserName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return super.getExpirationDate().map(LocalDate.now()::isBefore).orElse(false);
    }

    @Override
    public boolean isAccountNonLocked() {
        return super.isActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false; // not implemented
    }

    @Override
    public boolean isEnabled() {
        return super.isActive();
    }

    @Override
    public void eraseCredentials() {
        
    }

    @Override
    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public void setTimeBeforeExpiration(int time) {
        
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Arrays.asList(getRole());
    }
}
