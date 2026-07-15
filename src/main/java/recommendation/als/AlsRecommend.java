package recommendation.als;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import recommendation.models.neo4j.CharacteristicVector;

import java.util.stream.Stream;

public class AlsRecommend extends AlsProcedure {

    @Procedure(name = "recommendation.als.recommend", mode = Mode.READ)
    @Description(value = """
            This procedure is used to perform the recommend operation of the als algorithm.
            Inputs:
            userNodeId: nodeId of the user you wish to recommend the item
            itemNodeId: nodeId of the item you wish to recommend to the user
            """)
    public Stream<BaseProcedure.DoubleOutput> recommend(@Name("userNodeId") Long userNodeId, @Name("itemNodeId") Long itemNodeId) {
        CharacteristicVector userVector = getCharacteristicVector(userNodeId);

        CharacteristicVector itemVector = getCharacteristicVector(itemNodeId);

        if (userVector.getLength() != itemVector.getLength()) {
            throw new RuntimeException("Cannot calculate the recommendation: characteristic vectors have different lengths");
        }
        double recommendation = 0.0f;
        for (int i = 0; i < userVector.getLength(); i++) {
            recommendation += userVector.doubleValue(i) * itemVector.doubleValue(i);
        }
        return Stream.of(new BaseProcedure.DoubleOutput(recommendation));
    }

}
