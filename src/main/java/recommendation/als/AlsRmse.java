package recommendation.als;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.dense.row.CommonOps_DDRM;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

public class AlsRmse extends AlsProcedure {

    @Procedure(name = "recommendation.als.rmse", mode = Mode.READ)
    @Description(value = """
            This procedure is used to calculate the rmse of the als algorithm.
            Inputs:
            user: the class name of the user nodes
            item: the class name of the item nodes
            relationship: the class name of the relationship between the user and item nodes
            value: the name of the value in the relationship that is supposed to be predicted via als
            """)
    public Stream<DoubleOutput> rmse(
            @Name(value = "relationship", defaultValue = "RATED") String relationship,
            @Name(value = "user", defaultValue = "User") String user,
            @Name(value = "item", defaultValue = "Movie") String item,
            @Name(value = "value", defaultValue = "rating") String value) {
        Map<String, Integer> userIdMap = getNodeIdMap(user);
        Map<String, Integer> itemIdMap = getNodeIdMap(item);
        DMatrixSparseCSC userItemMatrix = getUserItemMatrix(relationship, item, userIdMap, itemIdMap, value);

        DMatrixRMaj userFactors = getCharacteristicFactors(user, userIdMap);
        DMatrixRMaj itemFactors = getCharacteristicFactors(item, itemIdMap);
        if (userFactors.numCols == 0 || itemFactors.numCols == 0) {
            throw new RuntimeException("Cannot calculate RMSE: No characteristic factors present");
        }

        if (userFactors.numRows != userItemMatrix.numRows
                || itemFactors.numRows != userItemMatrix.numCols) {
            throw new RuntimeException("Cannot calculate RMSE: Matrix dimensions do not match");
        }

        double rmse = 0;
        int total = 0;

        DMatrixRMaj userVector = new DMatrixRMaj(1, userFactors.numCols);
        DMatrixRMaj itemVector = new DMatrixRMaj(1, itemFactors.numCols);
        DMatrixRMaj recommendation = new DMatrixRMaj(1, 1);
        for (int col = 0; col < userItemMatrix.numCols; col++) {
            int startIndex = userItemMatrix.col_idx[col];
            int endIndex = userItemMatrix.col_idx[col + 1];
            for (int i = startIndex; i < endIndex; i++) {
                int row = userItemMatrix.nz_rows[i];
                double trueValue = userItemMatrix.nz_values[i];
                CommonOps_DDRM.extractRow(userFactors, row, userVector);
                CommonOps_DDRM.extractRow(itemFactors, col, itemVector);
                CommonOps_DDRM.multTransB(userVector, itemVector, recommendation);
                double predValue = recommendation.get(0);
                rmse += Math.pow(trueValue - predValue, 2);
                total++;
            }
        }

        if (total == 0) {
            log.warn("Cannot calculate RMSE: No real values were found");
            return Stream.of(new DoubleOutput(0.0));
        }
        rmse = Math.sqrt(rmse / total);
        return Stream.of(new DoubleOutput(rmse));
    }

}
