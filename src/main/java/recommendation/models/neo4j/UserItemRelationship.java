package recommendation.models.neo4j;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserItemRelationship {

    private Long userId;

    private Float rating;

    private Long itemId;

    public Long getUserId() {
        return userId;
    }

    public Float getRating() {
        return rating;
    }

    public Long getItemId() {
        return itemId;
    }
}
