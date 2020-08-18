package net.ssehub.sparkyservice.api.user.storage;

import java.util.List;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.util.SparkyUtil;
import net.ssehub.sparkyservice.api.util.NullHelpers;

/**
 * User service implementation only for users which have the role {@link UserRole#SERVICE}.
 * 
 * @author marcel
 */
@Service
public class ServiceAccStorageService {

    @Autowired
    private UserRepository repository;

    /**
     * Finds all Service Accounts.
     * 
     * @return List of service accounts
     */
    @Nonnull
    public List<User> findAllServiceAccounts() {
        return SparkyUtil.toList(
                NullHelpers.notNull(repository.findByRole(UserRole.SERVICE))
        );
    }
}
