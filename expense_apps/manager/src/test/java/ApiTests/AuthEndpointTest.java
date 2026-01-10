package ApiTests;

import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.Allure;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.Tag;

import static io.restassured.RestAssured.given;

@Epic("Manager App")
@Feature("Manager Authentication")
@Tag("API")
@Tag("Sprint-3")
public class AuthEndpointTest {

    private final String BASE_URL = "http://localhost:5001";

    @BeforeAll
    public static void setUp() throws Exception {
        DummyDataLoader dataLoader = new DummyDataLoader();
        dataLoader.restoreDatabase();
    }

    @BeforeEach
    public void beforeEach() {
        Allure.label("suite", "API Tests");
    }

    @Test
    @Story("Manager Login")
    @Description("Verify status endpoint returns unauthenticated when no token provided")
    @Severity(SeverityLevel.NORMAL)
    void test_status_with_no_token() {
        given()
                .when()
                .get(BASE_URL + "/api/auth/status")
                .then()
                .statusCode(200)
                .body("authenticated", is(false));
    }

    @Test
    @Story("Manager Login")
    @Description("Verify logout endpoint succeeds even without token")
    @Severity(SeverityLevel.NORMAL)
    void test_logout_with_no_token() {
        given()
                .when()
                .post(BASE_URL + "/api/auth/logout")
                .then()
                .statusCode(200)
                .body("success", is(true));
    }

    @Test
    @Story("Manager Login")
    @Description("Verify login fails with malformed JSON")
    @Severity(SeverityLevel.NORMAL)
    void test_login_with_malformed_fields() {
        given()
                .body("{\"fail\":}")
                .when()
                .post(BASE_URL + "/api/auth/login")
                .then()
                .statusCode(400)
                .body("success", is(false))
                .body("error", is("Invalid request format"));
    }

    @Test
    @Story("Manager Login")
    @Description("Verify login fails with null username or password")
    @Severity(SeverityLevel.NORMAL)
    void test_login_with_null_fields() {
        given()
                .body("{\"username\":null, \"password\":null}")
                .when()
                .post(BASE_URL + "/api/auth/login")
                .then()
                .statusCode(400)
                .body("success", is(false))
                .body("error", is("Username and password are required"));
    }

    @ParameterizedTest
    @Story("Manager Login")
    @Description("Verify login fails with invalid credentials")
    @Severity(SeverityLevel.NORMAL)
    @CsvSource({
        "invalid_user, invalid_password",
        "invalid_user, password123",
        "manager1, invalid_password"
    })
    void test_login_with_invalid_credentials(String username, String password) {
        given()
                .body("{\"username\":\"" + username + "\", \"password\":\"" + password + "\"}")
                .when()
                .post(BASE_URL + "/api/auth/login")
                .then()
                .statusCode(401)
                .body("success", is(false));
    }

    @Test
    @Story("Manager Login")
    @Description("Verify non-manager roles cannot login to manager app")
    @Severity(SeverityLevel.CRITICAL)
    void test_login_with_incorrect_role() {
        given()
                .body("{\"username\":\"employee1\", \"password\":\"password123\"}")
                .when()
                .post(BASE_URL + "/api/auth/login")
                .then()
                .statusCode(401)
                .body("success", is(false));
    }

    @Test
    @Story("Manager Login")
    @Description("Verify successful login with valid manager credentials")
    @Severity(SeverityLevel.CRITICAL)
    void test_login_with_valid_credentials() {
        given()
                .body("{\"username\":\"manager1\", \"password\":\"password123\"}")
                .when()
                .post(BASE_URL + "/api/auth/login")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("user.username", is("manager1"))
                .body("user.role", is("Manager"));
    }

    @Test
    @Story("Manager Login")
    @Description("Verify status endpoint returns authenticated with valid token")
    @Severity(SeverityLevel.CRITICAL)
    void test_status_with_valid_credentials() {
        String jwt = given()
                .body("{\"username\":\"manager1\", \"password\":\"password123\"}")
                .post(BASE_URL + "/api/auth/login")
                .getCookie("jwt");

        given()
                .cookie("jwt", jwt)
                .when()
                .get(BASE_URL + "/api/auth/status")
                .then()
                .statusCode(200)
                .body("authenticated", is(true))
                .body("user.username", is("manager1"))
                .body("user.role", is("Manager"));

    }

    @Test
    @Story("Manager Login")
    @Description("Verify logout flow invalidate session")
    @Severity(SeverityLevel.CRITICAL)
    void test_logout_with_valid_credentials() {
        io.restassured.filter.cookie.CookieFilter cookieFilter = new io.restassured.filter.cookie.CookieFilter();

        // Login
        given()
                .filter(cookieFilter)
                .contentType(io.restassured.http.ContentType.JSON)
                .body("{\"username\":\"manager1\", \"password\":\"password123\"}")
                .when()
                .post(BASE_URL + "/api/auth/login")
                .then()
                .statusCode(200);

        // Logout
        given()
                .filter(cookieFilter)
                .when()
                .post(BASE_URL + "/api/auth/logout")
                .then()
                .statusCode(200)
                .body("success", is(true));

        // Verify status is unauthenticated
        given()
                .filter(cookieFilter)
                .when()
                .get(BASE_URL + "/api/auth/status")
                .then()
                .statusCode(200)
                .body("authenticated", is(false));
    }

}
