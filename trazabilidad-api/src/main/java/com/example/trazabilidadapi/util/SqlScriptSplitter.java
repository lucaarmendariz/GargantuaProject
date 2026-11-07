package com.example.trazabilidadapi.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Parte un script SQL en sentencias para ejecutarlas por JDBC, igual que lo haría el cliente
 * de línea de comandos de MariaDB. Entiende {@code DELIMITER ;;}, que usan los scripts con
 * procedimientos almacenados. Las líneas que solo tienen un comentario {@code --} se ignoran.
 */
public final class SqlScriptSplitter {

    private SqlScriptSplitter() {
    }

    public static List<String> split(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String delimiter = ";";

        for (String line : script.split("\\R", -1)) {
            String trimmed = line.trim();

            if (trimmed.regionMatches(true, 0, "DELIMITER ", 0, 10)) {
                addIfNotBlank(statements, current.toString());
                current.setLength(0);
                delimiter = trimmed.substring(10).trim();
                continue;
            }
            if (trimmed.startsWith("--") || (current.isEmpty() && trimmed.isEmpty())) {
                continue;
            }

            current.append(line).append('\n');
            if (trimmed.endsWith(delimiter)) {
                String text = current.toString().stripTrailing();
                addIfNotBlank(statements, text.substring(0, text.length() - delimiter.length()));
                current.setLength(0);
            }
        }
        addIfNotBlank(statements, current.toString());
        return statements;
    }

    private static void addIfNotBlank(List<String> statements, String statement) {
        String trimmed = statement.trim();
        if (!trimmed.isEmpty()) {
            statements.add(trimmed);
        }
    }
}
