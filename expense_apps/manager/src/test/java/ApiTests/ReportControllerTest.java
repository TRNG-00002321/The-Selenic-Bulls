package ApiTests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.Tag;

@Epic("Manager App")
@Feature("Expense Reporting")
@Tag("API")
@Tag("Sprint-3")
public class ReportControllerTest {

    private final String BASE_URL = "http://localhost:5001";
    private String managerJwtCookie;

    @BeforeEach
    public void setUp() throws Exception {
        Allure.label("suite", "API Tests");
        DummyDataLoader dataLoader = new DummyDataLoader();
        dataLoader.restoreDatabase();
        RestAssured.baseURI = BASE_URL;
        managerJwtCookie = given()
                .contentType(ContentType.JSON)
                .body("{\"username\": \"manager1\", \"password\": \"password123\"}")
                .post(BASE_URL + "/api/auth/login")
                .getCookie("jwt");
    }

    @Test
    @Story("Generate Reports")
    @Description("Verify managers can generate CSV report for all expenses")
    @Severity(SeverityLevel.NORMAL)
    public void generateAllExpenseReport_ValidToken() {

        String responseBody = given().cookie("jwt", managerJwtCookie)
                .when().get("/api/reports/expenses/csv")
                .then()
                .statusCode(200)
                .contentType("text/csv")
                .extract().body().asString();

        Assertions.assertTrue(responseBody.contains("Business lunch"));
        Assertions.assertTrue(responseBody.contains("Travel expense"));
        Assertions.assertTrue(responseBody.contains("Office supplies"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 5})
    @Story("Generate Reports")
    @Description("Verify generation of expense report by employee ID")
    @Severity(SeverityLevel.NORMAL)
    public void generateEmployeeExpensesReport(int employeeId) {
        given().cookie("jwt", managerJwtCookie)
                .pathParam("employeeId", employeeId)
                .when().get("/api/reports/expenses/employee/{employeeId}/csv")
                .then()
                .statusCode(200)
                .contentType("text/csv")
                .body(org.hamcrest.Matchers.containsString("Expense ID"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Travel", "Business", "Office", "Dinner"})
    @Story("Generate Reports")
    @Description("Verify generation of expense report by category")
    @Severity(SeverityLevel.NORMAL)
    public void generateCategoryExpensesReport(String category) {
        given().cookie("jwt", managerJwtCookie)
                .pathParam("category", category)
                .when().get("/api/reports/expenses/category/{category}/csv")
                .then()
                .statusCode(200)
                .contentType("text/csv")
                .body(org.hamcrest.Matchers.containsString("Expense ID"));
    }

    @Test
    @Story("Generate Reports")
    @Description("Verify generation of expense report by date range")
    @Severity(SeverityLevel.NORMAL)
    public void generateDateRangeExpensesReport() {
        given().cookie("jwt", managerJwtCookie)
                .queryParam("startDate", "2024-12-01")
                .queryParam("endDate", "2024-12-05")
                .when().get("/api/reports/expenses/daterange/csv")
                .then()
                .statusCode(200)
                .contentType("text/csv")
                .body(org.hamcrest.Matchers.containsString("Business lunch"));
    }

    @Test
    @Story("Generate Reports")
    @Description("Verify generation of report for pending expenses")
    @Severity(SeverityLevel.NORMAL)
    public void generatePendingExpensesReport() {
        given().cookie("jwt", managerJwtCookie)
                .when().get("/api/reports/expenses/pending/csv")
                .then()
                .statusCode(200)
                .contentType("text/csv");
    }

    @ParameterizedTest
    @CsvSource({
        "12-01-2024, 2024-12-05",
        "2024-12-01, 12-05-2024",
        "invalid, 2024-12-05",
        "2024-12-01, invalid"
    })
    @Story("Generate Reports")
    @Description("Verify handling of invalid date formats")
    @Severity(SeverityLevel.NORMAL)
    public void generateDateRangeExpensesReport_InvalidFormat(String startDate, String endDate) {
        given().cookie("jwt", managerJwtCookie)
                .queryParam("startDate", startDate)
                .queryParam("endDate", endDate)
                .when().get("/api/reports/expenses/daterange/csv")
                .then()
                .statusCode(400);
    }

    @Test
    @Disabled("Disabled due to bug in ReportController parameter handling")
    @Story("Generate Reports")
    @Description("Verify handling of invalid employee IDs")
    @Severity(SeverityLevel.NORMAL)
    public void generateEmployeeExpensesReport_InvalidId() {
        given().cookie("jwt", managerJwtCookie)
                .pathParam("employeeId", "abc")
                .when().get("/api/reports/expenses/employee/{employeeId}/csv")
                .then()
                .statusCode(400);
    }

    @ParameterizedTest
    @CsvSource({
        "/api/reports/expenses/csv",
        "/api/reports/expenses/pending/csv"
    })
    @Story("Generate Reports")
    @Description("Verify access control for report endpoints (no/invalid token)")
    @Severity(SeverityLevel.CRITICAL)
    public void testProtectedEndpoints_NoParams_InvalidToken(String endpoint) {
        // No token
        given()
                .when().get(endpoint)
                .then()
                .statusCode(401);

        // Invalid token
        given().cookie("jwt", "invalid_token_value")
                .when().get(endpoint)
                .then()
                .statusCode(401);
    }

    @ParameterizedTest
    @CsvSource({
        "/api/reports/expenses/employee/{id}/csv, id, 1",
        "/api/reports/expenses/category/{cat}/csv, cat, Travel"
    })
    @Story("Generate Reports")
    @Description("Verify access control for parameterized report endpoints")
    @Severity(SeverityLevel.CRITICAL)
    public void testProtectedEndpoints_WithPathParam_InvalidToken(String endpoint, String paramName, String paramValue) {
        // No token
        given()
                .pathParam(paramName, paramValue)
                .when().get(endpoint)
                .then()
                .statusCode(401);

        // Invalid token
        given().cookie("jwt", "invalid_token_value")
                .pathParam(paramName, paramValue)
                .when().get(endpoint)
                .then()
                .statusCode(401);
    }

    @Test
    @Story("Generate Reports")
    @Description("Verify access control for date range report endpoint")
    @Severity(SeverityLevel.CRITICAL)
    public void testProtectedEndpoints_DateRange_InvalidToken() {
        // No token
        given()
                .queryParam("startDate", "2024-01-01")
                .queryParam("endDate", "2024-01-31")
                .when().get("/api/reports/expenses/daterange/csv")
                .then()
                .statusCode(401);

        // Invalid token
        given().cookie("jwt", "invalid_token_value")
                .queryParam("startDate", "2024-01-01")
                .queryParam("endDate", "2024-01-31")
                .when().get("/api/reports/expenses/daterange/csv")
                .then()
                .statusCode(401);
    }

}
