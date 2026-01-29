package chat_multi.servidor;

/*
* Esta clase gestiona el registro de eventos del servidor en un archivo de log
* Regostra conexiones, desconexiona y expulsiones
*/

import chat_multi.comun.Configuracion;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private String rutaLog;
    private DateTimeFormatter formateador;

    public Logger() {
        this.rutaLog = Configuracion.getLog();
        this.formateador = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    /*
    * Método para registrar un evento en el archivo de log
    */
    private void registrar(String evento) {
        try (FileWriter escritor = new FileWriter(rutaLog, true)) {
            // El true sirve para que se añada al final del archivo y no se sobreescriba
            String timestamp = LocalDateTime.now().format(formateador);
            String linea = "[" + timestamp + "] " + evento + "\n";
            escritor.write(linea);
        } catch (IOException e) {
            System.err.println("Error escribiendo en el log: " + e.getMessage());
        }
    }

    /*
    * Método para registrar la conexión de un usuario
    */
    public void registrarConexion(String nombreUsuario, String rol, String salaInicial) {
        String evento = "Conexión - Usuario: " + nombreUsuario + " | Rol: " + rol + " | Sala: " + salaInicial;
        registrar(evento);
    }

    /*
    * Método para registrar la desconexión de un usuario
    */
    public void registrarDesconexion(String nombreUsuario) {
        String evento = "Desconexión - Usuario: " + nombreUsuario;
        registrar(evento);
    }

    /*
    * Método para registrar la expulsión de un usuario
    */
    public void registrarExpulsion(String nombreUsuario, String razon) {
        String evento = "Expulsión - Usuario: " + nombreUsuario + " | Razón: " + razon;
        registrar(evento);
    }
}
