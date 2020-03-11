package net.ssehub.sparkyservice.api.storeduser;

import java.util.Arrays;
import java.util.Collection;

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
public class StoredUserDetails extends StoredUser implements UserDetails, GrantedAuthority {
    
    public static final String DEFAULT_REALM = "LOCAL";
    public static final String DEFAULT_ALGO = "BCRYPT";
    private static final long serialVersionUID = 1L;

    public static StoredUserDetails createStoredLocalUser(String userName, String rawPassword, boolean isActive) {
        var newUser = new StoredUserDetails(userName, null, DEFAULT_REALM, isActive);
        newUser.hashAndSetPassword(rawPassword);
        return newUser;
    }

    private PasswordEncoder encoder;

    public StoredUserDetails(StoredUser userData) {
        super(userData);
    }

    private StoredUserDetails(String userName, Password passwordEntity, String realm, boolean isActive) {
        super(userName, passwordEntity, realm, isActive, UserRole.DEFAULT.name());
    }

    public StoredUser getTransactionObject() {
        return new StoredUser(this.userName, this.passwordEntity, this.realm, this.isActive, this.role);
    }
    
    private void initPasswordEncoder() {
        if (encoder == null) {
            encoder = new BCryptPasswordEncoder();
        }
    }
    
    public String hashAndSetPassword(String rawPassword) {
        initPasswordEncoder();
        if (passwordEntity == null) {
            var encodedPass = encoder.encode(rawPassword);
            var password = new Password(encodedPass, DEFAULT_ALGO);
            this.passwordEntity = password;
        } else {
            this.passwordEntity.setPassword(encoder.encode(rawPassword));
            this.passwordEntity.hashAlgorithm = DEFAULT_ALGO;
        }
        return this.passwordEntity.getPassword();
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

    @Override
    public String getPassword() {
        return passwordEntity.getPassword();
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
}
