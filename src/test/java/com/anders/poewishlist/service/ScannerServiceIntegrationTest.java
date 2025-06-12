package com.anders.poewishlist.service;

import com.anders.poewishlist.api.HttpStashApiClient;
import com.anders.poewishlist.db.CursorStore;
import com.anders.poewishlist.db.DatabaseCursorStore;
import com.anders.poewishlist.db.DatabaseWishlistStore;
import com.anders.poewishlist.db.WishlistStore;
import com.anders.poewishlist.util.UniqueItemMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.JDA;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

/**
 * Integration test for ScannerService using WireMock to simulate PoE API.
 */
public class ScannerServiceIntegrationTest {
    private WireMockServer wireMock;
    private ScannerService service;
    private TextChannel channel;
    private JDA jda;
    private CursorStore cursorStore;
    private MessageCreateAction mockAction;

    @BeforeEach
    void before() throws Exception {
        // Start WireMock on a dynamic port
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        WireMock.configureFor("localhost", wireMock.port());

        // Stub scenario: two 429s then valid JSON
        wireMock.stubFor(get(urlPathEqualTo("/public-stash-tabs"))
                .inScenario("Retry")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(429))
                .willSetStateTo("SECOND"));

        wireMock.stubFor(get(urlPathEqualTo("/public-stash-tabs"))
                .inScenario("Retry")
                .whenScenarioStateIs("SECOND")
                .willReturn(aResponse().withStatus(429))
                .willSetStateTo("THIRD"));

        wireMock.stubFor(get(urlPathEqualTo("/public-stash-tabs"))
                .inScenario("Retry")
                .whenScenarioStateIs("THIRD")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("stash-response-match.json"))
                .willSetStateTo("Done"));

        // Hikari in-memory SQLite
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite::memory:");
        DataSource ds = new HikariDataSource(config);
        cursorStore = new DatabaseCursorStore(ds);
        WishlistStore wishlist = new DatabaseWishlistStore(ds);
        wishlist.addWish("user123", "Tabula Rasa");

        // Mock JDA channel
        jda = mock(JDA.class);
        channel = mock(TextChannel.class);
        mockAction = mock(MessageCreateAction.class);
        when(channel.sendMessage(anyString())).thenReturn(mockAction);
        doNothing().when(mockAction).queue();
        when(jda.getTextChannelById(anyString())).thenReturn(channel);



        // Service setup
        UniqueItemMatcher matcher = new UniqueItemMatcher();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        service = new ScannerService(
                new HttpStashApiClient(
                        "http://localhost:" + wireMock.port() + "/public-stash-tabs",
                        java.net.http.HttpClient.newHttpClient(),
                        new ObjectMapper()),
                cursorStore,
                wishlist,
                matcher,
                jda,
                "channelId",
                "Standard",
                1,
                scheduler,
                millis -> {}
        );
    }

    @AfterEach
    void after() {
        wireMock.stop();
    }

    @Test
    void integrationFlowUpdatesCursorAndSendsPing() throws Exception {
        service.scanWithRetry();

        // Verificer at sendMessage() blev kaldt
        verify(channel).sendMessage(contains("has found Tabula Rasa"));

        // Verificer at queue() blev kaldt på vores mockAction
        verify(mockAction).queue();
    }
}