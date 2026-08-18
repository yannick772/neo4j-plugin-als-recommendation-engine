package recommendation.als;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.neo4j.graphdb.*;
import recommendation.BaseProcedure;
import recommendation.models.neo4j.CharacteristicVector;

import java.time.Instant;
import java.util.*;

public class AlsProcedure extends BaseProcedure {

    protected static String PROPERTY_ALS = "als";

    protected Map<String, Integer> getNodeIdMap(String nodeClass) {
        Instant start = Instant.now();
        Map<String, Integer> nodeIdMap = new HashMap<>();
        ResourceIterator<Node> nodeIterator = findNodesByLabel(nodeClass);
        int index = 0;
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.next();
            nodeIdMap.put(node.getElementId(), index++);
        }
        log.info("Pulled %s ids for %s in %s".formatted(nodeIdMap.size(), nodeClass, durationToString(start)));
        return nodeIdMap;
    }

    protected DMatrixSparseCSC getUserItemMatrix(String relationClass, String itemClass, Map<String, Integer> userIdMap, Map<String, Integer> itemIdMap, String propertyName) {
        Instant start = Instant.now();
        int relationshipCount = Math.toIntExact(findRelationshipsByName(relationClass).stream().count());
        DMatrixSparseCSC userItemMatrix = new DMatrixSparseCSC(userIdMap.size(), itemIdMap.size(), relationshipCount);
        ResourceIterator<Node> items = findNodesByLabel(itemClass);
        int nzPointer = 0;
        while (items.hasNext()) {
            Node item = items.next();
            int itemIndex = itemIdMap.get(item.getElementId());
            ResourceIterable<Relationship> relationships = item.getRelationships(Direction.INCOMING, RelationshipType.withName(relationClass));
            for (Relationship relationship : relationships) {
                Node user = relationship.getStartNode();
                int userIndex = userIdMap.get(user.getElementId());
                double rating = ((Number) relationship.getProperty(propertyName)).doubleValue();
                userItemMatrix.nz_rows[nzPointer] = userIndex;
                userItemMatrix.nz_values[nzPointer] = rating;
                nzPointer++;
            }
            userItemMatrix.col_idx[itemIndex + 1] = nzPointer;
        }
        userItemMatrix.nz_length = relationshipCount;
        log.info("Created %sx%s Matrix in %s".formatted(userItemMatrix.getNumRows(), userItemMatrix.getNumCols(), durationToString(start)));
        start = Instant.now();
        userItemMatrix.sortIndices(null);
        log.info("Sorted %sx%s Matrix row indices in %s".formatted(userItemMatrix.getNumRows(), userItemMatrix.getNumCols(), durationToString(start)));
        return userItemMatrix;
    }

    protected DMatrixRMaj getCharacteristicFactors(String nodeClass, Map<String, Integer> nodeIdMap) {
        ResourceIterator<Node> nodeIterator = findNodesByLabel(nodeClass);
        if (!nodeIterator.hasNext()) {
            throw new RuntimeException("Could not get Characteristic vectors: No nodes found with label %s".formatted(nodeClass));
        }
        List<CharacteristicVector> vectors = new ArrayList<>();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.next();
            CharacteristicVector characteristicVector = new CharacteristicVector();
            characteristicVector.setNodeId(node.getElementId());
            if (!node.hasProperty(PROPERTY_ALS)) throw new RuntimeException("Could not get Characteristic vectors: No vectors found for %s nodes".formatted(nodeClass));
            characteristicVector.setFactors((double[]) node.getProperty(PROPERTY_ALS));
            vectors.add(characteristicVector);
        }
        List<Integer> vectorLengths = vectors.stream()
                .mapToInt(CharacteristicVector::getLength)
                .distinct()
                .boxed()
                .toList();
        if (vectorLengths.size() > 1) {
            throw new RuntimeException("Could not get Characteristic vectors: vectors have differing lengths for class " + nodeClass);
        }
        DMatrixRMaj vectorMatrix = new DMatrixRMaj(vectors.size(), vectorLengths.stream().findFirst().orElse(0));
        for (CharacteristicVector vector : vectors) {
            for (int i = 0; i < vector.getLength(); i++) {
                vectorMatrix.set(nodeIdMap.get(vector.getNodeId()), i, vector.doubleValue(i));
            }
        }
        return vectorMatrix;
    }

    protected CharacteristicVector getCharacteristicVector(String nodeId) {
        Node node = findNodeByElementId(nodeId)
                .orElseThrow(() -> new RuntimeException("Node with elementId %s could not be found".formatted(nodeId)));
        CharacteristicVector vector = new CharacteristicVector();
        if (!node.hasProperty(PROPERTY_ALS)) {
            throw new RuntimeException("Node %s has no property '%s'".formatted(nodeId, PROPERTY_ALS));
        }
        vector.setNodeId(node.getElementId());
        vector.setFactors((double[]) node.getProperty(PROPERTY_ALS));
        return vector;
    }
}
