package net.ssehub.sparkyservice.api.user.extraction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.security.core.Authentication;

import net.ssehub.sparkyservice.api.user.SparkyUser;

/**
 * Provides extraction methods to create a user object from other information.
 * 
 * @author marcel
 */
public interface UserExtractionService {

    /**
     * Tries to extract information from an authentication object and create an usable {@link SparkyUser} of it. 
     * There is no guarantee that the information match with those from a storage since here shouldn't be done
     * any storage operations. 
     * 
     * @param auth
     * @return User extracted from the authentication (without validating information from a storage)
     * @throws MissingDataException
     */
    @Nonnull
    @Deprecated(forRemoval = true)
    // TODO remove in the future
    SparkyUser extract(@Nullable Authentication auth);
}
