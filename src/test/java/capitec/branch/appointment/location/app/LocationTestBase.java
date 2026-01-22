package capitec.branch.appointment.location.app;

import capitec.branch.appointment.AppointmentBookingApplicationTests;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for Location Use Case tests.
 * Sets up WireMock for Capitec Branch API.
 */
abstract class LocationTestBase extends AppointmentBookingApplicationTests {

    @Autowired
    protected FindNearestBranchesUseCase findNearestBranchesUseCase;

    @Autowired
    protected SearchBranchesByAreaUseCase searchBranchesByAreaUseCase;

    protected WireMock capitecApiWireMock;

    protected static final String CAPITEC_BRANCH_API_RESPONSE = """
            {
                "Branches": [
                    {
                        "Id": null,
                        "Code": "470010",
                        "Latitude": -33.960553,
                        "Longitude": 18.470156,
                        "Name": "Rondebosch",
                        "AddressLine1": "Shop G21, Cnr Main & Belmont Road, Fountain Centre, Rondebosch, 7700",
                        "AddressLine2": "Fountain Centre",
                        "OpeningHours": "Monday - Friday, 8am - 5pm",
                        "SaturdayHours": "Saturday, 8am - 1pm",
                        "SundayHours": "Closed on Sundays",
                        "PublicHolidayHours": "Closed on Public Holidays",
                        "City": "Rondebosch",
                        "Province": "Western Cape",
                        "IsAtm": false,
                        "CashAccepting": false,
                        "HandlesHomeLoans": false,
                        "IsClosed": false,
                        "BusinessBankCenter": false
                    },
                    {
                        "Id": null,
                        "Code": "470020",
                        "Latitude": -33.925839,
                        "Longitude": 18.423622,
                        "Name": "Cape Town CBD",
                        "AddressLine1": "Shop 5, Cape Town Station Building, Adderley Street",
                        "AddressLine2": null,
                        "OpeningHours": "Monday - Friday, 8am - 5pm",
                        "SaturdayHours": "Saturday, 8am - 1pm",
                        "SundayHours": "Closed on Sundays",
                        "PublicHolidayHours": "Closed on Public Holidays",
                        "City": "Cape Town",
                        "Province": "Western Cape",
                        "IsAtm": false,
                        "CashAccepting": false,
                        "HandlesHomeLoans": true,
                        "IsClosed": false,
                        "BusinessBankCenter": true
                    },
                    {
                        "Id": null,
                        "Code": "470030",
                        "Latitude": -33.917546,
                        "Longitude": 18.421345,
                        "Name": "V&A Waterfront",
                        "AddressLine1": "Shop 123, Victoria Wharf Shopping Centre",
                        "AddressLine2": "V&A Waterfront",
                        "OpeningHours": "Monday - Friday, 9am - 6pm",
                        "SaturdayHours": "Saturday, 9am - 5pm",
                        "SundayHours": "Sunday, 10am - 4pm",
                        "PublicHolidayHours": "Public Holidays, 10am - 4pm",
                        "City": "Cape Town",
                        "Province": "Western Cape",
                        "IsAtm": false,
                        "CashAccepting": false,
                        "HandlesHomeLoans": false,
                        "IsClosed": false,
                        "BusinessBankCenter": false
                    },
                    {
                        "Id": "SAS29340",
                        "Code": null,
                        "Latitude": -25.7751312,
                        "Longitude": 29.4944725,
                        "Name": "Total Rondebosch Vulstasie",
                        "AddressLine1": "Total Rondebosch Vulstasie, Corner of N11 and Cowan Ntuli Street, Mpumalanga",
                        "AddressLine2": null,
                        "OpeningHours": null,
                        "SaturdayHours": null,
                        "SundayHours": null,
                        "PublicHolidayHours": null,
                        "City": "Middelburg",
                        "Province": "Mpumalanga",
                        "IsAtm": true,
                        "CashAccepting": false,
                        "HandlesHomeLoans": false,
                        "IsClosed": false,
                        "BusinessBankCenter": false
                    }
                ]
            }
            """;

    protected static final String EMPTY_BRANCH_RESPONSE = """
            {
                "Branches": []
            }
            """;

    @BeforeEach
    void setupLocationBase() {
        capitecApiWireMock = new WireMock(
                wiremockClientDomainServer.getHost(),
                wiremockClientDomainServer.getFirstMappedPort()
        );
        // Reset any previous stubs
        capitecApiWireMock.resetMappings();
    }

    protected void stubCapitecBranchApiForCoordinates(double latitude, double longitude) {
        capitecApiWireMock.register(
                WireMock.post(WireMock.urlPathEqualTo("/Branch"))
                        .withRequestBody(WireMock.containing("\"Latitude\""))
                        .withRequestBody(WireMock.containing("\"Longitude\""))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(CAPITEC_BRANCH_API_RESPONSE))
        );
    }

    protected void stubCapitecBranchApiForArea(String searchText) {
        capitecApiWireMock.register(
                WireMock.post(WireMock.urlPathEqualTo("/Branch"))
                        .withRequestBody(WireMock.containing("\"Query\""))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(CAPITEC_BRANCH_API_RESPONSE))
        );
    }

    protected void stubCapitecBranchApiEmptyResponse() {
        capitecApiWireMock.register(
                WireMock.post(WireMock.urlPathEqualTo("/Branch"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(EMPTY_BRANCH_RESPONSE))
        );
    }

    protected void stubCapitecBranchApiError() {
        capitecApiWireMock.register(
                WireMock.post(WireMock.urlPathEqualTo("/Branch"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(500)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"error\": \"Internal Server Error\"}"))
        );
    }
}

