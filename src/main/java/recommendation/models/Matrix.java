package recommendation.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Matrix implements Cloneable {
    protected int rows;
    protected int columns;
    protected double[][] values;

    public Matrix(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.values = new double[rows][columns];
    }

    public int getNumberOfRows() {
        return this.rows;
    }

    public int getNumberOfColumns() {
        return this.columns;
    }

    public boolean isEmpty() {
        return this.flat().length == 0;
    }

    public boolean isNonZero() {
        List<Float> floatList = this.asList();
        if (floatList.isEmpty()) {
            return false;
        }
        for (Float f : floatList) {
            if (!f.equals(0F)) {
                return true;
            }
        }
        return false;
    }

    public float getCell(int row, int column) {
        return (float) this.values[row][column];
    }

    public Matrix setCell(int row, int column, float value) {
        this.values[row][column] = value;
        return this;
    }

    public Matrix getRow(int row) {
        Matrix newMatrix = new Matrix(1, this.columns);
        for (int c = 0; c < this.columns; c++) {
            newMatrix.setCell(0, c, this.getCell(row, c));
        }
        return newMatrix;
    }

    public Matrix setRow(int row, Matrix rowVector) {
        if (row >= this.rows) {
            throw new IndexOutOfBoundsException("Row cannot be added: row index does not exist");
        }
        if (rowVector.rows != 1) {
            throw new RuntimeException("Row cannot be added: rowVector is not a row vector");
        }
        if (rowVector.columns != this.columns) {
            throw new RuntimeException("Row cannot be added: rowVector length does not match matrix");
        }
        for (int c = 0; c < this.columns; c++) {
            this.setCell(row, c, rowVector.getCell(0, c));
        }
        return this;
    }

    public Matrix getColumn(int column) {
        Matrix newMatrix = new Matrix(this.rows, 1);
        for (int r = 0; r < this.rows; r++) {
            newMatrix.setCell(r, 0, this.getCell(r, column));
        }
        return newMatrix;
    }

    public Matrix setColumn(int col, Matrix colVector) {
        if (col >= this.columns) {
            throw new IndexOutOfBoundsException("Column cannot be added: column index does not exist");
        }
        if (colVector.columns != 1) {
            throw new RuntimeException("Column cannot be added: colVector is not a row vector");
        }
        if (colVector.rows != this.rows) {
            throw new RuntimeException("Column cannot be added: colVector length does not match matrix");
        }
        for (int r = 0; r < this.rows; r++) {
            this.setCell(r, col, colVector.getCell(r, 0));
        }
        return this;
    }

    public double[] flat() {
//        float[] flatMatrix = new float[this.rows * this.columns];
        return Arrays.stream(this.values)
                .flatMapToDouble(Arrays::stream)
                .toArray();
//        for (int r = 0; r < this.rows; r++) {
//            for (int c = 0; c < this.columns; c++) {
//                flatMatrix[r * this.rows + c] = getCell(r, c);
//            }
//        }
//        return flatMatrix;
    }

    public List<Float> asList() {
        List<Float> list = new ArrayList<>();
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.columns; c++) {
                list.add(getCell(r, c));
            }
        }
        return list;
    }

    public Matrix removeRow(int row) {
        Matrix newMatrix = new Matrix(this.rows - 1, this.columns);
        List<Integer> rowIndices = IntStream.range(0, this.rows).boxed().collect(Collectors.toList());
        rowIndices.remove(row);
        int i = 0;
        for (int r : rowIndices) {
            for (int c = 0; c < this.columns; c++) {
                newMatrix.setCell(i, c, this.getCell(r, c));
            }
            i++;
        }
        return newMatrix;
    }

    public Matrix removeColumn(int column) {
        Matrix newMatrix = new Matrix(this.rows, this.columns - 1);
        List<Integer> columnIndices = IntStream.range(0, this.columns)
                .boxed()
                .collect(Collectors.toList());
        columnIndices.remove(column);
        for (int r = 0; r < this.rows; r++) {
            int i = 0;
            for (int c : columnIndices) {
                newMatrix.setCell(r, i++, this.getCell(r, c));
            }
        }
        return newMatrix;
    }

    public Matrix dot(float factor) {
        Matrix newMatrix = this.clone();
        for (int r = 0; r < newMatrix.rows; r++) {
            for (int c = 0; c < newMatrix.columns; c++) {
                newMatrix.setCell(r, c, newMatrix.getCell(r, c) * factor);
            }
        }
        return newMatrix;
    }

    public Matrix dot(Matrix matrix) {
        if (this.columns != matrix.rows) {
            throw new RuntimeException("Cannot multiply the matrix: because rows and columns do not align");
        }
        Matrix newMatrix = new Matrix(this.rows, matrix.columns);
        for (int r = 0; r < newMatrix.rows; r++) {
            for (int c = 0; c < newMatrix.columns; c++) {
                double[] row = this.getRow(r).flat();
                double[] col = matrix.getColumn(c).flat();
                float s = 0;
                for (int i = 0; i < row.length; i++) {
                    s += row[i] * col[i];
                }
                newMatrix.setCell(r, c, s);
            }
        }
        return newMatrix;
    }

    public Matrix add(Matrix matrix) {
        if (this.rows != matrix.rows || this.columns != matrix.columns) {
            throw new RuntimeException("Cannot add the matrix: as they do not have the same dimensions");
        }
        Matrix newMatrix = new Matrix(this.rows, this.columns);
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.columns; c++) {
                newMatrix.setCell(r, c, this.getCell(r, c) + matrix.getCell(r, c));
            }
        }
        return newMatrix;
    }

    public Matrix transpose() {
        Matrix transposedMatrix = new Matrix(this.columns, this.rows);
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.columns; j++) {
                transposedMatrix.setCell(j, i, this.getCell(i, j));
            }
        }
        return transposedMatrix;
    }

    /**
     * Returns the minor Matrix of the specified row and column
     * @param row
     * @param column
     * @return M_row,column
     */
    public Matrix minor(int row, int column) {
        if (this.rows == 1 || this.columns == 1) {
            throw new RuntimeException("Cannot calculate minor matrix: because the matrixis too small");
        }
        return this.clone().removeRow(row).removeColumn(column);
    }

