import com.alejandria.biblioteca.seguridad.ValidadorContrasena;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Herramienta del equipo (no forma parte del sistema): cambia la contraseña de los
 * usuarios de prueba admin@, encargado@ y alumno@alejandria.edu.pe.
 *
 * La contraseña se escribe aquí y NO se guarda en ningún archivo: en Neon y en el
 * script solo queda su hash BCrypt. Usa la misma regla y el mismo BCrypt del sistema.
 *
 * Uso (desde la carpeta backend):   herramientas\cambiar-contrasena.cmd
 * Para probar sin cambiar nada:     herramientas\cambiar-contrasena.cmd --probar
 */
public class CambiarContrasenaPrueba {

    private static final List<String> USUARIOS_PRUEBA = List.of(
            "admin@alejandria.edu.pe", "encargado@alejandria.edu.pe", "alumno@alejandria.edu.pe");

    private static final Path SCRIPT = Path.of("..", "database", "schema_postgresql.sql");
    private static final Pattern HASH_BCRYPT = Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");

    /** Se usa solo si no hay consola (por ejemplo, si la entrada viene de otro programa). */
    private static final BufferedReader ENTRADA = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    // comentario del script que se reemplaza cuando la contraseña ya cumple la regla
    private static final String COMENTARIO_VIEJO =
            "-- Ojo: este hash es de una contraseña antigua que NO cumple la regla de seguridad.\n"
          + "-- Pendiente: reemplazarlo (aquí y en Neon) por el hash de una contraseña que sí la cumpla.\n";
    private static final String COMENTARIO_NUEVO =
            "-- El hash es de una contraseña de prueba que cumple la regla de seguridad.\n"
          + "-- Para cambiarla: backend\\herramientas\\cambiar-contrasena.cmd\n";

    public static void main(String[] args) throws Exception {
        boolean soloProbar = Arrays.asList(args).contains("--probar");

        System.out.println("Cambiar la contraseña de los usuarios de prueba:");
        USUARIOS_PRUEBA.forEach(c -> System.out.println("  - " + c));
        System.out.println("Regla: " + ValidadorContrasena.MENSAJE_REGLA);
        if (soloProbar) {
            System.out.println("(modo --probar: no se cambia nada)");
        }
        System.out.println();

        char[] primera = leerOculto("Nueva contraseña: ");
        char[] segunda = leerOculto("Repítela: ");
        String clave = new String(primera);
        try {
            if (!clave.equals(new String(segunda))) {
                salir("Las contraseñas no coinciden. No se cambió nada.");
            }
            if (!ValidadorContrasena.esValida(clave)) {
                salir("La contraseña no cumple la regla. No se cambió nada.");
            }

            BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
            String hash = bcrypt.encode(clave);

            actualizarNeon(hash, clave, bcrypt, soloProbar);
            actualizarScript(hash, soloProbar);

            System.out.println();
            System.out.println(soloProbar
                    ? "Prueba terminada: todo está listo para el cambio real (sin --probar)."
                    : "Listo. Inicien sesión con la nueva contraseña. No la escriban en ningún archivo del repositorio.");
        } finally {
            Arrays.fill(primera, ' ');
            Arrays.fill(segunda, ' ');
        }
    }

