package recommendation.als;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ClientException;

public class AlsRmseTest extends AlsBaseTest<AlsRmse> {

    @Test
    public void testRmse() {
        // arrange
        loadFauxDataset("testDatabaseAls");
        double expected = 0.398556221042162d;
        try (Session session = driver.session()) {
            // act
            Result result = session.run("CALL recommendation.als.rmse(\"RATING\", \"User\", \"Movie\", \"rating\") YIELD value RETURN value");

            // assert
            Assertions.assertThat(result.single().get("value").asDouble()).isEqualTo(expected);
        }
    }

    @Test
    public void testRmse_lengthMismatch() {
        // arrange
        loadFauxDataset("testDatabaseAls");

        try (Session session = driver.session()) {
            session.run("MATCH (m:Movie) WHERE m.movieId=1 SET m.als=[0.445, 1.582, 0.446, 0.585]").consume();
            session.run("MATCH (u:User) WHERE u.userId=1 SET u.als=[0.357, 1.138, 0.787, 1.364]").consume();

            // act & assert
            Assertions.assertThatThrownBy(() -> {
                session.run("CALL recommendation.als.rmse(\"RATING\", \"User\", \"Movie\", \"rating\") YIELD value RETURN value").single();
            })
            .isInstanceOf(ClientException.class)
            .hasMessage("Failed to invoke procedure `recommendation.als.rmse`: Caused by: java.lang.RuntimeException: Could not get Characteristic vectors: vectors have differing lengths for class User");
        }
    }

    @Test
    public void testRmse_noRecommendations() {
        // arrange
        loadFauxDataset("testDatabase");

        try (Session session = driver.session()) {
            // act & assert
            Assertions.assertThatThrownBy(() -> {
                        session.run("CALL recommendation.als.rmse(\"RATING\", \"User\", \"Movie\", \"rating\") YIELD value RETURN value").single();
                    })
                    .isInstanceOf(ClientException.class)
                    .hasMessage("Failed to invoke procedure `recommendation.als.rmse`: Caused by: java.lang.RuntimeException: Cannot calculate RMSE: Recommendations could not be generated");
        }
    }

    @Test
    public void testRmse_noValuesPresent() {
        // arrange
        loadFauxDataset("testDatabaseAls");

        try (Session session = driver.session()) {
            session.run("MATCH (:User)-[r:RATING]->(:Movie) SET r.rating=0");

            // act & assert
            Assertions.assertThatThrownBy(() -> {
                        session.run("CALL recommendation.als.rmse(\"RATING\", \"User\", \"Movie\", \"rating\") YIELD value RETURN value").single();
                    })
                    .isInstanceOf(ClientException.class)
                    .hasMessage("Failed to invoke procedure `recommendation.als.rmse`: Caused by: java.lang.RuntimeException: Cannot calculate RMSE: No real values given");
        }
    }

}
