package io.adenium.papaya.compiler;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class CompiledScript {
    private final ByteArrayOutputStream         compilationResult;
    private final PapayaApplication             application;
    private final Map<String, StructureInfo>    structureInfoMap;
    private final Map<String, StructureInfo>    functionMap;

    public CompiledScript(PapayaApplication application) {
        this.application = application;
        this.compilationResult = new ByteArrayOutputStream();
        this.structureInfoMap = new HashMap<>();
        functionMap = new HashMap<>();
    }

    public PapayaApplication getApplication() {
        return application;
    }

    private final static class StructureInfo {
        private String name;
        private StructureType structureType;
        private int length;
        private LineInfo lineInfo;
        private int offset;

        private StructureInfo(String name, StructureType structureType, int length, LineInfo lineInfo, int offset) {
            this.name = name;
            this.structureType = structureType;
            this.length = length;
            this.lineInfo = lineInfo;
            this.offset = offset;
        }
    }
}
