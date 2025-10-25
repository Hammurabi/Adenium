package io.adenium.papaya.archive;

public class ArchivedType {
    private final String    simpleName;
    private final String    canonicalName[];
    private final int       sequentialName;

    public ArchivedType(String simpleName, String canonicalName[], int sequentialName) {
        this.simpleName = simpleName;
        this.canonicalName = canonicalName;
        this.sequentialName = sequentialName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String[] getCanonicalName() {
        return canonicalName;
    }

    public int getSequentialName() {
        return sequentialName;
    }
}