//    public float determinant() {
//        if (this.rows != this.columns) {
//            throw new RuntimeException("Cannot calculate the determinant of a non square matrix");
//        }
//        int n = this.rows;
//        if (n == 1) {
//            return this.getCell(0, 0);
//        }
//        float det = 0;
//        for (int c = 0; c < this.columns; c++) {
//            int sign = c % 2 == 0 ? 1 : -1;
//            det += sign * this.getCell(0, c) * this.minor(0,c).determinant();
//        }
//        return det;
//    }

//    public Matrix cofactor() {
//        if (this.rows != this.columns) {
//            throw new RuntimeException("Cannot calculate the cofactor of a non square matrix");
//        }
//        int n = this.rows;
//        Matrix cofactorMatrix = new Matrix(n, n);
//        for (int r = 0; r < this.rows; r++) {
//            for (int c = 0; c < this.columns; c++) {
//                int sign = (r % 2 == c % 2) ? 1 : -1;
//                cofactorMatrix.setCell(r, c, sign * this.minor(r, c).determinant());
//            }
//        }
//        return cofactorMatrix;
//    }

//    public Matrix adjoint() {
//        if (this.rows != this.columns) {
//            throw new RuntimeException("Cannot calculate the adjoint of a non square matrix");
//        }
//        return this.cofactor().transpose();
//    }

//    public Matrix inverse() {
//        float det = this.determinant();
//        if (det == 0) {
//            return null;
//        }
//        Matrix inversedMatrix = this.adjoint();
//        return inversedMatrix.dot(1 / det);
//    }


    /**
     * Calculates the inverse of the matrix using gaussian elimination with partial pivoting
     * STILL WIP
     * @return the inversed Matrix
     */
    public Matrix inverse() {
        if (this.rows != this.columns) {
            throw new RuntimeException("Cannot calculate the inverse of a non square matrix");
        }
        int n = this.rows;
        Matrix augmented = new Matrix(n, n * 2);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                augmented.setCell(i, j, this.getCell(i, j));
                if (i == j) {
                    augmented.setCell(i, j + n, 1);
                }
            }
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= i; j++) {
                float v = augmented.getCell(i,j);
                if (j == i) {
                    if (v == 1) {
                        continue;
                    }
                    augmented.setRow(i, augmented.getRow(i).dot(1/v));
                } else {
                    if (v == 0) {
                        continue;
                    }
                    Matrix row = augmented.getRow(i);
                    for (int k = 0; k < n; k++) {
                        row.setCell(0, k + n, row.getCell(0, k));
                    }
                    augmented.setRow(i, augmented.getRow(j).add(row.dot(-v)));
                }
            }
        }

        for (int i = n-1; i >= 0; --i) {
            for (int j = n; j > i; --j) {
                float v = augmented.getCell(i,j);
                if (v == 0) {
                    continue;
                }
                Matrix row = augmented.getRow(i);
                for (int k = 0; k < n; k++) {
                    row.setCell(0, k + n, row.getCell(0, k));
                }
                augmented.setRow(i, augmented.getRow(i).add(row.dot(-v)));
            }
        }

        Matrix inversed = new Matrix(n,n);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                inversed.setCell(i, j, augmented.getCell(i, j + n));
            }
        }

        return inversed;
    }

    public Matrix filterRows(boolean[] filter) {
        if (filter.length != this.rows) {
            throw new RuntimeException("Cannot filter rows: filter length doe not match");
        }
        Matrix filteredMatrix = this.clone();
        int change = 0;
        for (int i = 0; i < this.rows; i++) {
            if (!filter[i]) {
                filteredMatrix = filteredMatrix.removeRow(i - change++);
            }
        }
        return filteredMatrix;
    }

    public Matrix filterColumns(boolean[] filter) {
        if (filter.length != this.columns) {
            throw new RuntimeException("Cannot filter columns: filter length doe not match");
        }
        Matrix filteredMatrix = this.clone();
        int change = 0;
        for (int i = 0; i < this.columns; i++) {
            if (!filter[i]) {
                filteredMatrix = filteredMatrix.removeColumn(i - change++);
            }
        }
        return filteredMatrix;
    }

    @Override
    public Matrix clone() {
        final Matrix clone;
        try {
            clone = (Matrix) super.clone();
        } catch (Exception e) {
            throw new RuntimeException("Superclass messed up while cloning", e);
        }
        clone.rows = this.rows;
        clone.columns = this.columns;
        clone.values = this.values.clone();
        return clone;
    }
}

