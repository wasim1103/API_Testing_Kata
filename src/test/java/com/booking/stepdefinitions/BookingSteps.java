package com.booking.stepdefinitions;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import com.booking.utils.Helper;



public class BookingSteps {

    private Response response;
    private Map<String, String> bookingPayload;
    private String bookingId;
    private String authToken;
    private Scenario scenario;

    @Before
    public void setUp(Scenario scenario) {
        this.scenario = scenario;
    }

    // ------------------------------
    // CREATE / UPDATE / PATCH payload
    // ------------------------------
    @Given("a booking payload")
    public void givenBookingPayload(DataTable dataTable) {
        Map<String, String> rowMap = new HashMap<>(dataTable.asMaps(String.class, String.class).get(0));
        rowMap.replaceAll((k, v) -> (v == null || v.trim().equalsIgnoreCase("[empty]")) ? "" : v);
        this.bookingPayload = rowMap;
    }

    @Given("I create a booking with payload")
    public void createBookingWithPayload(DataTable dataTable) {
        Map<String, String> data = dataTable.asMaps(String.class, String.class).get(0);

        this.bookingPayload = data;
        Map<String, Object> requestBody = Helper.buildBookingRequestBody(data);

        response = RestAssured.given()
                .baseUri(RestAssured.baseURI)
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/booking")
                .then().log().all().extract().response();
        this.bookingId = response.jsonPath().getString("bookingid");
        if (bookingId == null) {
            throw new RuntimeException("Booking ID is null after creation.");
        }
        System.out.println("Created booking ID: " + bookingId);
    }

    @Given("I am logged in as admin")
    public void loginAsAdmin() {
        String loginJson = "{ \"username\": \"admin\", \"password\": \"password\" }";
        Response loginResponse = RestAssured.given()
                .baseUri(RestAssured.baseURI)
                .header("Content-Type", "application/json")
                .body(loginJson)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .response();

        this.authToken = loginResponse.jsonPath().getString("token");

        // ✅ Print token for debugging
        System.out.println("Generated Auth Token: " + authToken);

        if (authToken == null || authToken.isEmpty()) {
            throw new AssertionError("Login failed. Token is null.");
        }
    }

    // ------------------------------
    // SEND REQUEST
    // ------------------------------
    @When("I send a POST request to {string}")
    public void sendPostRequest(String endpoint) {
        Map<String, Object> requestBody = Helper.buildBookingRequestBody(bookingPayload);

        response = RestAssured.given()
                .baseUri(RestAssured.baseURI)
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post(endpoint)
                .then()
                .log().all()
                .extract().response();

        Helper.attachRequestResponse(scenario, response, "POST", "/booking",
                Helper.buildBookingRequestBody(bookingPayload).toString());
    }

    @When("I send a GET request to {string}")
    public void sendGetRequest(String endpoint) {
        if (endpoint.contains("<id>")) {
            if (bookingId == null) {
                throw new RuntimeException("Booking ID is null. Cannot perform GET.");
            }
            endpoint = endpoint.replace("<id>", bookingId);
        }

        // Prepare request
        RequestSpecification req = RestAssured.given()
                .baseUri(RestAssured.baseURI)
                .header("Content-Type", "application/json");

        // Add auth token if present
        if (authToken != null && !authToken.isEmpty()) {
            req.header("Cookie", "token=" + authToken);
        }

        // Only add body if bookingPayload exists (optional for GET)
        String requestBody = null;
        if (bookingPayload != null) {
            Map<String, Object> bodyMap = Helper.buildBookingRequestBody(bookingPayload);
            req.body(bodyMap);
            requestBody = bodyMap.toString();
        }

        // Send GET request
        response = req
                .when()
                .get(endpoint)
                .then()
                .log().all()
                .extract()
                .response();

        // Attach request/response log safely
        Helper.attachRequestResponse(scenario, response, "GET", endpoint, requestBody);
    }

    @When("I send a PUT request to {string}")
    public void sendPutRequest(String endpoint) {
        if (bookingId == null) {
            throw new RuntimeException("Booking ID is null. Cannot perform PUT.");
        }
        endpoint = endpoint.replace("<id>", bookingId);

        Map<String, Object> requestBody = Helper.buildBookingRequestBody(bookingPayload);

        response = RestAssured.given()
                .baseUri(RestAssured.baseURI)
                .contentType("application/json")
                .header("Cookie", "token=" + authToken)
                .body(requestBody)
                .when()
                .put(endpoint)
                .then()
                .log().all()
                .extract().response();

        Helper.attachRequestResponse(scenario, response, "PUT", endpoint,
                Helper.buildBookingRequestBody(bookingPayload).toString());
    }

    @When("I send a PATCH request to {string}")
    public void sendPatchRequest(String endpoint) {
        if (bookingId == null) {
            throw new RuntimeException("Booking ID is null. Cannot perform PATCH.");
        }
        endpoint = endpoint.replace("<id>", bookingId);

        Map<String, Object> requestBody = Helper.buildBookingRequestBody(bookingPayload);

        response = RestAssured.given()
                .baseUri(RestAssured.baseURI)
                .contentType("application/json")
                .header("Cookie", "token=" + authToken)
                .body(requestBody)
                .when()
                .patch(endpoint)
                .then()
                .log().all()
                .extract().response();

        Helper.attachRequestResponse(scenario, response, "PATCH", endpoint,
                Helper.buildBookingRequestBody(bookingPayload).toString());
    }

    @When("I send a DELETE request to {string}")
    public void sendDeleteRequest(String endpoint) {
        if (bookingId == null) {
            throw new RuntimeException("Booking ID is null. Cannot perform DELETE.");
        }
        endpoint = endpoint.replace("<id>", bookingId);

        response = RestAssured.given()
                .baseUri(RestAssured.baseURI)
                .contentType("application/json")
                .header("Cookie", "token=" + authToken)
                .when()
                .delete(endpoint)
                .then()
                .log().all()
                .extract().response();

        Helper.attachRequestResponse(scenario, response, "DELETE", endpoint,
                Helper.buildBookingRequestBody(bookingPayload).toString());
    }

    // ------------------------------
    // VERIFY RESPONSE
    // ------------------------------
    @Then("the response status should be {int}")
    public void verifyResponseStatus(int expectedStatus) {
        Assertions.assertNotNull(response, "Response is null");
        Assertions.assertEquals(expectedStatus, response.getStatusCode());
    }

    @Then("the response should match the booking payload")
    public void verifyResponseMatchesPayload() {
        Helper.verifyResponseMatchesPayload(response, bookingPayload);
    }

    @Then("the response should match the updated fields")
    public void verifyResponseMatchesUpdatedFields() {
        Helper.verifyResponseMatchesPayload(response, bookingPayload);
    }

    @Then("the response should indicate a validation status error code {int}")
    public void verifyValidationErrors(int expectedStatus) {
        Assertions.assertNotNull(response, "Response is null");
        Assertions.assertEquals(expectedStatus, response.getStatusCode());
    }

    @Then("the response should match the booking details")
    public void the_response_should_match_the_booking_details() {
        Helper.verifyResponseMatchesPayload(response, bookingPayload);
    }

}
