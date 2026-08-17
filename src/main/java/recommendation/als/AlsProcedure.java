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

    protected DMatrixSparseCSC getUserItemMatrix(String relationClass, Map<String, Integer> userIdMap, Map<String, Integer> itemIdMap, String propertyName) {
        Instant start = Instant.now();
        DMatrixSparseCSC userItemMatrix = new DMatrixSparseCSC(userIdMap.size(), itemIdMap.size());
        ResourceIterator<Relationship> relationships = findRelationshipsByName(relationClass);
        while (relationships.hasNext()) {
            Relationship relationship = relationships.next();
            Integer userIndex = userIdMap.get(relationship.getStartNode().getElementId());
            Integer itemIndex = itemIdMap.get(relationship.getEndNode().getElementId());
            double rating = Double.parseDouble(String.valueOf(relationship.getProperty(propertyName)));
            userItemMatrix.set(userIndex, itemIndex, rating);
        }
        log.info("Created %sx%s Matrix in %s".formatted(userItemMatrix.getNumRows(), userItemMatrix.getNumCols(), durationToString(start)));
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
