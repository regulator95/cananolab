package gov.nih.nci.cananolab.restful;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SecurityServicesTest {
	
	String urlbase = "http://localhost:8080/caNanoLab/rest/";
	Client client; 

	@Before
	public void setUp() throws Exception {
		client = ClientBuilder.newClient();
	}

	@Test
	public void testLogin() {
		try {
		String jsonString = client.target(urlbase)
				.register(SampleServices.class)
				.path("security/login")
				.queryParam("username", "canano_res").queryParam("password", "Quality@4")
				.request("application/json")
				.header("some-header", "true")
				.get(String.class);

		assertNotNull(jsonString);
		assertTrue(jsonString.length() > 0);
		
		jsonString = client.target(urlbase)
				.register(SampleServices.class)
				.path("security/logout")
				.request("application/json")
				.header("some-header", "true")
				.get(String.class);
		
		//test with fake password
			assertNotNull(jsonString);
			assertTrue(!jsonString.contains("success"));
		} catch (Exception e) {
			assertTrue(e.getMessage().equals("HTTP 404 Not Found"));
		}
	}


	@Test
	public void testLogout() {
		String jsonString = client.target(urlbase)
				.register(SampleServices.class)
				.path("security/logout")
				.request("application/json")
				.header("some-header", "true")
				.get(String.class);
		
		assertNotNull(jsonString);
		assertTrue(jsonString.contains("success"));
	}

}
