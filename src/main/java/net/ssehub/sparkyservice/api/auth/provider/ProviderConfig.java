package net.ssehub.sparkyservice.api.auth.provider;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;

import net.ssehub.sparkyservice.api.auth.ldap.LdapContextMapper;
import net.ssehub.sparkyservice.api.user.LocalRealm;
import net.ssehub.sparkyservice.api.user.MemoryRealm;

@Lazy
@Configuration
@ParametersAreNonnullByDefault
public class ProviderConfig {
    public static final String LDAP_PROVIDER_BEANNAME = "ldapAuthProvider";
    
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

    @Autowired
    private PasswordEncoder pwEncoder;
    
    @Bean
    @ConditionalOnExpression("${ldap.enabled:false} and !${ldap.ad:false}")
    public LdapContextSource contextSource() {
        var ldapSource= new DefaultSpringSecurityContextSource(ldapUrl + "/" + ldapBaseDn);
        if (!ldapUser.isBlank() && !ldapPassword.isBlank()) {
            ldapSource.setPassword(ldapPassword);
            ldapSource.setUserDn(ldapUser);
        }
        return ldapSource;
    }
    
    @Bean(LDAP_PROVIDER_BEANNAME)
    @ConditionalOnExpression("${ldap.enabled:false} and !${ldap.ad:false}")
    public AuthenticationProvider authenticationProvider(LdapContextMapper mapper, BaseLdapPathContextSource contextSource) {
        String searchBase = "";
        FilterBasedLdapUserSearch search =
            new FilterBasedLdapUserSearch(searchBase, ldapUserDnPattern, contextSource);
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserSearch(search);
        
        var prov = new LdapAuthenticationProvider(authenticator);
        prov.setUserDetailsContextMapper(mapper);
        return prov;
    }
    
    @Bean(LDAP_PROVIDER_BEANNAME)
    @ConditionalOnProperty(value = "ldap.ad", havingValue = "true")
    public AuthenticationProvider adLdapAuthProvider(LdapContextMapper mapper) {
        ActiveDirectoryLdapAuthenticationProvider adProvider = new ActiveDirectoryLdapAuthenticationProvider(ldapBaseDn,
                ldapUrl);
        adProvider.setConvertSubErrorCodesToExceptions(true);
        adProvider.setUseAuthenticationRequestCredentials(true);
        adProvider.setUserDetailsContextMapper(mapper);
        if (ldapUserDnPattern != null && ldapUserDnPattern.trim().length() > 0) {
            adProvider.setSearchFilter(ldapUserDnPattern);
        }
        return adProvider;
    }

    @ConditionalOnProperty(value = "recovery.enabled", havingValue = "true")
    @Bean("memoryAuthProvider")
    public AuthenticationProvider memoryAuthProvider(StorageDetailsService detailsService, MemoryRealm realm) {
        var prov = new DaoAuthenticationProvider(); 
        UserDetailsService memoryService = nickname -> detailsService.loadUser(nickname, realm);
        prov.setUserDetailsService(memoryService);
        prov.setPasswordEncoder(pwEncoder);
        return prov;
    }

    @Bean("localDbAuthProvider")
    public AuthenticationProvider localDbAuthProvider(StorageDetailsService detailsService, LocalRealm realm) {
        var prov = new DaoAuthenticationProvider();
        UserDetailsService dbService = nickname -> detailsService.loadUser(nickname, realm);
        prov.setUserDetailsService(dbService);
        prov.setPasswordEncoder(pwEncoder);
        return prov;
    }
}
