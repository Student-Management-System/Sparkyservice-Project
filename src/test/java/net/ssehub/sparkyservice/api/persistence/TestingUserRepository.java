package net.ssehub.sparkyservice.api.persistence;

import org.springframework.context.annotation.Primary;

/**
 * Wrapper for {@link UserRepository} to make it available for test cases.
 * 
 * @author marcel
 */
@Primary
public interface TestingUserRepository extends UserRepository {

}
