package io.adenium.papaya.compiler;

import io.adenium.utils.ByteArray;

import java.util.Map;

public interface ObfuscationStrategy {
    public ByteArray forName(String name, Map<String, ByteArray> names);
    public ByteArray forType(String type, Map<String, ByteArray> types);

    public class KeepNames implements ObfuscationStrategy {
        @Override
        public ByteArray forName(String name, Map<String, ByteArray> names) {
            if (names.containsKey(name)) {
                return names.get(name);
            }

            ByteArray array = ByteArray.wrap(name);
            names.put(name, array);

            return array;
        }

        @Override
        public ByteArray forType(String type, Map<String, ByteArray> types) {
            if (types.containsKey(type)) {
                return types.get(type);
            }

            ByteArray array = ByteArray.wrap(type);
            types.put(type, array);

            return array;
        }
    }
}
