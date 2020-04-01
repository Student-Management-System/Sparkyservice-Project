package net.ssehub.sparkyservice.api.jpa.user;

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

import net.ssehub.sparkyservice.api.user.UserServiceImpl;

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
    protected PersonalSettings profileConfiguration;

    /**
     * Default constructor used by hibernate.
     */
    @SuppressWarnings("unused")
    private User() {
        role = UserRole.DEFAULT;
        userName = "";
        realm = UserRealm.UNKNOWN;
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
        this.profileConfiguration = user.profileConfiguration;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Is unique per realm and is never null or empty. It can be used as identifier
     * in combination with the realm.
     * 
     * @return name of the user which is unique per realm
     */
    public String getUserName() {
        return this.userName;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    @Nullable
    public Password getPasswordEntity() {
        return passwordEntity;
    }

    public void setPasswordEntity(@Nullable Password password) {
        this.passwordEntity = password;
    }

    /**
     * Sets a single authority role to the user. Old role will be overridden.
     * 
     * @param role Users permission role
     */
    public void setRole(UserRole role) {
        this.role = role;
    }

    /**
     * {@link PersonalSettings} of the user where extra settings are stored like
     * email addressees.
     * 
     * @return associated profile of this StoredUser - if no exists, a new will be
     *         generated
     */
    @Nonnull
    public PersonalSettings getProfileConfiguration() {
        final var profileConfiguration2 = profileConfiguration;
        if (profileConfiguration2 != null) {
            return profileConfiguration2;
        } else {
            final var conf = new PersonalSettings();
            profileConfiguration = conf;
            return conf;
        }
    }

    public void setProfileConfiguration(@Nullable PersonalSettings profileConfiguration) {
        this.profileConfiguration = profileConfiguration;
    }

    public UserRealm getRealm() {
        return realm;
    }

    public void setRealm(UserRealm realm) {
        this.realm = realm;
    }

    public UserRole getRole() {
        return role;
    }

    /**
     * Checks if the user is already stored in the database. When this is false and
     * a store operation is invoked, the user will be created. Otherwise his data
     * would be changed. This method does not perform any database action. To be
     * sure that this user is or is not in the database consider using
     * {@link UserServiceImpl#isUserInDatabase(User)}.
     * 
     * @return true if the user is already stored in the database, false otherwise.
     */
    public boolean isStored() {
        return this.id != 0;
    }
}
