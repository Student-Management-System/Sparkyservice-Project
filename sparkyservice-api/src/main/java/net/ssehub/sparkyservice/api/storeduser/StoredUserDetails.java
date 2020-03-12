package net.ssehub.sparkyservice.api.storeduser;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import net.ssehub.sparkyservice.db.user.Password;
import net.ssehub.sparkyservice.db.user.StoredUser;

/**
 * Class for authentication through Spring Security in a local realm.
 * 
 * @author marcel
 */
@ParametersAreNonnullByDefault
public class StoredUserDetails extends StoredUser implements UserDetails, GrantedAuthority {
    
    public static final String DEFAULT_REALM = "LOCAL";
    public static final String DEFAULT_ALGO = "BCRYPT";
    private static final long serialVersionUID = 1L;

    public static StoredUserDetails createStoredLocalUser(String userName, String rawPassword, boolean isActive) {
        var newUser = new StoredUserDetails(userName, null, DEFAULT_REALM, isActive);
        newUser.hashAndSetPassword(rawPassword);
        return newUser;
    }

       @Nullable
    private PasswordEncoder encoder;

    public StoredUserDetails(StoredUser userData) {
        super(userData);
    }

    private StoredUserDetails(String userName, @Nullable Password passwordEntity, String realm, boolean isActive) {
        super(userName, passwordEntity, realm, isActive, UserRole.DEFAULT.name());
    }

    @Nonnull
    public StoredUser getTransactionObject() {
        return new StoredUser(this.userName, this.passwordEntity, this.realm, this.isActive, this.role);
    }
    
    /**
     * Encodes a raw string to bcrypt hash sets the local password entity.
     * @param rawPassword
     * @return the hashed value - never null but may be empty
     */
    public String hashAndSetPassword(String rawPassword) {
        var passwordEntityLocal = passwordEntity;
        if (passwordEntityLocal!= null) {
            passwordEntityLocal.setPassword(getEncoder().encode(rawPassword));
            passwordEntityLocal.hashAlgorithm = DEFAULT_ALGO;
        } else {
            var encodedPass = getEncoder().encode(rawPassword);
            passwordEntityLocal = new Password(encodedPass, DEFAULT_ALGO);
        }
        setPasswordEntity(passwordEntityLocal);
        return getPassword();
    }
    
    public UserRole getUserRole() {
        return UserRole.valueOf(role);
    }
    
    public void setUserRole(UserRole role) {
        this.role = role.name();
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Arrays.asList(this);
    }

    /**
     * Returns the hashed password of the user. If the user is not in the default realm, it will return an empty string
     * return the stored password - never be null but may be empty if the user isn't in the default realm.
     */
    @Nonnull
    @Override
    public String getPassword() {
        final var passwordEntityLocal = passwordEntity;
        if (passwordEntityLocal != null) {
            return passwordEntityLocal.getPassword();
        } else {
            if (realm == DEFAULT_REALM) {
                throw new RuntimeException("User has no password even though he is in the default realm. This "
                        + "shouldn't be happen.");
            } else {
                return "";
            }
        }
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        // TODO Auto-generated method stub
        return isActive;
    }

    @Override
    public boolean isAccountNonLocked() {
        // TODO Auto-generated method stub
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // TODO Auto-generated method stub
        return isActive;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
    
    @Override
    public String getAuthority() {
        return this.getRole();
    }

    /**
     * Returns springs default {@link BCryptPasswordEncoder}. 
     * @return default instance of {@link BCryptPasswordEncoder}
     */
    @Nonnull
    public PasswordEncoder getEncoder() {
        PasswordEncoder encoder2 = encoder;
        if (encoder2 != null) {
            return encoder2;
        } else {
            encoder = new BCryptPasswordEncoder();
            return getEncoder();
        }
    }
}
