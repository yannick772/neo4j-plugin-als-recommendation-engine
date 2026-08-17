package recommendation.models;

@Deprecated
public class IdentityMatrix extends Matrix {

    public IdentityMatrix(int n) {
        super(n, n);
        for (int i = 0; i < n; i++) {
            this.values[i][i] = 1;
        }
    }

}
