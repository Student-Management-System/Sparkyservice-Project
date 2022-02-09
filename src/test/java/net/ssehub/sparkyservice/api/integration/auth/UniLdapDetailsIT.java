package net.ssehub.sparkyservice.api.integration.auth;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import net.ssehub.sparkyservice.api.testconf.AbstractContainerTestDatabase;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = {"classpath:test-routing.properties"})
@AutoConfigureMockMvc
//checkstyle: stop exception type check
public class UniLdapDetailsIT extends AbstractContainerTestDatabase{

}
