package recommendation.als.service;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.ejml.sparse.csc.CommonOps_DSCC;
import org.neo4j.logging.Log;
import recommendation.models.AlsFitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.IntStream;

public class AlsService {

    private static final Random randomGenerator = new Random();

    private static Log serverLog;

    public static AlsFitResult fit(DMatrixSparseCSC userItemTable, int iterations, float regulation, int factors) {
        if (Objects.isNull(userItemTable)) {
            throw new RuntimeException("Cannot run ALS: user-item table is not set");
        }
        int n = userItemTable.getNumRows();
        int m = userItemTable.getNumCols();
        DMatrixRMaj userFactors = new DMatrixRMaj(n, factors);
        RandomMatrices_DDRM.fillUniform(userFactors, randomGenerator);
        DMatrixRMaj itemFactors = new DMatrixRMaj(n, factors);
        RandomMatrices_DDRM.fillUniform(itemFactors, randomGenerator);
        // Declaration of matrixs for calculation to allocate RAM
        DMatrixRMaj uv = new DMatrixRMaj(Math.max(n, m), factors);
        DMatrixRMaj calc = new DMatrixRMaj(factors, factors);
        DMatrixRMaj calc2 = new DMatrixRMaj(factors, factors);
        DMatrixRMaj big = new DMatrixRMaj(factors, Math.max(n, m));
        DMatrixRMaj vector = new DMatrixRMaj(1, factors);
        DMatrixSparseCSC tmp = new DMatrixSparseCSC(1, Math.max(n, m));
        DMatrixSparseCSC r = new DMatrixSparseCSC(1, Math.max(n, m));

        DMatrixRMaj I = CommonOps_DDRM.identity(factors);
        CommonOps_DDRM.scale(regulation, I);

        DMatrixSparseCSC userItemTableTransposed = CommonOps_DSCC.transpose(userItemTable, null, null);
        for (int t = 0; t < iterations; t++) {
            if (Objects.nonNull(serverLog)) {
                serverLog.info("Running Iteration %s/%s".formatted(t+1, iterations));
            }
            for (int i = 0; i < n; i++) {
                int[] filter = createFilterForColumns(i, userItemTable);
                if (filter.length == 0) continue;
                // R_i | dims: 1xm-?
                filterColumns(getRow(userItemTable, i, tmp), filter, r);
                // V_i | dims: m-?xk
                filterRows(itemFactors, filter, uv);
                // U_i = (V_i^T * V_i + I_k * r)^(-1) * V_i^T * R_i
                CommonOps_DDRM.multTransA(uv, uv, calc); // V_i^T * V_i | dims: kxm-? * m-?xk = kxk
                CommonOps_DDRM.add(calc, I, calc2); // V_i^T * V_i + I_k * r | dims: kxk + kxk = kxk
                CommonOps_DDRM.invert(calc2, calc); // (V_i^T * V_i + I_k * r)^(-1) | dims: kxk^(-1) = kxk
                CommonOps_DDRM.multTransB(calc, uv, big); // (V_i^T * V_i + I_k * r)^(-1) * V_i^T | dims: kxk * kxm-? = kxm-?
                CommonOps_DSCC.multTransB(r, big, vector, null); // R_i * ((V_i^T * V_i + I_k * r)^(-1) * V_i^T)^T | dims: 1xm-? * m-?xk = 1xk
                setRow(userFactors, vector, i);
            }
            for (int j = 0; j < m; j++) {
                int[] filter = createFilterForRows(j, userItemTableTransposed);
                if (filter.length == 0) continue;
                // R_j | dims: n-?x1
                filterRows(getColumn(userItemTable, j, tmp), filter, r);
                // U_j | dims: n-?xk
                filterRows(userFactors, filter, uv);
                CommonOps_DDRM.multTransA(uv, uv, calc); // U_j^T * U_j | dims: kxn-? * n-?xk = kxk
                CommonOps_DDRM.add(calc, I, calc2); // U_j^T * U_j + I_k * r | dims kxk + kxk = kxk
                CommonOps_DDRM.invert(calc2, calc); // (U_j^T * U_j + I_k * r)^(-1) | dims: kxk^(-1) = kxk
                CommonOps_DDRM.multTransB(calc, uv, big); // (U_j^T * U_j + I_k * r)^(-1) * U_j^T | dims: kxk * kxn-? = kxn-?
                CommonOps_DSCC.multTransAB(r, big, vector); // R_i^T * ((U_j^T * U_j + I_k * r)^(-1) * U_j^T)^T | dims: 1xn-? * n-?xk = 1xk
                setRow(itemFactors, vector, j);
            }
        }
        return new AlsFitResult(userFactors, itemFactors);
    }

    public static AlsFitResult fit(DMatrixSparseCSC userItemTable, int iterations, float regulation, int factors, long seed) {
        randomGenerator.setSeed(seed);
        return fit(userItemTable, iterations, regulation, factors);
    }

    private static int[] createFilterForRows(int c, DMatrixSparseCSC matrix) {
        int startIndex = matrix.col_idx[c];
        int endIndex = matrix.col_idx[c + 1];
        int length = endIndex - startIndex;

        if (length == 0) return new int[0];

        int[] nonZeroRows = new int[length];
        System.arraycopy(matrix.nz_rows, startIndex, nonZeroRows, 0, length);
        return nonZeroRows;
    }

