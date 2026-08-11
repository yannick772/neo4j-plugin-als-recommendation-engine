package recommendation.models.neo4j;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CharacteristicVector {

    private String nodeId;

    private double[] factors;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public double[] getFactors() {
        return factors;
    }

    public void setFactors(double[] factors) {
        this.factors = factors;
    }

    public int getLength() {
        if (Objects.isNull(factors)) {
            return 0;
        }
        return factors.length;
    }

    public double doubleValue(int i) {
        return factors[i];
    }

}
