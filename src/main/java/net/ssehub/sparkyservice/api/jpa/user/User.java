package net.ssehub.sparkyservice.api.jpa.user;

import java.time.LocalDate;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Represents a user with JPA annotations.
 *
 * @author marcel
 */

@Entity
@Table(name = "user_stored", uniqueConstraints = { @UniqueConstraint(columnNames = { "userName", "realm" }) })
@ParametersAreNonnullByDefault
public class User {

    /**
     * Unique identifier (primary key) for local user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected int id;

    @Nonnull
    @Column(nullable = false, length = 50)
    protected String userName;

    @Nullable
    @Column(length = 255)
    protected String fullName;

    @Column
    protected boolean isActive;

    @OneToOne(cascade = { CascadeType.ALL })
    @Nullable
    protected Password passwordEntity;

    @Nonnull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    protected UserRealm realm;

    @Nonnull
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    protected UserRole role;

    @OneToOne(cascade = {
            CascadeType.ALL }, targetEntity = net.ssehub.sparkyservice.api.jpa.user.PersonalSettings.class)
    @Nonnull
    protected PersonalSettings profileConfiguration;

    @Nullable
    @Column
    protected java.sql.Date expirationTime;

    /**
     * Default constructor used by hibernate.
     */
    @SuppressWarnings("unused")
    private User() {
        role = UserRole.DEFAULT;
        userName = "";
        realm = UserRealm.UNKNOWN;
        profileConfiguration = new PersonalSettings();
    }

    public User(String userName, @Nullable Password passwordEntity, UserRealm realm, boolean isActive, UserRole role) {
        this.userName = userName;
        this.passwordEntity = passwordEntity;
        this.realm = realm;
        this.isActive = isActive;
        this.role = role;
        this.profileConfiguration = new PersonalSettings();
    }

    public User(final User user) {
        this.id = user.id;
        this.realm = user.realm;
        this.role = user.role;
        this.userName = user.userName;
        this.isActive = user.isActive;
        this.passwordEntity = user.passwordEntity;
        this.fullName = user.fullName;
        this.profileConfiguration = user.profileConfiguration;
        this.expirationTime = user.expirationTime;
    }

    /**
     * Users database ID for unique identification of that entry. 
     * 
     * @return Primary key
     */
    public int getId() {
        return id;
    }

    /**
     * Users database ID for unqiue identification of that entry. 
     * Typically set through OR mapper.
     * 
     * @param id Primary key
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Is unique per realm and is never null or empty. It can be used as identifier
     * in combination with the realm.
     * 
     * @return name of the user which is unique per realm
     */
    @Nonnull
    public String getUserName() {
        return this.userName;
    }

    /**
     * Full name of the user. Can be null and is may not be unique.
     * @return First + Last Name of the user
     */
    @Nullable
    public String getFullName() {
        return fullName;
    }

    /**
     * Setter for the full name of the user. Should contain first and last name.
     * @param fullName string
     */
    public void setFullName(@Nullable String fullName) {
        this.fullName = fullName;
    }

    /**
     * Overrides the old username - it have to pe unique per realm and max length
     * 50.
     * 
     * @param userName unique string per realm
     */
    public void setUserName(String userName) {
        if (userName.length() > 50) {
            throw new IllegalArgumentException("The max length for a username is 50");
        }
        this.userName = userName;
    }

    /**
     * Indicator if the user is currently enabled. When set to false, the is user is not able to authenticate 
     * himself anymore.
     * 
     * @return - Boolean if the user is enabled or not. 
     */
    public boolean isActive() {
        return isActive;
    }

    
    /**
     * Indicator if the user is currently enabled. When set to false, the is user is not able to authenticate 
     * himself anymore.
     * 
     * @param isActive
     */
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Entity which contains information about the current password. Can be null if the user has no password.
     * 
     * @return PasswordEntity
     */
    @Nullable
    public Password getPasswordEntity() {
        return passwordEntity;
    }

    public void setPasswordEntity(@Nullable Password password) {
        this.passwordEntity = password;
    }

    /**
     * Sets a single authority role to the user. Old role will be overridden. Null values will be ignored
     * 
     * @param role Users permission role
     */
    public void setRole(@Nullable UserRole role) {
        if (role != null) {
            this.role = role;
        }
    }

    /**
     * {@link PersonalSettings} of the user where extra settings are stored like
     * email addresses.
     * 
     * @return associated profile of this StoredUser - if no exists, a new will be
     *         generated
     */
    @Nonnull
    public PersonalSettings getProfileConfiguration() {
        return profileConfiguration;
    }
    
    public Optional<java.sql.Date> getExpirationDate() {
        return Optional.ofNullable(expirationTime);
    }

    public void setExpirationDate(@Nullable java.sql.Date expirationDate) {
        this.expirationTime = expirationDate;
    }

    public void setExpirationDate(Optional<LocalDate> expirationDate) {
        this.expirationTime = expirationDate.map(java.sql.Date::valueOf).orElse(null);
    }

    public void setProfileConfiguration(PersonalSettings profileConfiguration) {
        this.profileConfiguration = profileConfiguration;
    }

    public UserRealm getRealm() {
        return realm;
    }

    public void setRealm(UserRealm realm) {
        this.realm = realm;
    }

    @Nonnull
    public UserRole getRole() {
        return role;
    }
}
