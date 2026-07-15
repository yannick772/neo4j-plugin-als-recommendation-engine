package recommendation.als;

import recommendation.models.neo4j.CharacteristicVector;
import recommendation.models.Matrix;
import recommendation.models.neo4j.UserItemRelationship;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AlsProcedure extends BaseProcedure {

    protected static String PROPERTY_ALS = "als";

    protected Map<Long, Integer> getNodeIdMap(String nodeClass) {
        String query = "MATCH (n:" + nodeClass + ") " +
                "RETURN DISTINCT id(n) as id " +
                "ORDER BY id ASC";
        List<Long> ids = queryForIds(query);
        return IntStream.range(0, ids.size())
                .boxed()
                .collect(Collectors.toMap(
                        ids::get,
                        index -> index
                ));
    }

    protected Matrix getUserItemMatrix(String relationClass, String userCLass, String itemClass, String propertyName) {
        List<UserItemRelationship> relations = getRelations(relationClass, userCLass, itemClass, propertyName);
        Map<Long, Integer> userIdMap = getNodeIdMap(userCLass);
        Map<Long, Integer> itemIdMap = getNodeIdMap(itemClass);
        Matrix userItemMatrix = new Matrix(userIdMap.size(), itemIdMap.size());
        relations.forEach(r -> {
            userItemMatrix.setCell(
                    userIdMap.get(r.getUserId()),
                    itemIdMap.get(r.getItemId()),
                    r.getRating()
            );
        });
        return userItemMatrix;
    }

    protected List<UserItemRelationship> getRelations(String relationClass, String userClass, String itemClass, String propertyName) {
        String queryR = "MATCH (u:" + userClass + ")-[r:" + relationClass + "]->(i:" + itemClass + ") " +
                "RETURN id(u) AS userId, r." + propertyName + " AS rating, id(i) AS itemId " +
                "ORDER BY userId ASC, " +
                "itemId ASC";
        return queryList(queryR, UserItemRelationship.class);
    }

    protected Matrix getCharacteristicFactors(String nodeClass) {
        String query = "MATCH (u:" + nodeClass + ") " +
                "RETURN u." + PROPERTY_ALS + " AS factors " +
                "ORDER BY id(u) ASC";
        List<CharacteristicVector> vectors = queryList(query, CharacteristicVector.class);
        if (vectors.isEmpty()) {
            throw new RuntimeException("Could not get Characteristic vectors: No vectors found for class " + nodeClass);
        }
        List<Integer> vectorLengths = vectors.stream()
                .mapToInt(CharacteristicVector::getLength)
                .distinct()
                .boxed()
                .toList();
        if (vectorLengths.size() > 1) {
            throw new RuntimeException("Could not get Characteristic vectors: vectors have differing lengths for class " + nodeClass);
        }
        Matrix vectorMatrix = new Matrix(vectors.size(), vectorLengths.stream().findFirst().orElse(0));
        for (int i = 0; i < vectorMatrix.getNumberOfRows(); i++) {
            CharacteristicVector vector = vectors.get(i);
            for (int j = 0; j < vectorMatrix.getNumberOfColumns(); j++) {
                vectorMatrix.setCell(i, j, vector.floatValue(j));
            }
        }
        return vectorMatrix;
    }

    protected CharacteristicVector getCharacteristicVector(Long nodeId) {
        String queryUser = "MATCH (n) " +
                "WHERE id(n)=$nodeId " +
                "RETURN n." + PROPERTY_ALS + " AS factors";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("nodeId", nodeId);
        return querySingle(queryUser, parameters, CharacteristicVector.class);
    }
}
