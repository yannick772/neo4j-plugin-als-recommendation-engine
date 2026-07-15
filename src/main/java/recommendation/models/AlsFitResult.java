package recommendation.models;

public class AlsFitResult {

    private Matrix userFactors;

    private Matrix itemFactors;

    public AlsFitResult(Matrix userFactors, Matrix itemFactors) {
        this.userFactors = userFactors;
        this.itemFactors = itemFactors;
    }

    public Matrix getUserFactors() {
        return userFactors;
    }

    public void setUserFactors(Matrix userFactors) {
        this.userFactors = userFactors;
    }

    public Matrix getItemFactors() {
        return itemFactors;
    }

    public void setItemFactors(Matrix itemFactors) {
        this.itemFactors = itemFactors;
    }
}
