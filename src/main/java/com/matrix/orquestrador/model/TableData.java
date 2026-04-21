package com.matrix.orquestrador.model;

import java.util.List;

public class TableData {

    private final String tableName;
    private final List<String> columns;
    private final double[][] rows;

    public TableData(String tableName, List<String> columns, double[][] rows) {
        this.tableName = tableName;
        this.columns = columns;
        this.rows = rows;
    }

    public String getTableName() {
        return tableName;
    }

    public List<String> getColumns() {
        return columns;
    }

    public double[][] getRows() {
        return rows;
    }

    public int getRowCount() {
        return rows.length;
    }
}
