package recommendation.als;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import recommendation.models.IdentityMatrix;
import recommendation.models.Matrix;

@Deprecated
public class MatrixTest {

//    @Test
    public void testMatrixInverse() {
        // arrange
        Matrix matrix = new Matrix(3,3);
        matrix.setCell(0,0,-1);
        matrix.setCell(0,1,2);
        matrix.setCell(0,2,3);
        matrix.setCell(1,0,4);
        matrix.setCell(1,1,-2);
        matrix.setCell(1,2,8);
        matrix.setCell(2,0,2);
        matrix.setCell(2,1,1);
        matrix.setCell(2,2,8);

        Matrix expected = new Matrix(3,3);
        expected.setCell(0,0,-3/2f);
        expected.setCell(0,1,-13/16f);
        expected.setCell(0,2,11/8f);
        expected.setCell(1,0,-1);
        expected.setCell(1,1,-7/8f);
        expected.setCell(1,2,5/4f);
        expected.setCell(2,0,1/2f);
        expected.setCell(2,1,5/16f);
        expected.setCell(2,2,-3/8f);

        IdentityMatrix identityMatrix = new IdentityMatrix(3);

        // act
        Matrix actual = matrix.inverse();

        // assert
        Assertions.assertThat(actual.flat()).isEqualTo(expected.flat());
        Assertions.assertThat(matrix.dot(actual).flat()).isEqualTo(identityMatrix.flat());
    }

}
