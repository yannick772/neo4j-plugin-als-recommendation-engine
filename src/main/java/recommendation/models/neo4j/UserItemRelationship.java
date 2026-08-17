package recommendation.models.neo4j;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserItemRelationship {

    private String userId;

    private double rating;

    private String itemId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
}
