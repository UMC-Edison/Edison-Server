package com.edison.project;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

// @SpringBootTest
@ImportAutoConfiguration(
  exclude = org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class
)
@ActiveProfiles("test")
class ProjectApplicationTests {

  @Test
  void contextLoads() {
  }
}
