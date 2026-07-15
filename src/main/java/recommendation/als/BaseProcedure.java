package recommendation.als;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import recommendation.models.neo4j.NodeId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BaseProcedure {

    @Context
    public Log log;

    @Context
    public Transaction transaction;

    public record DoubleOutput(Double value) {
        public Double getValue() {
            return value;
        }
    }

    public record BooleanOutput(Boolean value) {
        public Boolean getValue() {
            return value;
        }
    }

    public record StringOutput(String value) {
        public String getValue() {
            return value;
        }
    }

    public record LongOutput(Long value) {
        public Long getValue() {
            return value;
        }
    }

    public record NodeOutput(Node value) {
        public Node getValue() {
            return value;
        }
    }

    public record RelationshipOutput(Relationship value) {
        public Relationship getValue() {
            return value;
        }
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected <T> T querySingle(String query, Class<T> clazz) {
        try {
            String resultJson = objectMapper.writeValueAsString(
                    transaction.execute(query).stream()
                            .findFirst()
                            .orElse(null)
            );
            return objectMapper.readValue(resultJson, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T querySingle(String query, Map<String, Object> parameters, Class<T> clazz) {
        try {
            String resultJson = objectMapper.writeValueAsString(
                    transaction.execute(query, parameters).stream()
                            .findFirst()
                            .orElse(null)
            );
            return objectMapper.readValue(resultJson, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> List<T> queryList(String query, Class<T> clazz) {
        try {
            String resultJson = objectMapper.writeValueAsString(
                    transaction.execute(query).stream()
                            .collect(Collectors.toList())
            );
            return objectMapper.readValue(resultJson, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> List<T> queryList(String query, Map<String, Object> parameters, Class<T> clazz) {
        try {
            String resultJson = objectMapper.writeValueAsString(
                    transaction.execute(query, parameters).stream()
                            .collect(Collectors.toList())
            );
            return objectMapper.readValue(resultJson, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected void setNodeProperty(Long nodeId, String propertyName, Object property) {
        String query = "MATCH (n) " +
                "WHERE id(n)=$nodeId " +
                "SET n." + propertyName + " = $property";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("nodeId", nodeId);
        parameters.put("property", property);
        transaction.execute(query, parameters);
    }

    protected List<Long> queryForIds(String query) {
        return queryList(query, NodeId.class).stream()
                .map(NodeId::getId)
                .collect(Collectors.toList());
    }

    protected List<Long> queryForIds(String query, Map<String, Object> parameters) {
        return queryList(query, parameters, NodeId.class).stream()
                .map(NodeId::getId)
                .collect(Collectors.toList());
    }
}
