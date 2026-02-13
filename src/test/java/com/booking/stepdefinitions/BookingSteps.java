package com.booking.stepdefinitions;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.booking.utils.AssertionUtils;

public class BookingSteps {

    private RequestSpecification request;
    private Response response;
    private Map<String, String> bookingPayload;
    private String bookingId;
    private String authToken;

    // ------------------------------
    // CREATE / UPDATE / PATCH payload
    // ------------------------------
    @Given("a booking payload")
    public void givenBookingPayload(DataTable dataTable) {
        // Convert DataTable row to modifiable map
        Map<String, String> rowMap = new HashMap<>(dataTable.asMaps(String.class, String.class).get(0));

        // Replace "[empty]" with ""
        rowMap.replaceAll((k, v) -> (v == null || v.trim().equalsIgnoreCase("[empty]")) ? "" : v);

        this.bookingPayload = rowMap;

        request = RestAssured.given()
                .baseUri(RestAssured.baseURI)
                .header("Content-Type", "application/json")
                .body(buildBookingJson(bookingPayload))
                .log().all();
    }

    @Given("I create a booking with payload")
    public void createBookingWithPayload(DataTable dataTable) {
        Map<String, String> data = dataTable.asMaps(String.class, String.class).get(0);

        Map<String, Object> bookingDates = new HashMap<>();
        bookingDates.put("checkin", data.get("checkin"));
        bookingDates.put("checkout", data.get("checkout"));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("roomid", Integer.parseInt(data.get("roomid")));
        requestBody.put("firstname", data.get("firstname"));
        requestBody.put("lastname", data.get("lastname"));
        requestBody.put("depositpaid", Boolean.parseBoolean(data.get("depositpaid")));
        requestBody.put("bookingdates", bookingDates);
        requestBody.put("email", data.get("email"));
        requestBody.put("phone", data.get("phone"));

        this.bookingPayload = data; // store original for validation

        response = RestAssured.given()
                .baseUri(RestAssured.baseURI)
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/booking")
                .then()
                .log().all()
                .extract()
                .response();

        int statusCode = response.getStatusCode();

        if (statusCode != 200 && statusCode != 201) {
            throw new RuntimeException("Booking creation failed. Status: "
                    + statusCode + " Response: " + response.asString());
        }

        this.bookingId = response.jsonPath().getString("bookingid");

        if (bookingId == null) {
            throw new RuntimeException("bookingId is null after creation.");
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
        response = request.when().post(endpoint).then().log().all().extract().response();
    }

    @When("I send a GET request to {string}")
    public void sendGetRequest(String endpoint) {
        if (endpoint.contains("<id>")) {

            if (bookingId == null) {
                throw new RuntimeException("Booking ID is null. Cannot perform GET.");
            }

            endpoint = endpoint.replace("<id>", bookingId);
        }

        // send GET request with admin token if available
        if (authToken != null && !authToken.isEmpty()) {
            response = RestAssured.given()
                    .baseUri(RestAssured.baseURI)
                    .header("Content-Type", "application/json")
                    .header("Cookie", "token=" + authToken) // <-- add token here
                    .when()
                    .get(endpoint)
                    .then()
                    .log().all()
                    .extract()
                    .response();
        } else {
            // fallback if no token
            response = RestAssured.given()
                    .baseUri(RestAssured.baseURI)
                    .header("Content-Type", "application/json")
                    .when()
                    .get(endpoint)
                    .then()
                    .log().all()
                    .extract()
                    .response();
        }
    }

    @When("I send a PUT request to {string}")
    public void sendPutRequest(String endpoint) {
        if (endpoint.contains("<id>")) {

            if (endpoint.contains("<id>")) {
                if (bookingId == null) {
                    throw new RuntimeException("Booking ID is null. Cannot perform PUT.");
                }
                endpoint = endpoint.replace("<id>", bookingId);                
            }

            if (authToken != null && !authToken.isEmpty()) {
                response = RestAssured.given()
                        .baseUri(RestAssured.baseURI)
                        .contentType("application/json")
                        .header("Cookie", "token=" + authToken)
                        .body(buildBookingJson(bookingPayload)) // 🔥 THIS WAS MISSING
                        .when()
                        .put(endpoint)
                        .then()
                        .log().all()
                        .extract()
                        .response();                
            } else {
                // fallback if no token
                response = RestAssured.given()
                        .baseUri(RestAssured.baseURI)
                        .header("Content-Type", "application/json")
                        .when()
                        .put(endpoint)
                        .then()
                        .log().all()
                        .extract()
                        .response();
            }
        }
    }

    @When("I send a PATCH request to {string}")
    public void sendPatchRequest(String endpoint) {
        endpoint = endpoint.replace("<id>", bookingId);
        response = RestAssured.given().baseUri(RestAssured.baseURI)
                .header("Content-Type", "application/json")
                .body(buildPartialBookingJson(bookingPayload))
                .when().patch(endpoint)
                .then().log().all().extract().response();
    }

    @When("I send a DELETE request to {string}")
    public void sendDeleteRequest(String endpoint) {
        endpoint = endpoint.replace("<id>", bookingId);
        response = RestAssured.given().baseUri(RestAssured.baseURI)
                .header("Content-Type", "application/json")
                .when().delete(endpoint)
                .then().log().all().extract().response();
    }

    // ------------------------------
    // VERIFY RESPONSE
    // ------------------------------
    @Then("the response should be successful")
    public void verifyResponseStatus() {
        assert response != null;
        int status = response.getStatusCode();
        if (!(status == 200 || status == 201)) {
            throw new AssertionError("Expected success status 200/201 but got: " + status);
        }
    }

    @Then("the response should match the booking payload")
    public void verifyResponseMatchesPayload() {
        AssertionUtils.verifyResponseMatchesPayload(response, bookingPayload);
    }

    @Then("the response should match the updated fields")
    public void verifyResponseMatchesUpdatedFields() {
        AssertionUtils.verifyResponseMatchesPayload(response, bookingPayload);
    }

    @Then("the response should indicate a validation error")
    public void verifyValidationErrors() {
        List<String> expectedErrors = List.of("size must be between 3 and 18"); // customize per your API
        AssertionUtils.verifyValidationErrors(response, expectedErrors);
    }

    @Then("the response should indicate booking not found")
    public void verifyBookingNotFound() {
        assert response != null;
        if (response.getStatusCode() != 404) {
            throw new AssertionError("Expected 404 Not Found but got: " + response.getStatusCode());
        }
    }

    @Then("the response should match the booking details")
    public void the_response_should_match_the_booking_details() {
        if (response == null) {
            throw new AssertionError("Response is null, cannot validate booking details");
        }

        // Extract values from response
        // int responseBookingId = response.jsonPath().getInt("bookingid");
        int responseRoomId = response.jsonPath().getInt("roomid");
        String responseFirstname = response.jsonPath().getString("firstname");
        String responseLastname = response.jsonPath().getString("lastname");
        boolean responseDepositPaid = response.jsonPath().getBoolean("depositpaid");
        String responseCheckin = response.jsonPath().getString("bookingdates.checkin");
        String responseCheckout = response.jsonPath().getString("bookingdates.checkout");

        // Compare with expected payload
        if (!String.valueOf(responseRoomId).equals(bookingPayload.get("roomid"))) {
            throw new AssertionError(
                    "Room ID mismatch. Expected: " + bookingPayload.get("roomid") + ", but got: " + responseRoomId);
        }
        if (!responseFirstname.equals(bookingPayload.get("firstname"))) {
            throw new AssertionError("Firstname mismatch. Expected: " + bookingPayload.get("firstname") + ", but got: "
                    + responseFirstname);
        }
        if (!responseLastname.equals(bookingPayload.get("lastname"))) {
            throw new AssertionError("Lastname mismatch. Expected: " + bookingPayload.get("lastname") + ", but got: "
                    + responseLastname);
        }
        if (responseDepositPaid != Boolean.parseBoolean(bookingPayload.get("depositpaid"))) {
            throw new AssertionError("DepositPaid mismatch. Expected: " + bookingPayload.get("depositpaid")
                    + ", but got: " + responseDepositPaid);
        }
        if (!responseCheckin.equals(bookingPayload.get("checkin"))) {
            throw new AssertionError(
                    "Checkin mismatch. Expected: " + bookingPayload.get("checkin") + ", but got: " + responseCheckin);
        }
        if (!responseCheckout.equals(bookingPayload.get("checkout"))) {
            throw new AssertionError("Checkout mismatch. Expected: " + bookingPayload.get("checkout") + ", but got: "
                    + responseCheckout);
        }

        System.out.println("Response matches expected booking payload!");
    }

    // ------------------------------
    // HELPER METHODS TO BUILD JSON
    // ------------------------------
    private String buildBookingJson(Map<String, String> payload) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"roomid\":").append(payload.getOrDefault("roomid", "0")).append(",");
        sb.append("\"firstname\":\"").append(payload.getOrDefault("firstname", "")).append("\",");
        sb.append("\"lastname\":\"").append(payload.getOrDefault("lastname", "")).append("\",");
        sb.append("\"depositpaid\":").append(payload.getOrDefault("depositpaid", "false")).append(",");
        sb.append("\"bookingdates\":{")
                .append("\"checkin\":\"").append(payload.getOrDefault("checkin", "")).append("\",")
                .append("\"checkout\":\"").append(payload.getOrDefault("checkout", "")).append("\"},");
        sb.append("\"email\":\"").append(payload.getOrDefault("email", "")).append("\",");
        sb.append("\"phone\":\"").append(payload.getOrDefault("phone", "")).append("\"}");
        return sb.toString();
    }

    private String buildPartialBookingJson(Map<String, String> payload) {
        StringBuilder sb = new StringBuilder("{");
        boolean firstField = true;
        for (String key : payload.keySet()) {
            if (!firstField)
                sb.append(",");
            sb.append("\"").append(key).append("\":");
            String value = payload.get(key);
            // handle boolean or string
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                sb.append(value);
            } else {
                sb.append("\"").append(value).append("\"");
            }
            firstField = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
