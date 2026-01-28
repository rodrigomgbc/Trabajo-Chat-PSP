package chat_multi.servidor;

// Esta clase representa un usario conectado al servidor
// Almacena info del cliente

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static chat_multi.comun.Constantes.ROL_ADMIN;
import static chat_multi.comun.Constantes.ROL_BASICO;

public class Usuario {

    // Declaración atributos
    private String nombre;                      // Nombre único del usuario
    private String rol;                         // BASICO O ADMIN (constantes)
    private String salaActual;                  // Sala en la que está ahora
    private LocalDateTime tiempoConexion;       // Instante exacto de conexión
    private PrintWriter escritor;               // Para enviar el mensaje directamente

    // Constructor
    public Usuario(String nombre, String rol, String salaActual, PrintWriter escritor) {
        this.nombre = nombre;
        this.rol = rol;
        this.salaActual = salaActual;
        this.tiempoConexion = LocalDateTime.now();
        this.escritor = escritor;
    }

    // Getters

    public String getNombre() {
        return nombre;
    }

    public String getRol() {
        return rol;
    }

    public String getSalaActual() {
        return salaActual;
    }

    public LocalDateTime getTiempoConexion() {
        return tiempoConexion;
    }

    public PrintWriter getEscritor() {
        return escritor;
    }

    // Setter necesario

    public void setSalaActual(String salaActual) {
        this.salaActual = salaActual;
    }

    // Métodos importantes

    // Método para enviar un mensaje al usuario
    public void enviarMensaje(String mensaje) {
        if (escritor != null) {
            escritor.println(mensaje);
        }
    }

    // Método para calcular cuanto tiempo lleva conectado un usuario
    public long getTiempoConectadoSegundos() {
        return ChronoUnit.SECONDS.between(tiempoConexion, LocalDateTime.now());
    }

    // Métodos para comprobar el rol

    // Usuario normal
    public boolean esNormal() {
        return rol.equals(ROL_BASICO);
    }

    // Usuario administrador
    public boolean esAdmin() {
        return rol.equals(ROL_ADMIN);
    }

    @Override
    public String toString() {
        return String.format(
                "Usuario: %s | Rol: %s | Sala: %s | Tiempo conectado: %d segundos",
                nombre, rol, salaActual, getTiempoConectadoSegundos()
        );
    }
}
