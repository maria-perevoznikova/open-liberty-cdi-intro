package it.io.openliberty.guides.inventory;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InventoryEndpointConcurrentIT {

    private static String systemsEndpoint;

    private Client client1;
    private Client client2;


    @BeforeAll
    public static void oneTimeSetup() {
        String port = System.getProperty("http.port");
        systemsEndpoint = "http://localhost:" + port + "/inventory/systems";
    }

    @BeforeEach
    public void setup() {
        client1 = ClientBuilder.newClient();
        client2 = ClientBuilder.newClient();

        resetSystems(client1);
    }

    @AfterEach
    public void teardown() {
        client1.close();
        client2.close();
    }

    @Test
    public void testConcurrentHostRegistration() throws InterruptedException {
        // This test fails if InventoryManager#add() method doesn't have 'synchronized' modifier
        String[] registeredHostnames = getSystemHostnames(client1);
        assertEquals(0, registeredHostnames.length);

        new Thread(() -> addLocalhostToSystems(client1)).start();
        new Thread(() -> addLocalhostToSystems(client2)).start();
        Thread.sleep(2000);
        registeredHostnames = getSystemHostnames(client1);

        assertEquals(1, registeredHostnames.length);
        assertEquals("\"localhost\"", registeredHostnames[0]);
    }

    private void resetSystems(Client client) {
        String resetUrl = systemsEndpoint + "/reset";
        Response response = getResponse(client, resetUrl);
        assertResponse(response, resetUrl);
        response.close();
    }

    private void addLocalhostToSystems(Client client) {
        String addToSystemsUrl = systemsEndpoint + "/localhost";
        Response response = getResponse(client, addToSystemsUrl);
        assertResponse(response, addToSystemsUrl);
        response.close();
    }

    private String[] getSystemHostnames(Client client) {
        Response response = getResponse(client, systemsEndpoint);
        this.assertResponse(response, systemsEndpoint);
        JsonArray systems = response.readEntity(JsonObject.class).getJsonArray("systems");
        response.close();

        String[] hostnames = new String[systems.size()];
        for (int i = 0; i < systems.size(); i++) {
            hostnames[i] = systems.getJsonObject(i).get("hostname").toString();
        }
        return hostnames;
    }

    private Response getResponse(Client client, String url) {
        return client.target(url).request().get();
    }

    private void assertResponse(Response response, String url) {
        assertEquals(200, response.getStatus(), "Incorrect response code from " + url);
    }
}
