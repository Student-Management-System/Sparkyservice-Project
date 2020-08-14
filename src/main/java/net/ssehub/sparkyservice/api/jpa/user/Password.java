package net.ssehub.sparkyservice.api.jpa.user;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Immutable JPA class for passwords.
 * 
 * @author marcel
 */
@Entity
@Table(name = "user_local_password")
@ParametersAreNonnullByDefault
public class Password {
    
    /**
     * This algorithm value is saved to database if no other was provided. 
     */
    protected static final String DEFAULT_HASH_ALGORITHM = "PLAIN";
    
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected int passwordId;

    @Nonnull
    @Column
    protected final String hashAlgorithm;
    
    @OneToOne
    @Nullable
    protected User user;
    
    /**
     * 
     */
    @Nonnull
    @Column(nullable = false)
    private final String passwordString;
    
    /**
     * Default constructor needed by hibernate.
     */
    public Password() {
        this("");
    }

    /**
     * Takes a plain text password and hash it with the default password algorithm.
     * 
     * @param passwordString - A hashed password as string
     */
    public Password(String passwordString) {
        this.passwordString = passwordString;
        this.hashAlgorithm = DEFAULT_HASH_ALGORITHM;
    }

    public Password(String passwordString, String hashAlgorithm) {
        this.passwordString = passwordString;
        this.hashAlgorithm = hashAlgorithm;
    }

    /**
     * Used algorithm for hashing the @link passworString}; 
     */
    public @Nonnull String getHashAlgorithm() {
        return hashAlgorithm;
    }

    /**
     * Password as string which is encoded with {@link #getHashAlgorithm}
     * 
     * @return Password as string
     */
    public @Nonnull String getPasswordString() {
        return this.passwordString;
    }
}

