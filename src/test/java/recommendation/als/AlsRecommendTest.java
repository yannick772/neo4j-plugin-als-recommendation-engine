package recommendation.als;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.ClientException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AlsRecommendTest extends AlsBaseTest<AlsRecommend> {

    @Test
    public void testRecommend() {
        float expected = 5;
        float[] userFactors = new float[5];
        Arrays.fill(userFactors, 2f);
        float[] itemFactors = new float[5];
        Arrays.fill(itemFactors, 0.5f);

        try (Session session = driver.session()) {
            // arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("userVector", userFactors);
            parameters.put("itemVector", itemFactors);

            long userId = session.run("CREATE (u:User {als:$userVector}) RETURN id(u) AS nodeId", parameters).single().get("nodeId").asLong();
            long itemId = session.run("CREATE (i:Item {als:$itemVector}) RETURN id(i) AS nodeId", parameters).single().get("nodeId").asLong();

            // act
            parameters.clear();
            parameters.put("userId", userId);
            parameters.put("itemId", itemId);
            Record record = session.executeWrite(tx -> tx.run("CALL recommendation.als.recommend($userId, $itemId) YIELD value RETURN value", parameters).single());

            // assert
            Assertions.assertThat(record.get("value").asFloat()).isEqualTo(expected);
        }
    }

    @Test
    public void testRecommend_lengthMismatch() {
        float[] userFactors = new float[4];
        Arrays.fill(userFactors, 2f);
        float[] itemFactors = new float[5];
        Arrays.fill(itemFactors, 0.5f);

        try (Session session = driver.session()) {
            // arrange
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("userVector", userFactors);
            parameters.put("itemVector", itemFactors);

            long userId = session.run("CREATE (u:User {als:$userVector}) RETURN id(u) AS nodeId", parameters).single().get("nodeId").asLong();
            long itemId = session.run("CREATE (i:Item {als:$itemVector}) RETURN id(i) AS nodeId", parameters).single().get("nodeId").asLong();

            // act & assert
            parameters.clear();
            parameters.put("userId", userId);
            parameters.put("itemId", itemId);
            Assertions.assertThatThrownBy(() -> {
                session.run("CALL recommendation.als.recommend($userId, $itemId) YIELD value RETURN value", parameters).consume();
            })
            .isInstanceOf(ClientException.class)
            .hasMessage("Failed to invoke procedure `recommendation.als.recommend`: Caused by: java.lang.RuntimeException: Cannot calculate the recommendation: characteristic vectors have different lengths");
        }
    }

}
