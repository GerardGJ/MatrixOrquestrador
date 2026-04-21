package com.matrix.orquestrador.model;

import java.util.Arrays;

public class Matrix {

    private final double[][] values;

    public Matrix(double[][] values) {
        this.values = copy(values);
    }

    public int getRowCount() {
        return values.length;
    }

    public int getColumnCount() {
        return values.length == 0 ? 0 : values[0].length;
    }

    public double[][] getValues() {
        return copy(values);
    }

    public Matrix multiply(Matrix other) {
        if (getColumnCount() != other.getRowCount()) {
            throw new IllegalArgumentException(
                    "Matrix multiplication requires left columns to match right rows."
            );
        }

        double[][] result = new double[getRowCount()][other.getColumnCount()];
        for (int row = 0; row < getRowCount(); row++) {
            for (int column = 0; column < other.getColumnCount(); column++) {
                double sum = 0;
                for (int inner = 0; inner < getColumnCount(); inner++) {
                    sum += values[row][inner] * other.values[inner][column];
                }
                result[row][column] = sum;
            }
        }
        return new Matrix(result);
    }

    private double[][] copy(double[][] source) {
        double[][] target = new double[source.length][];
        for (int index = 0; index < source.length; index++) {
            target[index] = Arrays.copyOf(source[index], source[index].length);
        }
        return target;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (double[] row : values) {
            builder.append(Arrays.toString(row)).append(System.lineSeparator());
        }
        return builder.toString();
    }
}
