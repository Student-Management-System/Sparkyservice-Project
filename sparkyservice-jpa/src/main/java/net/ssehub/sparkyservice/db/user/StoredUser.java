package net.ssehub.sparkyservice.db.user;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import net.ssehub.sparkyservice.db.hibernate.AnnotatedClass;

@Entity
@Table(
    name = "user_stored", 
    uniqueConstraints = {@UniqueConstraint(columnNames = {"userName", "realm"})})
@ParametersAreNonnullByDefault
public class StoredUser implements AnnotatedClass {

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
    
    @OneToOne(cascade = {CascadeType.ALL})
    @Nullable
    protected Password passwordEntity;

    @Nonnull
    @Column(nullable = false, length = 50)
    protected String realm;
    
    @Nonnull
    @Column(nullable = false)
    protected String role;

    @OneToOne(cascade = {CascadeType.ALL}, targetEntity = net.ssehub.sparkyservice.db.user.PersonalSettings.class)
    protected PersonalSettings profileConfiguration;

    /**
     * Default constructor used by hibernate.
     */
    protected StoredUser() {
        this.realm = "";
        this.role = "";
        this.userName = "";
    }
        
    public StoredUser(String userName, 
            @Nullable Password passwordEntity, 
            String realm, 
            boolean isActive, 
            String role) {
        this.userName = userName;
        this.passwordEntity = passwordEntity;
        this.realm = realm;
        this.isActive = isActive;
        this.role = role;
        this.profileConfiguration = new PersonalSettings();
    }

    public StoredUser(final StoredUser user) {
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
     * Is unique per realm and is never null or empty. It can be used as identifier in combination with the realm.
     * 
     * @return name of the user which is unique per realm
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * Overrides the old username - it have to pe unique per realm and max length 50.
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
     * @param the authentication realm of the user. 
     */
    public String getRealm() {
        return realm;
    }

    /**
     * @param realm name with max character size of 50
     */
    public void setRealm(String realm) {
        if (realm.length() > 50) {
            throw new IllegalArgumentException("");
        }
        this.realm = realm;
    }
    
    /**
     * Single authority role of the user.Only one role at a time is supported.
     * 
     * @return name of a role
     */
    public String getRole() {
        return role;
    }
    
    /**
     * Sets a single authority role to the user. Old role will be overridden.
     * 
     * @param role name of a role
     */
    public void setRole(String role) {
        this.role = role;
    }
    
    /**
     * {@link PersonalSettings} of the user where extra settings are stored like email addressees.
     * 
     * @return associated profile of this StoredUser - if no exists, a new will be generated
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
}
