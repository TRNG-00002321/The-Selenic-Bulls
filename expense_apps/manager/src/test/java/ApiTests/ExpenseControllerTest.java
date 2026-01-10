package ApiTests;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import org.junit.jupiter.api.Tag;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@Epic("Manager App")
@Feature("Expense Management")
@Tag("API")
@Tag("Sprint-3")
public class ExpenseControllerTest {

    private static final String BASE_URL = "http://localhost:5001";
    protected static CookieFilter cookieFilter;

    @BeforeAll
    public static void globalSetup() {
        RestAssured.baseURI = BASE_URL;
        cookieFilter = new CookieFilter();
    }

    @BeforeEach
    public void setUp() throws Exception {
        Allure.label("suite", "API Tests");
        DummyDataLoader dataLoader = new DummyDataLoader();
        dataLoader.restoreDatabase();
    }

    @Test
    @Story("View All Expenses")
    @Description("Verify managers can retrieve all expenses with statistics")
    @Severity(SeverityLevel.CRITICAL)
    public void get_expenses_with_valid_manager_auth_sucess() {
        String jwt = given()
                .contentType(ContentType.JSON)
                .body("{\"username\": \"manager1\", \"password\": \"password123\"}")
                .post(BASE_URL + "/api/auth/login")
                .getCookie("jwt");

        given()
                .cookie("jwt", jwt)
                .when()
                .get(BASE_URL + "/api/expenses")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("count", is(7))
                .body("data.size()", is(7))
                .body("data.expense.id", hasItems(4, 5));
    }

    @Test
    @Disabled("Bug in expense API - ticket EMS-55")
    @Story("View All Expenses")
    @Description("Verify unauthenticated users cannot access expenses")
    @Severity(SeverityLevel.CRITICAL)
    public void get_expenses_with_no_auth_fail() {
        given()
                .when()
                .get(BASE_URL + "/api/expenses")
                .then()
                .statusCode(401);
    }

    @Test
    @Disabled("Bug in expense API - ticket EMS-56")
    @Story("View All Expenses")
    @Description("Verify employees cannot access all expenses")
    @Severity(SeverityLevel.NORMAL)
    public void get_expenses_with_employee_auth_fail() {
        String jwt = given()
                .contentType(ContentType.JSON)
                .body("{\"username\": \"employee1\", \"password\": \"password123\"}")
                .post(BASE_URL + "/api/auth/login")
                .getCookie("jwt");

        given()
                .cookie("jwt", jwt)
                .when()
                .get(BASE_URL + "/api/expenses")
                .then()
                .statusCode(401);
    }

    @Test
    @Disabled("Bug in expense API - ticket EMS-57")
    @Story("View All Expenses")
    @Description("Verify retrieval of specific expense by ID")
    @Severity(SeverityLevel.NORMAL)
    public void get_expense_by_id_with_auth_success() {
        String jwt = given()
                .contentType(ContentType.JSON)
                .body("{\"username\": \"manager1\", \"password\": \"password123\"}")
                .post(BASE_URL + "/api/auth/login")
                .getCookie("jwt");

        given()
                .cookie("jwt", jwt)
                .when()
                .get(BASE_URL + "/api/expenses/1")
                .then()
                .statusCode(200);
    }

