package com.matrix.orquestrador.model;

public class ProcessOutput {

    private final Object value;
    private final int rowCount;
    private final String description;

    public ProcessOutput(Object value, int rowCount, String description) {
        this.value = value;
        this.rowCount = rowCount;
        this.description = description;
    }

    public static ProcessOutput of(Object value, int rowCount, String description) {
        return new ProcessOutput(value, rowCount, description);
    }

    public Object getValue() {
        return value;
    }

    public int getRowCount() {
        return rowCount;
    }

    public String getDescription() {
        return description;
    }

    @SuppressWarnings("unchecked")
    public <T> T getValueAs(Class<T> type) {
        if (!type.isInstance(value)) {
            throw new IllegalStateException("Process output is not a " + type.getSimpleName());
        }
        return (T) value;
    }
}
