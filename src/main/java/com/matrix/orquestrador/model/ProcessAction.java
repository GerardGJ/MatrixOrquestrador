package com.matrix.orquestrador.model;

@FunctionalInterface
public interface ProcessAction {

    ProcessOutput execute(ProcessContext context);
}
