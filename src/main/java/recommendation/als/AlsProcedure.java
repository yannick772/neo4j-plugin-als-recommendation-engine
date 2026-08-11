package recommendation.als;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.neo4j.graphdb.*;
import recommendation.BaseProcedure;
import recommendation.models.neo4j.CharacteristicVector;
import recommendation.models.neo4j.UserItemRelationship;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlsProcedure extends BaseProcedure {

    protected static String PROPERTY_ALS = "als";

    protected Map<String, Integer> getNodeIdMap(String nodeClass) {
        Instant start = Instant.now();
        Map<String, Integer> nodeIdMap = new HashMap<>();
        ResourceIterator<Node> nodeIterator = transaction.findNodes(Label.label(nodeClass));
        int index = 0;
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.next();
            nodeIdMap.put(node.getElementId(), index++);
        }
        log.info("Pulled %s ids for %s in %s".formatted(nodeIdMap.size(), nodeClass, durationToString(start)));
        return nodeIdMap;
    }

    protected DMatrixSparseCSC getUserItemMatrix(String relationClass, Map<String, Integer> userIdMap, Map<String, Integer> itemIdMap, String propertyName) {
        List<UserItemRelationship> relations = getRelations(relationClass, propertyName);
        Instant start = Instant.now();
        DMatrixSparseCSC userItemMatrix = new DMatrixSparseCSC(userIdMap.size(), itemIdMap.size());
        relations.forEach(r -> userItemMatrix.set(
                userIdMap.get(r.getUserId()),
                itemIdMap.get(r.getItemId()),
                r.getRating()
        ));
        log.info("Created %sx%s Matrix in %s".formatted(userItemMatrix.getNumRows(), userItemMatrix.getNumCols(), durationToString(start)));
        return userItemMatrix;
    }

    protected List<UserItemRelationship> getRelations(String relationClass, String propertyName) {
        Instant start = Instant.now();
        ResourceIterator<Relationship> relationshipIterator = transaction.findRelationships(RelationshipType.withName(relationClass));
        List<UserItemRelationship> relationships = new ArrayList<>();
        while (relationshipIterator.hasNext()) {
            Relationship relationship = relationshipIterator.next();
            UserItemRelationship userItemRelationship = new UserItemRelationship();
            userItemRelationship.setUserId(relationship.getStartNode().getElementId());
            userItemRelationship.setRating(Double.parseDouble(String.valueOf(relationship.getProperty(propertyName))));
            userItemRelationship.setItemId(relationship.getEndNode().getElementId());
            relationships.add(userItemRelationship);
        }
        log.info("Pulled %s relationships of type %s in %s".formatted(relationships.size(), relationClass, durationToString(start)));
        return relationships;
    }

    protected DMatrixRMaj getCharacteristicFactors(String nodeClass, Map<String, Integer> nodeIdMap) {
        ResourceIterator<Node> nodeIterator = transaction.findNodes(Label.label(nodeClass));
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
        Node node = transaction.getNodeByElementId(nodeId);
        CharacteristicVector vector = new CharacteristicVector();
        vector.setNodeId(node.getElementId());
        vector.setFactors((double[]) node.getProperty(PROPERTY_ALS));
        return vector;
    }
}