    @ParameterizedTest
    @CsvSource({
        "1, 3",
        "2, 2"
    })
    @Story("View All Expenses")
    @Description("Verify retrieval of expenses stats by employee ID")
    @Severity(SeverityLevel.NORMAL)
    public void get_expense_by_employeeId_with_auth_success(String employeeId, int count) {
        String jwt = given()
                .contentType(ContentType.JSON)
                .body("{\"username\": \"manager1\", \"password\": \"password123\"}")
                .post(BASE_URL + "/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .cookie("jwt");

        given()
                .cookie("jwt", jwt)
                .when()
                .get(BASE_URL + "/api/expenses/employee/" + employeeId)
                .then()
                .statusCode(200)
                .body("count", is(count))
                .body("data.expenses.size()", is(count));
    }

    @ParameterizedTest
    @CsvSource({
        "1, 3",
        "2, 2"
    })
    @Story("View All Expenses")
    @Description("Verify unauthenticated users cannot access employee expense stats")
    @Severity(SeverityLevel.NORMAL)
    public void get_expense_by_employeeId_with_no_auth_fails(String employeeId, int count) {
        given()
                .when()
                .get(BASE_URL + "/api/expenses/employee/" + employeeId)
                .then()
                .statusCode(401);
    }

    @ParameterizedTest
    @CsvSource({
        "1, 3",
        "2, 2"
    })
    @Story("View All Expenses")
    @Description("Verify employees cannot access other employees' expense stats")
    @Severity(SeverityLevel.NORMAL)
    public void get_expense_by_employeeId_with_employee_auth_fails(String employeeId, int count) {
        given()
                .when()
                .get(BASE_URL + "/api/expenses/employee/" + employeeId)
                .then()
                .statusCode(401);
    }

    //J's tests
    @Test
    @Story("Review Expenses")
    @Description("Verify retrieval of pending expenses for review")
    @Severity(SeverityLevel.CRITICAL)
    void getPendingExpenseValidAuthReturns200() {
        String jwt = given()
                .contentType(ContentType.JSON)
                .body("{\"username\": \"manager1\", \"password\": \"password123\"}")
                .post(BASE_URL + "/api/auth/login")
                .getCookie("jwt");

        given()
                .cookie("jwt", jwt)
                .when()
                .get(BASE_URL + "/api/expenses/pending")
                .then()
                .statusCode(200)
                .body("success", equalTo(true));
    }

    @Test
    @Story("Review Expenses")
    @Description("Verify unauthenticated access to pending expenses fails")
    @Severity(SeverityLevel.NORMAL)
    void getPendingExpenseInvalidAuthReturns401() {
        given()
                .when()
                .get(BASE_URL + "/api/expenses/pending")
                .then()
                .statusCode(401);
    }

    @Test
    @Story("View All Expenses")
    @Description("Verify retrieval of expense list by employee")
    @Severity(SeverityLevel.NORMAL)
    void getExpenseByEmployeeReturns200() {
        String jwt = given()
                .contentType(ContentType.JSON)
                .body("{\"username\": \"manager1\", \"password\": \"password123\"}")
                .post(BASE_URL + "/api/auth/login")
                .getCookie("jwt");

        given()
                .cookie("jwt", jwt)
                .when()
                .get(BASE_URL + "/api/expenses/employee/1")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("employeeId", equalTo(1));
    }

    private void authenticateForCookieFilter() {
        given()
                .filter(cookieFilter)
                .contentType("application/json")
                .body("""
                {
                  "username": "manager1",
                  "password": "password123"
                }
            """)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200);
    }

    @Test
    @Story("Approve/Deny Expenses")
    @Description("Verify valid denial request succeeds")
    @Severity(SeverityLevel.CRITICAL)
    public void denyExpenseHappyTest() {
        authenticateForCookieFilter();
        Response response = given()
                .filter(cookieFilter)
                .body("""
                {
                "comment" : "new expense is denied"
                }
                """)
                .when()
                .post("/api/expenses/6/deny");

        assert response.statusCode() == 200;
        assert response.getBody().asString().contains("denied");
    }

    @Test
    @Story("Approve/Deny Expenses")
    @Description("Verify denial fails for invalid expense ID")
    @Severity(SeverityLevel.NORMAL)
    public void denyExpenseSadTest() {
        authenticateForCookieFilter();
        Response response = given()
                .filter(cookieFilter)
                .body("""
                {
                "comment" : "new expense is denied"
                }
                """)
                .when()
                .post("/api/expenses/999/deny");

        assert response.statusCode() == 404;
    }

    @Test
    @Story("Approve/Deny Expenses")
    @Description("Verify unauthenticated users cannot deny expenses")
    @Severity(SeverityLevel.CRITICAL)
    public void denyExpenseNoAuthTest() {
        Response response = given()
                .body("""
                {
                "comment" : "new expense is denied"
                }
                """)
                .when()
                .post("/api/expenses/6/deny");

        assert response.statusCode() == 401;
    }

    @Test
    @Story("Approve/Deny Expenses")
    @Description("Verify valid approval request succeeds")
    @Severity(SeverityLevel.CRITICAL)
    public void approveExpenseHappyTest() {
        authenticateForCookieFilter();
        Response response = given()
                .filter(cookieFilter)
                .body("""
                {
                "comment" : "new expense is approved"
                }
                """)
                .when()
                .post("/api/expenses/6/approve");

        assert response.statusCode() == 200;
        assert response.getBody().asString().contains("approved");
    }

    @Test
    @Story("Approve/Deny Expenses")
    @Description("Verify approval fails for invalid expense ID")
    @Severity(SeverityLevel.NORMAL)
    public void approveExpenseSadTest() {
        authenticateForCookieFilter();
        Response response = given()
                .filter(cookieFilter)
                .body("""
                {
                "comment" : "new expense is approved"
                }
                """)
                .when()
                .post("/api/expenses/999/approve");

        assert response.statusCode() == 404;
    }

    @Test
    @Story("Approve/Deny Expenses")
    @Description("Verify unauthenticated users cannot approve expenses")
    @Severity(SeverityLevel.CRITICAL)
    public void approveExpenseNoAuthTest() {
        Response response = given()
                .body("""
                {
                "comment" : "new expense is approved"
                }
                """)
                .when()
                .post("/api/expenses/6/approve");

        assert response.statusCode() == 401;
    }
}
