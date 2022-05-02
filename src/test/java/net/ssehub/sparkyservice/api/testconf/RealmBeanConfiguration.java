package net.ssehub.sparkyservice.api.testconf;

import java.util.List;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import net.ssehub.sparkyservice.api.user.LdapRealm;
import net.ssehub.sparkyservice.api.user.LocalRealm;
import net.ssehub.sparkyservice.api.user.MemoryRealm;
import net.ssehub.sparkyservice.api.user.RealmRegistry;
import net.ssehub.sparkyservice.api.user.UserRealm;

@TestConfiguration
public class RealmBeanConfiguration {

    @Bean("defaultRealm")
    public LocalRealm localRealm() {
        return new LocalRealm();
    }
    
    @Bean
    public MemoryRealm memoryRealm() {
        return new MemoryRealm();
        
    }
    
    @Bean
    public LdapRealm ldapRealm() {
        return new LdapRealm();
    }
    
    @Bean
    public RealmRegistry registrySetup(List<UserRealm> realms) {
        return new RealmRegistry(realms);
    }
}