    /** Cambia el hash en la tabla usuario de Neon y comprueba que la nueva contraseña funciona. */
    private static void actualizarNeon(String hash, String clave, BCryptPasswordEncoder bcrypt, boolean soloProbar) throws Exception {
        Properties env = leerEnv();
        try (Connection c = DriverManager.getConnection(
                valor(env, "SPRING_DATASOURCE_URL"), valor(env, "SPRING_DATASOURCE_USERNAME"), valor(env, "SPRING_DATASOURCE_PASSWORD"))) {

            if (soloProbar) {
                int encontrados = 0;
                for (String correo : USUARIOS_PRUEBA) {
                    try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM usuario WHERE correo = ?")) {
                        ps.setString(1, correo);
                        try (ResultSet rs = ps.executeQuery()) { if (rs.next()) encontrados++; }
                    }
                }
                System.out.println("Neon: conexión correcta, " + encontrados + " de " + USUARIOS_PRUEBA.size() + " usuarios encontrados.");
                return;
            }

            // todo en una transaccion: si la comprobacion falla, se deshace y Neon queda como estaba
            c.setAutoCommit(false);
            try {
                int actualizados = 0;
                for (String correo : USUARIOS_PRUEBA) {
                    try (PreparedStatement ps = c.prepareStatement("UPDATE usuario SET contrasena = ? WHERE correo = ?")) {
                        ps.setString(1, hash);
                        ps.setString(2, correo);
                        actualizados += ps.executeUpdate();
                    }
                }

                // se vuelve a leer el hash guardado y se comprueba con BCrypt, como hace el login
                boolean todosOk = true;
                for (String correo : USUARIOS_PRUEBA) {
                    try (PreparedStatement ps = c.prepareStatement("SELECT contrasena FROM usuario WHERE correo = ?")) {
                        ps.setString(1, correo);
                        try (ResultSet rs = ps.executeQuery()) {
                            boolean ok = rs.next() && bcrypt.matches(clave, rs.getString(1));
                            todosOk &= ok;
                            System.out.println("  " + correo + ": " + (ok ? "la nueva contraseña funciona" : "ERROR, revisar"));
                        }
                    }
                }

                if (!todosOk) {
                    c.rollback();
                    salir("Neon: no se guardó nada (la comprobación falló).");
                }
                c.commit();
                System.out.println("Neon: " + actualizados + " usuarios actualizados.");
            } catch (Exception e) {
                c.rollback();
                System.out.println("Neon: no se guardó nada por un error.");
                throw e;
            }
        }
    }

    /** Reemplaza el hash viejo del script por el nuevo, para que una BD nueva también quede bien. */
    private static void actualizarScript(String hash, boolean soloProbar) throws Exception {
        String contenido = Files.readString(SCRIPT, StandardCharsets.UTF_8);
        Matcher m = HASH_BCRYPT.matcher(contenido);
        long cantidad = m.results().count();
        if (soloProbar) {
            System.out.println("Script: se reemplazarían " + cantidad + " hashes en " + SCRIPT.normalize() + ".");
            return;
        }
        // en Windows git puede bajar el script con saltos \r\n: se reemplaza el comentario con los dos tipos de salto
        String nuevo = HASH_BCRYPT.matcher(contenido).replaceAll(Matcher.quoteReplacement(hash))
                .replace(COMENTARIO_VIEJO, COMENTARIO_NUEVO)
                .replace(COMENTARIO_VIEJO.replace("\n", "\r\n"), COMENTARIO_NUEVO.replace("\n", "\r\n"));
        Files.writeString(SCRIPT, nuevo, StandardCharsets.UTF_8);
        System.out.println("Script: " + cantidad + " hashes reemplazados en " + SCRIPT.normalize() + ".");
    }

    /** Lee la contraseña sin mostrarla en pantalla (si la consola lo permite). */
    private static char[] leerOculto(String mensaje) throws Exception {
        Console consola = System.console();
        if (consola != null) {
            char[] leida = consola.readPassword(mensaje);
            if (leida == null) salir("No se escribió nada.");
            return leida;
        }
        System.out.print(mensaje);
        String linea = ENTRADA.readLine();
        if (linea == null) salir("No se escribió nada.");
        return linea.toCharArray();
    }

    /** Mismas credenciales que usa el backend: variables de entorno o el archivo .env. */
    private static Properties leerEnv() throws Exception {
        Properties p = new Properties();
        Path env = Path.of(".env");
        if (Files.exists(env)) {
            try (var lector = Files.newBufferedReader(env, StandardCharsets.UTF_8)) { p.load(lector); }
        }
        return p;
    }

    private static String valor(Properties env, String clave) {
        String delSistema = System.getenv(clave);
        return (delSistema != null && !delSistema.isBlank()) ? delSistema : env.getProperty(clave);
    }

    private static void salir(String mensaje) {
        System.out.println(mensaje);
        System.exit(1);
    }
}
