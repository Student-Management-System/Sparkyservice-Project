package net.ssehub.sparkyservice.api.auth.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;

import net.ssehub.sparkyservice.api.auth.ldap.LdapContextMapper;
import net.ssehub.sparkyservice.api.user.UserRealm;

@Configuration
public class ProviderConfig {
    
    public class SparkyProvider implements AuthenticationProvider {
        private final UserRealm supportedRealm;
        private final AuthenticationProvider provider;
        private final int weight;
        
        public SparkyProvider(UserRealm supportedRealm, AuthenticationProvider provider, int priorityWeight) {
            this.weight = priorityWeight; // TODO test for this
            this.supportedRealm = supportedRealm;
            this.provider = provider;
        }

        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            return provider.authenticate(authentication);
        }
        
        @Override
        public boolean supports(Class<?> authentication) {
            return provider.supports(authentication);
        }
        
        public boolean supports(UserRealm realm) {
            return realm == supportedRealm;
        }

        public int getWeight() {
            return weight;
        }
    }
    
    @Value("${ldap.url:}")
    private String ldapUrl;

    @Value("${ldap.basedn:}")
    private String ldapBaseDn;

    @Value("${ldap.username:}")
    private String ldapSecurityPrincipal;

    @Value("${ldap.password:}")
    private String ldapPrincipalPassword;

    @Value("${ldap.userdn:}")
    private String ldapUserDnPattern;

    @Value("${ldap.enabled:false}")
    private boolean ldapEnabled;

    @Value("${ldap.ad:false}")
    private boolean ldapAd;

    @Value("${recovery.enabled:false}")
    private boolean inMemoryEnabled;

    @Value("${recovery.password:}")
    private String inMemoryPassword;

    @Value("${recovery.user:user}")
    private String inMemoryUser;
    
    @Autowired
    private LdapContextMapper ldapContextMapper;
    
    @Autowired
    private MemoryDetailsService memoryDetailsService;

    @Autowired
    private LocalDbDetailsMapper localDetailsMapper;
    
    @Autowired
    private PasswordEncoder pwEncoder;
    
    // TODO enable normal LDAP login
//    @Bean("ldapAuthProvider")
//    @ConditionalOnProperty(value = "ldap.enabled", havingValue = "true")
//    public LdapAuthenticationProvider authenticationProvider(LdapAuthenticator authenticator) {
//        return new LdapAuthenticationProvider(authenticator);
//    }
//
//    @Bean
//    public BindAuthenticator authenticator(BaseLdapPathContextSource contextSource) {
//        String searchBase = "ou=people";
//        String filter = "(uid={0})";
//        FilterBasedLdapUserSearch search =
//            new FilterBasedLdapUserSearch(searchBase, filter, contextSource);
//        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
//        authenticator.setUserSearch(search);
//        return authenticator;
//    }
//    

    @ConditionalOnProperty(value = "recovery.enabled", havingValue = "true")
    @Bean("memoryAuthProvider")
    public SparkyProvider memoryAuthProvider() {
        var prov = new DaoAuthenticationProvider(); 
        prov.setUserDetailsService(memoryDetailsService);
        prov.setPasswordEncoder(pwEncoder);
        return new SparkyProvider(UserRealm.RECOVERY, prov, 3);
    }

    @Bean("localDbAuthProvider")
    public SparkyProvider localDbAuthProvider() {
        var prov = new DaoAuthenticationProvider();
        prov.setUserDetailsService(localDetailsMapper);
        prov.setPasswordEncoder(pwEncoder);
        return new SparkyProvider(UserRealm.LOCAL, prov, 2);
    }

    @Bean("adLdapAuthProvider")
    @ConditionalOnProperty(value = "ldap.ad.enabled", havingValue = "true")
    public SparkyProvider adLdapAuthProvider() {
        ActiveDirectoryLdapAuthenticationProvider adProvider = new ActiveDirectoryLdapAuthenticationProvider(ldapBaseDn,
                ldapUrl);
        adProvider.setConvertSubErrorCodesToExceptions(true);
        adProvider.setUseAuthenticationRequestCredentials(true);
        adProvider.setUserDetailsContextMapper(ldapContextMapper);
        if (ldapUserDnPattern != null && ldapUserDnPattern.trim().length() > 0) {
            adProvider.setSearchFilter(ldapUserDnPattern);
        }
        return new SparkyProvider(UserRealm.UNIHI, adProvider, 1);
    }
}
