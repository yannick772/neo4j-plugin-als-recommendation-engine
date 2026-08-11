package recommendation.models;

import org.ejml.data.DMatrixRMaj;

public class AlsFitResult {

    private DMatrixRMaj userFactors;

    private DMatrixRMaj itemFactors;

    public AlsFitResult(DMatrixRMaj userFactors, DMatrixRMaj itemFactors) {
        this.userFactors = userFactors;
        this.itemFactors = itemFactors;
    }

    public DMatrixRMaj getUserFactors() {
        return userFactors;
    }

    public void setUserFactors(DMatrixRMaj userFactors) {
        this.userFactors = userFactors;
    }

    public DMatrixRMaj getItemFactors() {
        return itemFactors;
    }

    public void setItemFactors(DMatrixRMaj itemFactors) {
        this.itemFactors = itemFactors;
    }
}