    private static DMatrixSparseCSC filterRows(DMatrixSparseCSC matrix, int[] filter) {
        DMatrixSparseCSC filteredMatrix = new DMatrixSparseCSC(filter.length, matrix.numCols);
        int targetRowIndex = 0;
        for (int rowIndex : filter) {
            CommonOps_DSCC.extract(matrix, rowIndex, rowIndex + 1, 0, matrix.numCols, filteredMatrix, targetRowIndex++, 0);
        }
        return filteredMatrix;
    }

    private static void filterRows(DMatrixSparseCSC matrix, int[] filter, DMatrixSparseCSC out) {
        out.reshape(filter.length, matrix.numCols);
        int targetRowIndex = 0;
        for (int rowIndex : filter) {
            CommonOps_DSCC.extract(matrix, rowIndex, rowIndex + 1, 0, matrix.numCols, out, targetRowIndex++, 0);
        }
    }

    private static DMatrixRMaj filterRows(DMatrixRMaj matrix, int[] filter) {
        return CommonOps_DDRM.extract(matrix, filter, filter.length, IntStream.range(0, matrix.numCols).toArray(), matrix.numCols, null);
    }

    private static DMatrixRMaj filterRows(DMatrixRMaj matrix, int[] filter, DMatrixRMaj out) {
        return CommonOps_DDRM.extract(matrix, filter, filter.length, IntStream.range(0, matrix.numCols).toArray(), matrix.numCols, out);
    }

    private static int[] createFilterForColumns(int r, DMatrixSparseCSC matrix) {
        List<Integer> nonZeroColumns = new ArrayList<>();
        for (int c = 0; c < matrix.numCols; c++) {
            if (matrix.nz_index(r, c) >= 0) {
                nonZeroColumns.add(c);
            }
        }
        return nonZeroColumns.stream().mapToInt(x -> x).toArray();
    }

    private static DMatrixSparseCSC filterColumns(DMatrixSparseCSC matrix, int[] filter) {
        DMatrixSparseCSC filteredMatrix = new DMatrixSparseCSC(matrix.numRows, filter.length);
        int targetColumnIndex = 0;
        for (int columnIndex : filter) {
            CommonOps_DSCC.extract(matrix, 0, matrix.numRows, columnIndex, columnIndex + 1, filteredMatrix, 0, targetColumnIndex++);
        }
        return filteredMatrix;
    }

    private static DMatrixSparseCSC filterColumns(DMatrixSparseCSC matrix, int[] filter, DMatrixSparseCSC out) {
        out.reshape(matrix.numRows, filter.length);
        int targetColumnIndex = 0;
        for (int columnIndex : filter) {
            CommonOps_DSCC.extract(matrix, 0, matrix.numRows, columnIndex, columnIndex + 1, out, 0, targetColumnIndex++);
        }
        return out;
    }
    
    private static DMatrixSparseCSC getRow(DMatrixSparseCSC matrix, int row) {
        return CommonOps_DSCC.extractRows(matrix, row, row + 1, null);
    }

    private static DMatrixSparseCSC getRow(DMatrixSparseCSC matrix, int row, DMatrixSparseCSC out) {
        return CommonOps_DSCC.extractRows(matrix, row, row + 1, out);
    }

    private static DMatrixSparseCSC getColumn(DMatrixSparseCSC matrix, int col) {
        return CommonOps_DSCC.extractColumn(matrix, col, null);
    }

    private static DMatrixSparseCSC getColumn(DMatrixSparseCSC matrix, int col, DMatrixSparseCSC out) {
        return CommonOps_DSCC.extractColumn(matrix, col, out);
    }

    private static DMatrixRMaj transpose(DMatrixRMaj matrix) {
        return CommonOps_DDRM.transpose(matrix, null);
    }

    private static DMatrixSparseCSC transpose(DMatrixSparseCSC matrix) {
        return CommonOps_DSCC.transpose(matrix, null, null);
    }

    private static DMatrixSparseCSC transpose(DMatrixSparseCSC matrix, DMatrixSparseCSC out) {
        return CommonOps_DSCC.transpose(matrix, out, null);
    }

    private static DMatrixRMaj mult(DMatrixRMaj a, DMatrixSparseCSC b) {
        return transpose(CommonOps_DSCC.multTransAB(b, a, null));
    }

    private static DMatrixRMaj mult(DMatrixRMaj a, DMatrixRMaj b) {
        return CommonOps_DDRM.mult(a, b, null);
    }

    private static DMatrixRMaj add(DMatrixRMaj a, DMatrixRMaj b) {
        return CommonOps_DDRM.add(a, b, null);
    }

    private static DMatrixRMaj invert(DMatrixRMaj matrix) {
        DMatrixRMaj inverseMatrix = new DMatrixRMaj(matrix.numRows, matrix.numCols);
        CommonOps_DDRM.invert(matrix, inverseMatrix);
        return inverseMatrix;
    }

    private static void setRow(DMatrixRMaj dest, DMatrixRMaj row, int index) {
        CommonOps_DDRM.insert(row, dest, index, 0);
    }

    public static void setLog(Log log) {
        serverLog = log;
    }

    public static void removeLog() {
        serverLog = null;
    }

}
