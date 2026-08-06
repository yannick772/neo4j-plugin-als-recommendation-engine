package recommendation.models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Matrix {
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
        List<Double> doubleList = this.asList();
        if (doubleList.isEmpty()) {
            return false;
        }
        for (Double d : doubleList) {
            if (!d.equals(0D)) {
                return true;
            }
        }
        return false;
    }

    public double getCell(int row, int column) {
        return this.values[row][column];
    }

    public Matrix setCell(int row, int column, double value) {
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
        return Arrays.stream(this.values)
                .flatMapToDouble(Arrays::stream)
                .toArray();
    }

    public List<Double> asList() {
        List<Double> list = new ArrayList<>();
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
     * Calculates the inverse of the matrix according to "An Efficient and Simple Algorithm for Matrix Inversion"
     * @return inverse of the matrix
     */
    public Matrix inverse() {
        if (this.rows != this.columns) {
            throw new RuntimeException("Cannot calculate the inverse of a non square matrix");
        }
        Matrix inverseMatrix = this.clone();
        int n = this.rows;
        for (int p = 0; p < n; p++) {
            double pivot = inverseMatrix.getCell(p, p);
            if (pivot == 0) {
                return null;
            }
            for (int i = 0; i < n; i++) {
                inverseMatrix.setCell(i, p, -inverseMatrix.getCell(i, p) / pivot);
            }
            for (int i = 0; i < n; i++) {
                if (i != p) {
                    for (int j = 0; j < n; j++) {
                        if (j != p) {
                            inverseMatrix.setCell(i, j, inverseMatrix.getCell(i, j) + inverseMatrix.getCell(p, j) * inverseMatrix.getCell(i, p));
                        }
                    }
                }
            }
            for (int j = 0; j < n; j++) {
                inverseMatrix.setCell(p, j, inverseMatrix.getCell(p, j) / pivot);
            }
            inverseMatrix.setCell(p, p, 1 / pivot);
        }

        // round values due to floating point errors
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                inverseMatrix.setCell(i, j, BigDecimal.valueOf(inverseMatrix.getCell(i, j)).setScale(14, BigDecimal.ROUND_HALF_EVEN).doubleValue());
            }
        }
        return inverseMatrix;
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
        final Matrix clone = new Matrix(this.rows, this.columns);
        for (int r = 0; r < this.rows; r++) {
            for (int c = 0; c < this.columns; c++) {
                clone.setCell(r, c, this.getCell(r, c));
            }
        }
        return clone;
    }
}

