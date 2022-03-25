package net.ssehub.sparkyservice.api.auth.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;

import net.ssehub.sparkyservice.api.auth.ldap.LdapContextMapper;
import net.ssehub.sparkyservice.api.user.UserRealm;

@Configuration
public class ProviderConfig {
    
    public class SingleSparkyProviderConfig {
        private final UserRealm supportedRealm;
        private final AuthenticationProvider provider;
        private final int weight;
        
        public SingleSparkyProviderConfig(UserRealm supportedRealm, AuthenticationProvider provider, int priorityWeight) {
            this.weight = priorityWeight; // TODO test for this
            this.supportedRealm = supportedRealm;
            this.provider = provider;
        }
        
        public boolean supports(UserRealm realm) {
            return realm == supportedRealm;
        }

        public int getWeight() {
            return weight;
        }

        public AuthenticationProvider getProvider() {
            return provider;
        }
    }
    
    @Value("${ldap.url:}")
    private String ldapUrl;

    @Value("${ldap.basedn:}")
    private String ldapBaseDn;

    @Value("${ldap.user:}")
    private String ldapUser;

    @Value("${ldap.password:}")
    private String ldapPassword;

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
    
    @Bean
    public LdapContextSource contextSource() {
        var ldapSource= new DefaultSpringSecurityContextSource(ldapUrl + "/" + ldapBaseDn);
        if (!ldapUser.isBlank() && !ldapPassword.isBlank()) {
            ldapSource.setPassword(ldapPassword);
            ldapSource.setUserDn(ldapUser);
        }
        return ldapSource;
    }
    
    @Bean("ldapAuthProvider")
    @ConditionalOnExpression("${ldap.enabled:false} and !${ldap.ad:false}")
    public SingleSparkyProviderConfig authenticationProvider(LdapAuthenticator authenticator) {
        var prov = new LdapAuthenticationProvider(authenticator);
        prov.setUserDetailsContextMapper(ldapContextMapper);
        return new SingleSparkyProviderConfig(UserRealm.UNIHI, prov, 3);
    }

    @Bean
    public BindAuthenticator authenticator(BaseLdapPathContextSource contextSource) {
        String searchBase = "";
        FilterBasedLdapUserSearch search =
            new FilterBasedLdapUserSearch(searchBase, ldapUserDnPattern, contextSource);
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserSearch(search);
        return authenticator;
    }

    @ConditionalOnProperty(value = "recovery.enabled", havingValue = "true")
    @Bean("memoryAuthProvider")
    public SingleSparkyProviderConfig memoryAuthProvider() {
        var prov = new DaoAuthenticationProvider(); 
        prov.setUserDetailsService(memoryDetailsService);
        prov.setPasswordEncoder(pwEncoder);
        return new SingleSparkyProviderConfig(UserRealm.RECOVERY, prov, 3);
    }

    @Bean("localDbAuthProvider")
    public SingleSparkyProviderConfig localDbAuthProvider() {
        var prov = new DaoAuthenticationProvider();
        prov.setUserDetailsService(localDetailsMapper);
        prov.setPasswordEncoder(pwEncoder);
        return new SingleSparkyProviderConfig(UserRealm.LOCAL, prov, 2);
    }

    @Bean("adLdapAuthProvider")
    @ConditionalOnProperty(value = "ldap.ad", havingValue = "true")
    public SingleSparkyProviderConfig adLdapAuthProvider() {
        ActiveDirectoryLdapAuthenticationProvider adProvider = new ActiveDirectoryLdapAuthenticationProvider(ldapBaseDn,
                ldapUrl);
        adProvider.setConvertSubErrorCodesToExceptions(true);
        adProvider.setUseAuthenticationRequestCredentials(true);
        adProvider.setUserDetailsContextMapper(ldapContextMapper);
        if (ldapUserDnPattern != null && ldapUserDnPattern.trim().length() > 0) {
            adProvider.setSearchFilter(ldapUserDnPattern);
        }
        return new SingleSparkyProviderConfig(UserRealm.UNIHI, adProvider, 1);
    }
}
