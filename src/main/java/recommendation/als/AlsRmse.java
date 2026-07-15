package recommendation.als;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import recommendation.models.Matrix;

import java.util.stream.Stream;

public class AlsRmse extends AlsProcedure {

    @Procedure(name = "recommendation.als.rmse", mode = Mode.READ)
    @Description(value = """
            This procedure is used to calculate the rmse of the als algorithm.
            Inputs:
            user: the class name of the user nodes
            item: the class name of the item nodes
            relationship: the class name of the relationship between the user and item nodes
            value: the name of the value in the relationship that is supposed to be predicted via als
            """)
    public Stream<DoubleOutput> rmse(@Name("relationship") String relationship, @Name("user") String user, @Name("item") String item, @Name("value") String value) {
        Matrix userItemMatrix = getUserItemMatrix(relationship, user, item, value);

        Matrix userFactors = getCharacteristicFactors(user);
        Matrix itemFactors = getCharacteristicFactors(item);

        Matrix recommendations = userFactors
                .dot(itemFactors.transpose());
        if (!recommendations.isNonZero()) {
            throw new RuntimeException("Cannot calculate RMSE: Recommendations could not be generated");
        }
        if (!userItemMatrix.isNonZero()) {
            throw new RuntimeException("Cannot calculate RMSE: No real values given");
        }
        if (recommendations.getNumberOfRows() != userItemMatrix.getNumberOfRows()
                || recommendations.getNumberOfColumns() != userItemMatrix.getNumberOfColumns()) {
            throw new RuntimeException("Cannot calculate RMSE: Matrix dimensions do not match");
        }

        double rmse = 0;
        int total = 0;
        double[] flatRecommendations = recommendations.flat();
        double[] flatRealValues = userItemMatrix.flat();
        for (int i = 0; i < flatRealValues.length; i++) {
            if (flatRealValues[i] != 0) {
                rmse += Math.pow(flatRealValues[i] - flatRecommendations[i], 2);
                total++;
            }
        }
        rmse = Math.sqrt(rmse / total);
        return Stream.of(new DoubleOutput(rmse));
    }

}
