Feature: Booking API automation
    In order to manage hotel bookings
    As a tester
    I want to perform CRUD operations via API

    #####################################
    # CREATE
    #####################################

    @create
    Scenario Outline: Create a booking successfully
        Given a booking payload
            | roomid   | firstname   | lastname   | depositpaid   | checkin   | checkout   | email   | phone   |
            | <roomid> | <firstname> | <lastname> | <depositpaid> | <checkin> | <checkout> | <email> | <phone> |
        When I send a POST request to "/booking"
        Then the response should be successful
        And the response should match the booking payload

        Examples:
            | roomid | firstname | lastname | depositpaid | checkin    | checkout   | email                   | phone       |
            | 27     | Revina    | rog      | true        | 2026-03-13 | 2026-04-15 | evinss.rog@example.com  | 31062117436 |
            | 29     | Rowanna   | rog      | true        | 2026-03-13 | 2026-04-15 | Rowanna.rog@example.com | 31062627436 |

    @create
    Scenario Outline: Create booking with invalid values
        Given a booking payload
            | roomid   | firstname   | lastname   | depositpaid   | checkin   | checkout   | email   | phone   |
            | <roomid> | <firstname> | <lastname> | <depositpaid> | <checkin> | <checkout> | <email> | <phone> |
        When I send a POST request to "/booking"
        Then the response should indicate a validation error

        Examples:
            | roomid | firstname | lastname                 | depositpaid | checkin    | checkout   | email            | phone       |
            | 27     | ab        | rog                      | true        | 2026-03-13 | 2026-04-15 | evin@example.com | 31062817436 |
            | 28     | evin      | itistwentydigitslastname | true        | 2026-03-13 | 2026-04-15 | evin@example.com | 31062817436 |

    #####################################
    # GET
    #####################################

    @get
    Scenario: Retrieve an existing booking by ID
        Given I create a booking with payload
            | roomid | firstname | lastname | depositpaid | checkin    | checkout   | email                | phone       |
            | 219    | jaces     | jony     | true        | 2026-03-13 | 2026-04-15 | jacyes.j@example.com | 31025435879 |
        And I am logged in as admin
        When I send a GET request to "/booking/<id>"
        Then the response should be successful
        And the response should match the booking details


    @get
    Scenario Outline: Retrieve a non-existing booking
        Given I am logged in as admin
        When I send a GET request to "/booking/<id>"
        Then the response should indicate booking not found

        Examples:
            | id  |
            | 999 |


    #####################################
    # UPDATE
    #####################################

    @update
    Scenario Outline: Update a booking completely
        Given I create a booking with payload
            | roomid | firstname | lastname | depositpaid | checkin    | checkout   | email        | phone       |
            | 12     | Tempdata1 | User     | true        | 2026-01-01 | 2026-01-05 | tdata1@t.com | 31021424079 |
        And I am logged in as admin
        And a booking payload
            | roomid   | firstname   | lastname   | depositpaid   | checkin   | checkout   | email   | phone   |
            | <roomid> | <firstname> | <lastname> | <depositpaid> | <checkin> | <checkout> | <email> | <phone> |
        When I send a PUT request to "/booking/<id>"
        Then the response should be successful
        When I send a GET request to "/booking/<id>"
        Then the response should be successful
        And the response should match the booking details

        Examples:
            | roomid | firstname | lastname | depositpaid | checkin    | checkout   | email               | phone       |
            | 25     | Jones     | Does     | true        | 2026-03-13 | 2026-04-15 | jon.doe@example.com | 31025465879 |

    @todo
    Scenario Outline: Partially update a booking
        Given I am logged in as admin
        And a booking payload
            | firstname   | lastname   | depositpaid   |
            | <firstname> | <lastname> | <depositpaid> |
        And a booking exists with ID <id>
        When I send a PATCH request to "/booking/<id>"
        Then the response should be successful
        And the response should match the updated fields

        Examples:
            | id | firstname | lastname | depositpaid |
            | 4  | Jane      | Doe      | false       |

    #####################################
    # DELETE
    #####################################

    @delete
    Scenario Outline: Delete an existing booking
        Given a booking exists with ID <id>
        When I send a DELETE request to "/booking/<id>"
        Then the response should be successful

        Examples:
            | id |
            | 4  |
            | 5  |

    @delete
    Scenario Outline: Delete a non-existing booking
        When I send a DELETE request to "/booking/<id>"
        Then the response should indicate booking not found

        Examples:
            | id  |
            | 999 |
            | 888 |
