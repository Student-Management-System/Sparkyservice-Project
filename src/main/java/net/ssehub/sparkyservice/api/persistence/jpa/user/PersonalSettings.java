package net.ssehub.sparkyservice.api.persistence.jpa.user;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.apache.commons.lang.builder.HashCodeBuilder;

import net.ssehub.sparkyservice.api.auth.identity.SparkyUser;
import net.ssehub.sparkyservice.api.useraccess.dto.SettingsDto;

/**
 * Provides a JPA representation of account settings. <br>
 * In the current state of this application, this object is always used as generel representation of settings in
 * the whole project because it does not contain any logic. 
 * Disadvantage of this is, that it is hard to keep in sync with the database. Instances of this object must be 
 * bound to a {@link User}. When saving an instance to a storage, do not use it outside of the bounded user object 
 * (for example in {@link SparkyUser}). 
 * <br><br>
 * Strictly separate the use cases and create a new instance for each of them.
 * 
 * @author marcel
 */
@Entity
@Table(name = "user_configuration")
public class PersonalSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int configurationId;

    @OneToOne
    @PrimaryKeyJoinColumn
    private User user; 

    @Column
    private boolean emailReceive = false;

    @Column
    private String emailAddress;
    
    @Column
    private boolean wantsAi = false;

    @Column(name = "CONTENT", length = 512)
    private String payload;

    public PersonalSettings() {
    }

    public PersonalSettings(PersonalSettings copyMe) {
        this();
        emailAddress = copyMe.emailAddress;
        emailReceive = copyMe.emailReceive;
        wantsAi = copyMe.wantsAi;
        payload = copyMe.payload;
    }
    
    public PersonalSettings(SettingsDto dto) {
        emailAddress = dto.emailAddress;
        emailReceive = dto.emailReceive;
        wantsAi = dto.wantsAi;
        payload = dto.payload;
    }

    public int getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(int configurationId) {
        this.configurationId = configurationId;
    }

    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public boolean isEmailReceive() {
        return emailReceive;
    }
    
    public void setEmailReceive(boolean emailReceive) {
        this.emailReceive = emailReceive;
    }
    
    public String getEmailAddress() {
        return emailAddress;
    }
    
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
    
    public boolean isWantsAi() {
        return wantsAi;
    }
    
    public void setWantsAi(boolean wantsAi) {
        this.wantsAi = wantsAi;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    /*
     * Auto generated with eclipse.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PersonalSettings other = (PersonalSettings) obj;
        if (configurationId != other.configurationId) {
            return false;
        }
        if (emailAddress == null) {
            if (other.emailAddress != null) {
                return false;
            }
        } else if (!emailAddress.equals(other.emailAddress)) {
            return false;
        }
        if (emailReceive != other.emailReceive) {
            return false;
        }
        if (payload == null) {
            if (other.payload != null) {
                return false;
            }
        } else if (!payload.equals(other.payload)) {
            return false;
        }
        if (user == null) {
            if (other.user != null) {
                return false;
            }
        } else if (!user.equals(other.user)) {
            return false;
        }
        if (wantsAi != other.wantsAi) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(emailReceive)
            .append(wantsAi)
            .append(configurationId)
            .append(emailReceive)
            .toHashCode();
    }
}
