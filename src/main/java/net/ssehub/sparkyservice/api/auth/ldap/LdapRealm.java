package net.ssehub.sparkyservice.api.auth.ldap;

import static net.ssehub.sparkyservice.api.auth.ldap.LdapProvider.LDAP_PROVIDER_BEANNAME;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import net.ssehub.sparkyservice.api.auth.identity.SparkyUser;
import net.ssehub.sparkyservice.api.auth.identity.SparkyUserFactory;
import net.ssehub.sparkyservice.api.auth.identity.UserRealm;

@Component
@ConditionalOnProperty(
    value = "ldap.enabled", 
    havingValue = "true", 
    matchIfMissing = false)
@ParametersAreNonnullByDefault
public class LdapRealm implements UserRealm {

    @Nonnull
    private final SparkyUserFactory<LdapUser> factory;

    @Nonnull
    private final AuthenticationProvider provider;

    @Override
    public @Nonnull String identifierName() {
        return "LDAP";
    }

    @Override
    public @Nonnull String publicName() {
        return "uni-hildesheim.de";
    }

    @Autowired
    private LdapRealm(@Lazy @Qualifier(LDAP_PROVIDER_BEANNAME) AuthenticationProvider provider) {
        this.provider = provider;
        factory = new LdapUserFactory(this);
    }
    
    /**
     * Constructor only for test cases!
     */
    public LdapRealm() {
        this(new AuthenticationProvider() {
            
            @Override
            public boolean supports(@Nullable Class<?> authentication) {
                return false;
            }
            
            @Override
            public Authentication authenticate(@Nullable Authentication authentication) throws AuthenticationException {
                return authentication;
            }
        });
    }

    @Override
    public @Nonnull SparkyUserFactory<? extends SparkyUser> userFactory() {
        return factory;
    }

    @Override
    public @Nonnull AuthenticationProvider authenticationProvider() {
        return provider;
    }

    @Override
    public int authenticationPriorityWeight() {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this.nameEquals(obj);
    }

}
