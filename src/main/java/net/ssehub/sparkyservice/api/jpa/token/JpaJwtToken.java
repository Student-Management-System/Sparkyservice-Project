package net.ssehub.sparkyservice.api.jpa.token;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;

/**
 * Provides the JPA representation of a JWT token. 
 * @author marcel
 */
@Entity
@Table(name = "jwt_issued_token")
@ParametersAreNonnullByDefault
public class JpaJwtToken {

    @Id
    @Column(nullable = false, length = 50)
    @Nonnull
    private String jti;

    @Column
    private int remainingRefreshes;
    
    @Column
    private boolean locked;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "id")
    @Nonnull
    private User user;

    /**
     * Constructor needed for Hibernate/Spring Data to initialize an empty copy.
     */
    @SuppressWarnings("unused")
    private JpaJwtToken() {
        jti = "UNKWN";
        locked = true;
        user = new User("UNKWN", UserRealm.UNKNOWN, false, UserRole.DEFAULT);
    }


    /**
     * A jwt token with JPA annotations. It can be saved to a storage.
     * 
     * @param jti Unique identifier / primary key
     * @param remainingRefreshes
     * @param locked Decides if the token is valid for auth or not
     * @param user The user the token is associated to
     */
    public JpaJwtToken(String jti, int remainingRefreshes, boolean locked, User user) {
        super();
        this.jti = jti;
        this.remainingRefreshes = remainingRefreshes;
        this.locked = locked;
        this.user = user;
    }

    /**
     * Identifies the JWT token. 
     * 
     * @param jit not null nor empty
     */
    public void setJti(String jti) {
        if (jti.isBlank()) {
            throw new IllegalArgumentException("JIT can't be empty");
        }
        this.jti = jti;
    }


    public int getRemainingRefreshes() {
        return remainingRefreshes;
    }


    public void setRemainingRefreshes(int remainingRefreshes) {
        this.remainingRefreshes = remainingRefreshes;
    }


    public boolean isLocked() {
        return locked;
    }


    public void setLocked(boolean locked) {
        this.locked = locked;
    }


    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    /**
     * .
     * @return The primary key of the token
     */
    @Nonnull
    public String getJti() {
        return jti;
    }

}
