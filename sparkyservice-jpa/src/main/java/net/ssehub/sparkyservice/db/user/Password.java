package net.ssehub.sparkyservice.db.user;

import java.io.UnsupportedEncodingException;

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

import net.ssehub.sparkyservice.db.hibernate.AnnotatedClass;

@Entity
@Table(name = "user_local_password")
@ParametersAreNonnullByDefault
public class Password implements AnnotatedClass {
    
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
    protected String hashAlgorithm;
    
    @OneToOne
    @Nullable
    protected StoredUser user;
    
    /**
     * 
     */
    @Nonnull
    @Column(nullable = false)
    private String passwordString;
    
    /**
     * Default constructor needed by hibernate.
     */
    public Password() {
        this("");
    }

    /**
     * Takes a plain text password and hash it with the default password algorithm.
     * @param plainTextPassword
     * @throws UnsupportedEncodingException 
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

    public void setHashAlgorithm(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    public void setPasswordString(String passwordHash) {
        this.passwordString = passwordHash;
    }

    public @Nonnull String getPasswordString() {
        return this.passwordString;
    }
}

