package com.ruky.training.postgrest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ruky.training.domain.AthleteTier;
import com.ruky.training.domain.CompetitionRegistration;
import com.ruky.training.domain.CompetitionResult;
import com.ruky.training.domain.RegistrationStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CompetitionRepositoryTest {

    private static final UUID COMP_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID DIVISION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ATHLETE_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID EVENT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private MockRestServiceServer server;
    private CompetitionRepository repository;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://postgrest.test")
                .messageConverters(converters ->
                        converters.add(0, new MappingJackson2HttpMessageConverter(mapper)));
        server = MockRestServiceServer.bindTo(builder).build();
        repository = new CompetitionRepository(builder.build());
    }

    @Test
    void shouldReadRegistrationsWithEmbeddedAthlete() {
        String body = """
                [{
                  "id": "dddddddd-dddd-dddd-dddd-dddddddddddd",
                  "competition_id": "%s",
                  "division_id": "%s",
                  "athlete_id": "%s",
                  "status": "REGISTERED",
                  "athlete": {
                    "id": "%s", "handle": "ruky", "display_name": "Ruky",
                    "sex": "MALE", "tier": "ELITE", "disciplines": ["CROSSFIT"]
                  }
                }]
                """.formatted(COMP_ID, DIVISION_ID, ATHLETE_ID, ATHLETE_ID);

        server.expect(requestTo(startsWith("http://postgrest.test/competition_registrations?")))
                .andExpect(queryParam("select", "*,athlete:athletes(*)"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        CompetitionRegistration registration = repository.findRegistrations(COMP_ID).get(0);

        assertThat(registration.status()).isEqualTo(RegistrationStatus.REGISTERED);
        assertThat(registration.athlete().displayName()).isEqualTo("Ruky");
        assertThat(registration.athlete().tier()).isEqualTo(AthleteTier.ELITE);
        server.verify();
    }

    @Test
    void shouldUpsertResultWithConflictTarget() {
        String body = """
                [{
                  "id": "ffffffff-ffff-ffff-ffff-ffffffffffff",
                  "competition_id": "%s",
                  "event_id": "%s",
                  "athlete_id": "%s",
                  "raw_score": 240.0,
                  "display_score": "04:00"
                }]
                """.formatted(COMP_ID, EVENT_ID, ATHLETE_ID);

        server.expect(requestTo(startsWith("http://postgrest.test/competition_results?")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(queryParam("on_conflict", "event_id,athlete_id"))
                .andExpect(header("Prefer", "resolution=merge-duplicates,return=representation"))
                .andExpect(jsonPath("$.raw_score").value(240.0))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        CompetitionResult result = repository.upsertResult(new ResultWrite(COMP_ID, EVENT_ID,
                ATHLETE_ID, new BigDecimal("240"), "04:00", null));

        assertThat(result.displayScore()).isEqualTo("04:00");
        assertThat(result.rawScore()).isEqualByComparingTo("240");
        server.verify();
    }

    @Test
    void shouldUpsertRegistrationWithConflictTarget() {
        String body = """
                [{
                  "id": "dddddddd-dddd-dddd-dddd-dddddddddddd",
                  "competition_id": "%s",
                  "division_id": "%s",
                  "athlete_id": "%s",
                  "status": "REGISTERED"
                }]
                """.formatted(COMP_ID, DIVISION_ID, ATHLETE_ID);

        server.expect(requestTo(startsWith("http://postgrest.test/competition_registrations?")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(queryParam("on_conflict", "competition_id,athlete_id"))
                .andExpect(header("Prefer", "resolution=merge-duplicates,return=representation"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        CompetitionRegistration registration = repository.upsertRegistration(
                new RegistrationWrite(COMP_ID, DIVISION_ID, ATHLETE_ID, RegistrationStatus.REGISTERED));

        assertThat(registration.athleteId()).isEqualTo(ATHLETE_ID);
        server.verify();
    }
}
