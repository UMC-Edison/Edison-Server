package com.edison.project;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProjectApplicationTests {

	@MockBean
	private OAuth2AuthorizedClientService authorizedClientService;

	@MockBean
	private OAuth2AuthorizedClientManager authorizedClientManager;

	@Test
	void contextLoads() {
	}
}
