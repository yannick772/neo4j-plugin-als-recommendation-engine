package recommendation.als;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import recommendation.models.neo4j.CharacteristicVector;

import java.util.Arrays;
import java.util.List;

public class AlsFitTest extends AlsBaseTest<AlsFit> {

    @Test
    public void testFit() {
        ObjectMapper objectMapper = new ObjectMapper();
        try (Session session = driver.session()) {
            // arrange
            loadFauxDataset("testDatabase");

            // act
            Record record = session.run("CALL recommendation.als.fit(\"User\", \"Movie\", \"RATING\", \"rating\", 100, 1, 10, 12345678) YIELD value RETURN value").single();

            // assert
            List<CharacteristicVector> factors = objectMapper.readValue(objectMapper.writeValueAsString(
                    session.run("MATCH (u:User) RETURN u.als AS factors ORDER BY id(u) ASC")
                            .stream()
                            .map(Record::asMap)
                            .toList()
            ), objectMapper.getTypeFactory().constructCollectionType(List.class, CharacteristicVector.class));
            Assertions.assertThat(record.get("result").asBoolean()).isTrue();
            Assertions.assertThat(factors).isNotEmpty();
            Assertions.assertThat(factors).allMatch(x -> x.getLength() == 10);
            Assertions.assertThat(factors)
                    .extracting(CharacteristicVector::getFactors)
                    .allMatch(x -> Arrays.stream(x).allMatch(d -> d != 0));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
