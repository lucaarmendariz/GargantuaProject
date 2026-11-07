package com.example.trazabilidadapi.config;

import com.example.trazabilidadapi.util.SqlScriptSplitter;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests sin base de datos del reinicio del perfil dev. */
class DevDatabaseResetTest {

    @Test
    void parteSentenciasNormalesYBloquesConDelimiter() {
        String sql = """
                -- comentario; con punto y coma
                DROP TABLE IF EXISTS a;
                CREATE TABLE a (
                    id INT -- columna
                );

                DELIMITER ;;
                CREATE OR REPLACE PROCEDURE p()
                BEGIN
                    SELECT 1;
                    SELECT 2;
                END;;
                DELIMITER ;

                CALL p();
                """;

        List<String> statements = SqlScriptSplitter.split(sql);

        assertThat(statements).hasSize(4);
        assertThat(statements.get(0)).isEqualTo("DROP TABLE IF EXISTS a");
        assertThat(statements.get(1)).startsWith("CREATE TABLE a (").endsWith(")");
        assertThat(statements.get(2)).startsWith("CREATE OR REPLACE PROCEDURE p()")
                .contains("SELECT 1;", "SELECT 2;").endsWith("END");
        assertThat(statements.get(3)).isEqualTo("CALL p()");
    }

    @Test
    void cadaProcedimientoDeLosScriptsEsUnaSentencia() throws Exception {
        for (String script : List.of("02_procedures.sql", "04_demo_data.sql")) {
            String sql = Files.readString(Path.of("database", script));
            List<String> statements = SqlScriptSplitter.split(sql);

            long procedures = statements.stream()
                    .filter(s -> s.startsWith("CREATE OR REPLACE PROCEDURE")).count();
            assertThat(procedures).as(script).isEqualTo(count(sql, "CREATE OR REPLACE PROCEDURE"));
            assertThat(statements).as(script).allSatisfy(s -> {
                assertThat(s).doesNotStartWith("DELIMITER");
                assertThat(s).doesNotEndWith(";;");
            });
        }
    }

    @Test
    void todosLosScriptsDelReinicioExisten() {
        for (String script : DevDatabaseReset.SCRIPTS) {
            assertThat(Path.of("database", script)).isRegularFile();
        }
    }

    @Test
    void soloReiniciaBasesDeDatosLocales() {
        DevDatabaseReset.checkIsLocal("jdbc:mariadb://localhost:3307/traceability_lean");
        DevDatabaseReset.checkIsLocal("jdbc:mariadb://127.0.0.1:3307/traceability_lean?useSSL=false");
        DevDatabaseReset.checkIsLocal("jdbc:mariadb://host.docker.internal:3307/traceability_lean");

        assertThatThrownBy(() -> DevDatabaseReset.checkIsLocal("jdbc:mariadb://db.empresa.com:3306/traceability_lean"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no es local");
        assertThatThrownBy(() -> DevDatabaseReset.checkIsLocal("no-es-una-url"))
                .isInstanceOf(IllegalStateException.class);
    }

    private static long count(String text, String needle) {
        Matcher matcher = Pattern.compile(Pattern.quote(needle)).matcher(text);
        long n = 0;
        while (matcher.find()) n++;
        return n;
    }
}
