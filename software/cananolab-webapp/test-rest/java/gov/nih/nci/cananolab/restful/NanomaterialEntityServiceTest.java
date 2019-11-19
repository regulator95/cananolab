package gov.nih.nci.cananolab.restful;

import gov.nih.nci.cananolab.restful.util.RestTestLoginUtil;
import gov.nih.nci.cananolab.restful.view.edit.SimpleComposingElementBean;
import gov.nih.nci.cananolab.restful.view.edit.SimpleFileBean;
import gov.nih.nci.cananolab.restful.view.edit.SimpleNanomaterialEntityBean;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NanomaterialEntityServiceTest {

	@Test
	public void testSetup() {

		Response res =
				given().contentType("application/json")
				.param("sampleId", "20917510").expect()
				.body("nanomaterialEntityTypes", hasItems("biopolymer", "carbon", "carbon black", "carbon nanotube", "dendrimer", "emulsion", "fullerene", "liposome", "metal oxide", "metal particle", "metalloid", "nanohorn", "nanorod", "nanoshell", "polymer", "quantum dot", "silica"))
						.when().get("http://localhost:8080/caNanoLab/rest/nanomaterialEntity/setup");

		System.out.println(res.getBody().asString());
		
	}

	@Test
	public void testEdit() {
		
		String jsessionId = RestTestLoginUtil.loginTest();
		
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("sampleId", "20917506");
		parameters.put("dataId", "60260353");
		Response res =
				given().contentType("application/json").cookie("JSESSIONID=" + jsessionId)
				.params(parameters).expect()
				.body("description", equalToIgnoringCase("Test Nano Entity"))
						.when().get("http://localhost:8080/caNanoLab/rest/nanomaterialEntity/edit");

		System.out.println(res.getBody().asString());
		RestTestLoginUtil.logoutTest();
		
	}
//	@Test
//	public void testSaveComposingElement() {
//		
//		String jsessionId = RestTestLoginUtil.loginTest();
//		SimpleNanomaterialEntityBean simpleNano = new SimpleNanomaterialEntityBean();
//		simpleNano.setSampleId("20917510");
//		simpleNano.setType("carbon");
//		SimpleComposingElementBean comp = new SimpleComposingElementBean();
//		comp.setType("monomer");
//		comp.setName("TestChem");
//		List<Map<String, Object>> inherent = new ArrayList<Map<String,Object>>();
//		List<SimpleComposingElementBean> compList = new ArrayList<SimpleComposingElementBean>();
//		comp.setInherentFunction(inherent);
//		simpleNano.setSimpleCompBean(comp);
//		simpleNano.setComposingElements(compList);
//		
//		Response res =
//				given().contentType("application/json").cookie("JSESSIONID=" + jsessionId)
//				.body(simpleNano).expect()
//				.body("Type", equalToIgnoringCase("monomer"))
//						.when().post("http://localhost:8080/caNanoLab/rest/nanomaterialEntity/saveComposingElement");
//
//		System.out.println(res.getBody().asString());
//		RestTestLoginUtil.logoutTest();
//		
//	}
	
	@Test
	public void testSaveComposingElement() {

		SimpleNanomaterialEntityBean simpleNano = new SimpleNanomaterialEntityBean();
		simpleNano.setSampleId("20917510");
		simpleNano.setType("carbon");
		SimpleComposingElementBean comp = new SimpleComposingElementBean();
		comp.setType("monomer");
		comp.setName("TestChem");
		List<Map<String, Object>> inherent = new ArrayList<Map<String,Object>>();
		List<SimpleComposingElementBean> compList = new ArrayList<SimpleComposingElementBean>();
		comp.setInherentFunction(inherent);
		simpleNano.setSimpleCompBean(comp);
		simpleNano.setComposingElements(compList);
		
		String jsessionId = RestTestLoginUtil.loginTest();

		final Client aClient = ClientBuilder.newBuilder()
		        .register(ObjectMapperProvider.class)
		        .build();
		
		WebTarget webTarget = aClient.target("http://localhost:8080/caNanoLab/rest");
		webTarget.register(NanomaterialEntityServices.class);
		
		WebTarget submitWebTarget = webTarget.path("nanomaterialEntity").path("saveComposingElement");

		javax.ws.rs.core.Response postResponse =
				submitWebTarget.request("application/json")
		         .post(Entity.json(simpleNano));
		
		assertNotNull(postResponse);
		assertTrue(postResponse.getStatus() == 401);
		
		postResponse.bufferEntity();
		String json = (String) postResponse.readEntity(String.class);
				
		assertTrue(json.contains("Session expired"));
		RestTestLoginUtil.logoutTest();

		}
		
	@Test
	public void testRemoveComposingElement() {

		SimpleNanomaterialEntityBean simpleNano = new SimpleNanomaterialEntityBean();
		simpleNano.setSampleId("20917510");
		simpleNano.setType("carbon");
		SimpleComposingElementBean comp = new SimpleComposingElementBean();
		comp.setType("monomer");
		comp.setName("TestChem");
		List<Map<String, Object>> inherent = new ArrayList<Map<String,Object>>();
		List<SimpleComposingElementBean> compList = new ArrayList<SimpleComposingElementBean>();
		comp.setInherentFunction(inherent);
		simpleNano.setSimpleCompBean(comp);
		simpleNano.setComposingElements(compList);
		
		String jsessionId = RestTestLoginUtil.loginTest();

		final Client aClient = ClientBuilder.newBuilder()
		        .register(ObjectMapperProvider.class)
		        .build();
		
		WebTarget webTarget = aClient.target("http://localhost:8080/caNanoLab/rest");
		webTarget.register(NanomaterialEntityServices.class);
		
		WebTarget submitWebTarget = webTarget.path("nanomaterialEntity").path("removeComposingElement");

		javax.ws.rs.core.Response postResponse =
				submitWebTarget.request("application/json")
		         .post(Entity.json(simpleNano));
		
		assertNotNull(postResponse);
		assertTrue(postResponse.getStatus() == 401);
		
		postResponse.bufferEntity();
		String json = (String) postResponse.readEntity(String.class);
				
		assertTrue(json.contains("Session expired"));
		RestTestLoginUtil.logoutTest();
		}
		
	@Test
	public void testsaveFile() {

		SimpleNanomaterialEntityBean simpleNano = new SimpleNanomaterialEntityBean();
		simpleNano.setSampleId("20917510");
		simpleNano.setType("carbon");
		SimpleFileBean file = new SimpleFileBean();
		file.setType("movie");
		file.setTitle("TEST MoVIE");
		file.setUriExternal(false);
		file.setExternalUrl("http://www.cancer.gov");
		file.setSampleId("20917510");
		List<SimpleComposingElementBean> compList = new ArrayList<SimpleComposingElementBean>();
		simpleNano.setFileBean(file);
		simpleNano.setComposingElements(compList);
		
		String jsessionId = RestTestLoginUtil.loginTest();
		
		final Client aClient = ClientBuilder.newBuilder()
		        .register(ObjectMapperProvider.class)
		        .build();
		
		WebTarget webTarget = aClient.target("http://localhost:8080/caNanoLab/rest");
		webTarget.register(NanomaterialEntityServices.class);
		
		WebTarget submitWebTarget = webTarget.path("nanomaterialEntity").path("saveFile");

		javax.ws.rs.core.Response postResponse =
				submitWebTarget.request("application/json")
		         .post(Entity.json(simpleNano));
		
		assertNotNull(postResponse);
		assertTrue(postResponse.getStatus() == 401);
		
		postResponse.bufferEntity();
		String json = (String) postResponse.readEntity(String.class);
				
		assertTrue(json.contains("Session expired"));
		RestTestLoginUtil.logoutTest();
		}
		
	@Test
	public void testRemoveFile() {
		
		SimpleNanomaterialEntityBean simpleNano = new SimpleNanomaterialEntityBean();
		simpleNano.setSampleId("20917510");
		simpleNano.setType("carbon");
		SimpleFileBean file = new SimpleFileBean();
		file.setType("movie");
		file.setTitle("TEST MoVIE");
		file.setUriExternal(false);
		file.setExternalUrl("http://www.cancer.gov");
		file.setSampleId("20917510");
		List<SimpleComposingElementBean> compList = new ArrayList<SimpleComposingElementBean>();
		simpleNano.setFileBean(file);
		simpleNano.setComposingElements(compList);
		
		String jsessionId = RestTestLoginUtil.loginTest();
		
		final Client aClient = ClientBuilder.newBuilder()
		        .register(ObjectMapperProvider.class)
		        .build();
		
		WebTarget webTarget = aClient.target("http://localhost:8080/caNanoLab/rest");
		webTarget.register(NanomaterialEntityServices.class);
		
		WebTarget submitWebTarget = webTarget.path("nanomaterialEntity").path("removeFile");

		javax.ws.rs.core.Response postResponse =
				submitWebTarget.request("application/json")
		         .post(Entity.json(simpleNano));
		
		assertNotNull(postResponse);
		assertTrue(postResponse.getStatus() == 401);
		
		postResponse.bufferEntity();
		String json = (String) postResponse.readEntity(String.class);
				
		assertTrue(json.contains("Session expired"));
		RestTestLoginUtil.logoutTest();
		}
	
	@Test
	public void testSubmit() {

		SimpleNanomaterialEntityBean simpleNano = new SimpleNanomaterialEntityBean();
		simpleNano.setSampleId("20917510");
		simpleNano.setType("carbon");
		SimpleFileBean file = new SimpleFileBean();
		file.setType("graph");
		file.setTitle("TEST");
		file.setUriExternal(false);
		file.setExternalUrl("http://www.cancer.gov");
		file.setSampleId("20917510");
		SimpleComposingElementBean comp = new SimpleComposingElementBean();
		comp.setType("monomer");
		comp.setName("TestChem");
		Map<String, Object> domainEntity = new HashMap<String, Object>();
		domainEntity.put("id", "85852160");
		List<Map<String, Object>> inherent = new ArrayList<Map<String,Object>>();
		List<SimpleComposingElementBean> compList = new ArrayList<SimpleComposingElementBean>();
		List<SimpleFileBean> fileList = new ArrayList<SimpleFileBean>();
		compList.add(comp);
		fileList.add(file);
		simpleNano.setComposingElements(compList);
		simpleNano.setFiles(fileList);
		
		String jsessionId = RestTestLoginUtil.loginTest();
		
		final Client aClient = ClientBuilder.newBuilder()
		        .register(ObjectMapperProvider.class)
		        .build();
		
		WebTarget webTarget = aClient.target("http://localhost:8080/caNanoLab/rest");
		webTarget.register(NanomaterialEntityServices.class);
		
		WebTarget submitWebTarget = webTarget.path("nanomaterialEntity").path("submit");

		javax.ws.rs.core.Response postResponse =
				submitWebTarget.request("application/json")
		         .post(Entity.json(simpleNano));
		
		assertNotNull(postResponse);
		assertTrue(postResponse.getStatus() == 401);
		
		postResponse.bufferEntity();
		String json = (String) postResponse.readEntity(String.class);
				
		assertTrue(json.contains("Session expired"));
		RestTestLoginUtil.logoutTest();
		}
		
	@Test
	public void testDelete() {

		SimpleNanomaterialEntityBean simpleNano = new SimpleNanomaterialEntityBean();
		simpleNano.setSampleId("20917510");
		simpleNano.setType("carbon");
		SimpleFileBean file = new SimpleFileBean();
		file.setType("graph");
		file.setTitle("TEST");
		file.setUriExternal(false);
		file.setExternalUrl("http://www.cancer.gov");
		file.setSampleId("20917510");
		SimpleComposingElementBean comp = new SimpleComposingElementBean();
		comp.setType("monomer");
		comp.setName("TestChem");
		Map<String, Object> domainEntity = new HashMap<String, Object>();
		domainEntity.put("id", "85852160");
		List<SimpleComposingElementBean> compList = new ArrayList<SimpleComposingElementBean>();
		List<SimpleFileBean> fileList = new ArrayList<SimpleFileBean>();
		compList.add(comp);
		fileList.add(file);
		simpleNano.setComposingElements(compList);
		simpleNano.setFiles(fileList);
		
		String jsessionId = RestTestLoginUtil.loginTest();
		
		final Client aClient = ClientBuilder.newBuilder()
		        .register(ObjectMapperProvider.class)
		        .build();
		
		WebTarget webTarget = aClient.target("http://localhost:8080/caNanoLab/rest");
		webTarget.register(NanomaterialEntityServices.class);
		
		WebTarget submitWebTarget = webTarget.path("nanomaterialEntity").path("delete");

		javax.ws.rs.core.Response postResponse =
				submitWebTarget.request("application/json")
		         .post(Entity.json(simpleNano));
		
		assertNotNull(postResponse);
		assertTrue(postResponse.getStatus() == 401);
		
		postResponse.bufferEntity();
		String json = (String) postResponse.readEntity(String.class);
				
		assertTrue(json.contains("Session expired"));
		RestTestLoginUtil.logoutTest();
		}
		
}
