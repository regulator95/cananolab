package gov.nih.nci.cananolab.restful.util;

import gov.nih.nci.cananolab.util.PropertyUtils;
import io.restassured.response.Response;

//import static com.jayway.restassured.RestAssureded.expect;
//import static com.jayway.restassured.RestAssured.with;
import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.with;

/**
 * 
 * @author yangs8
 *
 */
public class RestTestLoginUtil {
	
	public static String jsessionId = null;

	/**
	 * Need to set correct credentials before running test. Do not check in this file
	 * with credential!!
	 * 
	 * @return
	 */
	public static String loginTest() {

		if (jsessionId == null) {
			String username = RestTestLoginUtil.readUserNameProperty();
			String pwd = RestTestLoginUtil.readPasswordProperty();
			
			if (username == null || username.length() == 0 ||
					pwd == null || pwd.length() == 0)
				return null;
			
			Response response = with().params("username", username, "password", pwd)
					.expect().statusCode(200).when().get("http://localhost:8080/caNanoLab/rest/security/login");

			jsessionId = response.getCookie("JSESSIONID");
		}
		return jsessionId;
	}
	
	public static void logoutTest() {
		expect().statusCode(200).when().get("http://localhost:8080/caNanoLab/rest/security/logout");
		jsessionId = null;
	}
	
	public static String readUserNameProperty() {
		return PropertyUtils.getPropertyCached("local.properties", "user.name");
	}

	public static String readPasswordProperty() {
		return PropertyUtils.getPropertyCached("local.properties", "password");
	}
}
