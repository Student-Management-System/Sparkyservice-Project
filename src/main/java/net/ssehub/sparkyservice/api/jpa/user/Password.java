package net.ssehub.sparkyservice.api.jpa.user;

import java.util.Optional;

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

import org.apache.commons.lang.builder.HashCodeBuilder;

import net.ssehub.sparkyservice.api.util.NullHelpers;

/**
 * Immutable JPA class for passwords. Although used in business logic for simplification. Make copys from it 
 * when bind them to a JPA user.
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
    
    @Nonnull
    @Column(nullable = false)
    private final String passwordString;
    
    /**
     * Default constructor needed by hibernate.
     */
    @Deprecated
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

    /**
     * Immutable instance of password.
     * 
     * @param passwordString
     * @param hashAlgorithm
     */
    public Password(String passwordString, @Nullable String hashAlgorithm) {
        this.passwordString = passwordString;
        this.hashAlgorithm = NullHelpers.notNull(
            Optional.ofNullable(hashAlgorithm).orElse(DEFAULT_HASH_ALGORITHM)
        );
    }

    /**
     * Copy constructor.
     * 
     * @param copyMe
     */
    public Password(Password copyMe) {
        this(copyMe.passwordString, copyMe.hashAlgorithm);
    }

    /**
     * Used algorithm for hashing the {@link #getPasswordString()}.
     * 
     * @return Hash algorithm in lowercase
     */
    public @Nonnull String getHashAlgorithm() {
        return NullHelpers.notNull(hashAlgorithm.toLowerCase());
    }

    /**
     * Password as string which is encoded with {@link #getHashAlgorithm}.
     * 
     * @return Password as string
     */
    public @Nonnull String getPasswordString() {
        return this.passwordString;
    }

    /**
     * Checks object fields for equality (not just the reference).
     */
    @Override
    public boolean equals(@Nullable Object object) {
        return Optional.ofNullable(object)
                .filter(Password.class::isInstance)
                .map(Password.class::cast)
                .filter(p -> hashAlgorithm.equalsIgnoreCase(p.getHashAlgorithm()))
                .filter(p -> passwordString.equals(p.getPasswordString()))
                .filter(p -> p.passwordId == passwordId)
                .isPresent();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(this.passwordId)
            .append(hashAlgorithm)
            .append(passwordString)
            .toHashCode();
    }
}

