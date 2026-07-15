package recommendation.als;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import recommendation.als.service.AlsService;
import recommendation.models.AlsFitResult;
import recommendation.models.Matrix;

import java.util.Map;
import java.util.stream.Stream;

public class AlsFit extends AlsProcedure {

//    @Procedure(name = "recommendation.als.fit", mode = Mode.WRITE)
//    @Description(value = """
//            This procedure is used to perform the fit operation of the als algorithm.
//            Inputs:
//            user: the class name of the user nodes
//            item: the class name of the item nodes
//            relationship: the class name of the relationship between the user and item nodes
//            valueName: the name of the value in the relationship that is supposed to be predicted via als
//            iterations: the amount of iterations that the als algorithms is supposed to perform
//            regulation: the regulation coefficient for the als algorithm
//            factors: tha amount of factors that the generated characteristic vectors will have
//            """)
//    public Stream<BooleanOutput> fit(
//            @Name("user") String user,
//            @Name("item") String item,
//            @Name("relationship") String relationship,
//            @Name("value") String value,
//            @Name("iterations") long iterations,
//            @Name("regulation") double regulation,
//            @Name("factors") long factors
//    ) {
//        // pulling necessary values
//        Map<Long, Integer> userIdMap = getNodeIdMap(user);
//
//        Map<Long, Integer> itemIdMap = getNodeIdMap(item);
//
//        Matrix userItemMatrix = getUserItemMatrix(relationship, user, item, value);
//
//        // calculate characteristic vectors
//        AlsFitResult fitResult = AlsService.fit(userItemMatrix, (int) iterations, (float) regulation, (int) factors);
//
//        // set als characteristic vectors for users
//        return getBooleanOutputStream(userIdMap, itemIdMap, fitResult);
//    }

    @Procedure(name = "recommendation.als.fit", mode = Mode.WRITE)
    @Description(value = """
            This procedure is used to perform the fit operation of the als algorithm.
            Inputs:
            user: the class name of the user nodes
            item: the class name of the item nodes
            relationship: the class name of the relationship between the user and item nodes
            value: the name of the value in the relationship that is supposed to be predicted via als
            iterations: the amount of iterations that the als algorithms is supposed to perform
            regulation: the regulation coefficient for the als algorithm
            factors: tha amount of factors that the generated characteristic vectors will have
            seed: seed for randomization (use "default" for random value)
            """)
    public Stream<BooleanOutput> fit(
            @Name("user") String user,
            @Name("item") String item,
            @Name("relationship") String relationship,
            @Name("value") String value,
            @Name("iterations") long iterations,
            @Name("regulation") double regulation,
            @Name("factors") long factors,
            @Name(value = "seed", defaultValue = "0") long seed
    ) {
        // pulling necessary values
        Map<Long, Integer> userIdMap = getNodeIdMap(user);

        Map<Long, Integer> itemIdMap = getNodeIdMap(item);

        Matrix userItemMatrix = getUserItemMatrix(relationship, user, item, value);

        // calculate characteristic vectors
        AlsFitResult fitResult;
        if (seed == 0) {
            fitResult = AlsService.fit(userItemMatrix, (int) iterations, (float) regulation, (int) factors);
        } else {
            fitResult = AlsService.fit(userItemMatrix, (int) iterations, (float) regulation, (int) factors, seed);
        }

        // set als characteristic vectors for users
        Matrix userFactors = fitResult.getUserFactors();
        userIdMap.forEach((userId, index) -> {
            setNodeProperty(userId, PROPERTY_ALS, userFactors.getRow(index).flat());
        });

        // set als characteristic vectors for items
        Matrix itemFactors = fitResult.getItemFactors();
        itemIdMap.forEach((itemId, index) -> {
            setNodeProperty(itemId, PROPERTY_ALS, itemFactors.getRow(index).flat());
        });

        return Stream.of(new BooleanOutput(true));
    }

//    private Stream<BooleanOutput> getBooleanOutputStream(Map<Long, Integer> userIdMap, Map<Long, Integer> itemIdMap, AlsFitResult fitResult) {
//        Matrix userFactors = fitResult.getUserFactors();
//        userIdMap.forEach((userId, index) -> {
//            setNodeProperty(userId, PROPERTY_ALS, userFactors.getRow(index).flat());
//        });
//
//        // set als characteristic vectors for items
//        Matrix itemFactors = fitResult.getItemFactors();
//        itemIdMap.forEach((itemId, index) -> {
//            setNodeProperty(itemId, PROPERTY_ALS, itemFactors.getRow(index).flat());
//        });
//
//        return Stream.of(new BooleanOutput(true));
//    }

}
