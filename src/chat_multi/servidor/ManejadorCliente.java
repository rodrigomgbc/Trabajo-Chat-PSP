package chat_multi.servidor;

import chat_multi.comun.Configuracion;
import chat_multi.comun.Constantes;

import java.io.*;
import java.net.Socket;

/*
* Esta clase maneja la conexion de los clientes con el servidor
* Se hace uso de ExecutorService para hacerlo posible
* Valida autenticación, parsea comandos y gestiona todas las operaciones del usuario
*/

public class ManejadorCliente implements Runnable {

    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private Usuario usuario;
    private GestorSalas gestorSalas;
    private GestorUsuarios gestorUsuarios;
    private Logger logger;
    private boolean conectado = true;
    private boolean esAdmin = false;

    // Constructor. Recibe el socket y referencia a los gestores(usuarios y salas)

    public ManejadorCliente(Socket socket, GestorSalas gestorSalas, GestorUsuarios gestorUsuarios, Logger logger) {
        this.socket = socket;
        this.gestorSalas = gestorSalas;
        this.gestorUsuarios = gestorUsuarios;
        this.logger = logger;
    }

    // Método run, ejecutado por el thread del pool

    @Override
    public void run() {
        try {
            //Inicialización de streams
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

            // Validación de nombre y rol

        } catch () {

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {

        }
    }
    // Autenticación
    private boolean validarAutenticacion() {
        try {
            salida.println("Bienvenido, ¿Con qué nombre quieres conectarte?");
            salida.print(">>> ");
            salida.flush();

            String nombre = entrada.readLine();
            if (nombre == null || nombre.isBlank()) {
                salida.println("El nombre no puede estár vacio.");
                return false;
            }

            // Validar nombre único
            if (gestorUsuarios.existeUsuario(nombre)) {
                salida.println("Ese nombre ya está en uso. Prueba con otro nombre.");
                return false;
            }

            // Pedir rol
            salida.println("Elige rol. basico o admin");
            salida.print(">>> ");
            salida.flush();

            String rol = entrada.readLine().toLowerCase();
            if (!rol.equals(Constantes.ROL_BASICO) && !rol.equals(Constantes.ROL_ADMIN)) {
                salida.println("Rol invalido, debes elegir 'basico' o 'admin'.");
                return false;
            }

            // Si es admin, tienes que pedir contraseña
            if (rol.equals(Constantes.ROL_ADMIN)) {
                salida.println("Introduce la contraseña de administrador:");
                salida.print(">>> ");
                salida.flush();

                String password = entrada.readLine();
                String passwordCorrecta = Configuracion.getAdminPassword();

                if (!password.equals(passwordCorrecta)) {
                    salida.println("Contraseña incorrecta.");
                    return false;
                }

                esAdmin = true;
            }

            // Crear usuario
            String salaInicial = esAdmin ? Constantes.SALA_JIUSTON : Constantes.SALA_RECEPCION;
            usuario = new Usuario(nombre, rol, salaInicial, salida);

            // Agregar usuario al gestor
            String resultado = gestorUsuarios.agregarUsuario(usuario);
            if (!resultado.equals("Agregado con exito.")) {
                salida.println("Error al conectarse: " + resultado);
                return false;
            }

            // Agregar usuario a la sala inicial
            Sala salaActual = gestorSalas.obtenerSala(salaInicial);
            if (!salaActual.agregarUsuario(usuario)) {
                salida.println("La salida está llena. No se pudo conectar.");
                gestorUsuarios.eliminarUsuario(nombre);
                return false;
            }

            // Notificar a otros usuarios de la sala
            salaActual.notificarMensaje(usuario.getNombre() + " ha entrado en la sala.");
            return true;
        } catch (IOException e) {
            System.err.println("Error en autenticación: " + e.getMessage());
            return false;
        }
    }

    // Procesamiento de comandos
}