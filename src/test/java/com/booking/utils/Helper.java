package com.booking.utils;

import io.cucumber.java.Scenario;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Helper {
    // ----------------------
    // Positive scenario: check response matches expected payload
    // ----------------------
    public static void verifyResponseMatchesPayload(Response response, Map<String, String> expected) {
        if (response == null) {
            throw new AssertionError("Response is null. Cannot validate payload.");
        }

        Map<String, Object> actual = response.jsonPath().getMap(""); // Root response map
        List<String> errors = new ArrayList<>();

        System.out.println("==== Verifying response for scenario ====");

        for (String key : expected.keySet()) {
            try {
                if ("checkin".equals(key) || "checkout".equals(key)) {
                    // Handle bookingdates separately
                    Map<String, String> bookingDates = response.jsonPath().getMap("bookingdates", String.class,
                            String.class);
                    if (bookingDates.containsKey(key)) {
                        String expectedValue = expected.get(key);
                        String actualValue = bookingDates.get(key);
                        if (!expectedValue.equals(actualValue)) {
                            errors.add("Mismatch for '" + key + "': expected [" + expectedValue + "] but was ["
                                    + actualValue + "]");
                        }
                    } else {
                        errors.add("Expected key '" + key + "' is missing in bookingdates!");
                    }
                } else {
                    if (actual.containsKey(key)) {
                        String expectedValue = expected.get(key);
                        String actualValue = String.valueOf(actual.get(key));
                        if (!expectedValue.equals(actualValue)) {
                            errors.add("Mismatch for '" + key + "': expected [" + expectedValue + "] but was ["
                                    + actualValue + "]");
                        }
                    } else {
                        errors.add("Expected key '" + key + "' is missing in response!");
                    }
                }
            } catch (Exception e) {
                errors.add("Exception while verifying key '" + key + "': " + e.getMessage());
            }
        }

        // Throw a single assertion with all details if there are errors
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Response validation failed with ").append(errors.size()).append(" issue(s):\n");
            errors.forEach(err -> sb.append(" - ").append(err).append("\n"));
            throw new AssertionError(sb.toString());
        }

        System.out.println("Response matches expected booking payload!");
    }

    // ----------------------
    // Negative scenario: check validation error response
    // ----------------------
    public static void verifyValidationErrors(Response response, List<String> expectedErrors) {
        Assertions.assertEquals(400, response.getStatusCode(), "Expected 400 Bad Request");

        List<String> actualErrors = response.jsonPath().getList("errors");
        Assertions.assertNotNull(actualErrors, "Errors array should be present");

        StringBuilder missingErrors = new StringBuilder();
        for (String expected : expectedErrors) {
            if (!actualErrors.contains(expected)) {
                missingErrors.append("Expected error message not found: ").append(expected).append("\n");
            }
        }

        if (missingErrors.length() > 0) {
            Assertions.fail("Validation error check failed! See errors above.\n" + missingErrors);
        }
    }

    // ------------------------------
    // Helper to convert DataTable to nested request payload
    // ------------------------------
    public static Map<String, Object> buildBookingRequestBody(Map<String, String> data) {
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

        return requestBody;
    }

    // ------------------------------
    // Attach request/response log to the scenario
    // ------------------------------
    public static void attachRequestResponse(Scenario scenario, Response response, String method, String endpoint,
            String requestBody) {
        StringBuilder log = new StringBuilder();
        log.append("HTTP Method: ").append(method)
                .append("\nEndpoint: ").append(endpoint);

        if (requestBody != null) {
            log.append("\nRequest Body:\n").append(requestBody);
        }

        if (response != null) {
            log.append("\n\nStatus Code: ").append(response.getStatusCode())
                    .append("\nResponse Body:\n").append(response.getBody().asString());
        }

        scenario.attach(log.toString(), "text/plain", method + " Log");
    }

}
