package recommendation.als.service;

import org.neo4j.logging.Log;
import recommendation.models.AlsFitResult;
import recommendation.models.IdentityMatrix;
import recommendation.models.Matrix;

import java.util.Objects;
import java.util.Random;

public class AlsService {

    private static final Random randomGenerator = new Random();

    private static Log serverLog;

    public static AlsFitResult fit(Matrix userItemTable, int iterations, float regulation, int factors) {
        if (Objects.isNull(userItemTable)) {
            throw new RuntimeException("Cannot run ALS: user-item table is not set");
        }
        int n = userItemTable.getNumberOfRows();
        int m = userItemTable.getNumberOfColumns();
        Matrix userFactors = generateRandomizedMatrix(n, factors);
        Matrix itemFactors = generateRandomizedMatrix(m, factors);
        Matrix I = new IdentityMatrix(factors).dot(regulation);
        for (int t = 0; t < iterations; t++) {
            if (Objects.nonNull(serverLog)) {
                serverLog.debug("Running Iteration " + (t+1) + "/" + iterations);
            }
            for (int i = 0; i < n; i++) {
                boolean[] filter = createFilterForColumns(i, userItemTable);
                Matrix ri = userItemTable.getRow(i).filterColumns(filter).transpose();
                Matrix vi = itemFactors.filterRows(filter);
                // V_i^T * V_i + I_k * r
                Matrix au = vi.transpose().dot(vi).add(I);
                // V_i^T * R_i
                Matrix bu = vi.transpose().dot(ri);
                // U_i = (V_i^T * V_i + I_k * r)^(-1) * V_i^T * R_i
                userFactors.setRow(i, au.inverse().dot(bu).transpose());
            }
            for (int j = 0; j < m; j++) {
                boolean[] filter = createFilterForRows(j, userItemTable);
                Matrix rj = userItemTable.getColumn(j).filterRows(filter);
                Matrix uj = itemFactors.filterRows(filter);
                // U_j^T * U_j + I_k * r
                Matrix av = uj.transpose().dot(uj).add(I);
                // U_j^T * R_i
                Matrix bv = uj.transpose().dot(rj);
                // V_j = (U_j^T * U_j + I_k * r)^(-1) * U_j^T * R_i
                itemFactors.setRow(j, av.inverse().dot(bv).transpose());
            }
        }
        return new AlsFitResult(userFactors, itemFactors);
    }

    public static AlsFitResult fit(Matrix userItemTable, int iterations, float regulation, int factors, long seed) {
        randomGenerator.setSeed(seed);
        return fit(userItemTable, iterations, regulation, factors);
    }

    private static Matrix generateRandomizedMatrix(int rows, int columns) {
        Matrix randomizedMatrix = new Matrix(rows, columns);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                randomizedMatrix.setCell(r, c, randomGenerator.nextFloat());
            }
        }
        return randomizedMatrix;
    }

    private static boolean[] createFilterForRows(int c, Matrix userItemTable) {
        double[] column = userItemTable.getColumn(c).flat();
        boolean[] filter = new boolean[column.length];
        for (int i = 0; i < column.length; i++) {
            filter[i] = column[i] != 0;
        }
        return filter;
    }

    private static boolean[] createFilterForColumns(int r, Matrix userItemTable) {
        double[] row = userItemTable.getRow(r).flat();
        boolean[] filter = new boolean[row.length];
        for (int i = 0; i < row.length; i++) {
            filter[i] = row[i] != 0;
        }
        return filter;
    }

    public static void setLog(Log log) {
        serverLog = log;
    }

}
