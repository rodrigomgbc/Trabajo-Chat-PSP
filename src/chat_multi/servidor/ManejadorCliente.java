package chat_multi.servidor;

import chat_multi.comun.Configuracion;
import chat_multi.comun.Constantes;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
    private void procesarComando(String linea) {
        String[] partes = linea.substring(1).split(" ", 2);
        String comando = partes[0].toLowerCase();
        String argumento = partes.length > 1 ? partes[1] : "";

        switch (comando) {
            // Comandos para admin y basico
            case "ayuda" -> cmdAyuda();
            case "quien_soy" -> cmdQuienSoy();
            case "salas" -> cmdSalas();
            case "usuarios" -> cmdUsuarios(argumento);
            case "mensaje" -> cmdMensaje(argumento);
            case "abandona" -> cmdAbandona();

            // Comandos para administradores
            case "crea" -> {
                if (validarAdmin() cmdCrea(argumento));
            }
            case "elimina" -> {
                if (validarAdmin() cmdElimina(argumento));
            }
            case "cambiar_aforo" -> {
                if (validarAdmin() cmdCambiaAforo(argumento));
            }
            case "elimina_forzado" -> {
                if (validarAdmin() cmdEliminaForzado(argumento));
            }
            case "expulsa" -> {
                if (validarAdmin() cmdExpulsa(argumento));
            }
            case "info_sala" -> {
                if (validarAdmin() cmdInfoSala(argumento));
            }
            case "info_usuario" -> {
                if (validarAdmin() cmdInforUsuario(argumento));
            }
            case "mensaje_todos" -> {
                if (validarAdmin() cmdMensajeTodos(argumento));
            }
            case "apaga" -> {
                if (validarAdmin() cmdApaga());
            }

            // Comandos solo usuario basico
            case "unirse" -> {
                if (!esAdmin) {
                    cmdUnirse(argumento);
                } else {
                    salida.println("Los administradores no pueden cambiar de sala.");
                }
            }
            case "ping" -> {
                if (!esAdmin) {
                    cmdPing(argumento);
                } else {
                    salida.println("Solo los usuarios basicos pueden usar ping.");
                }
            }

            default -> salida.println("comando desconocido: /" + comando + ".Escribe /ayuda");
        }
    }

    /*
    * Valida si el usuario es admin
    */
    private boolean validarAdmin() {
        if (!esAdmin) {
            salida.println("Este comando es solo para administradores.");
            return false;
        }
        return true;
    }

    // Comandos
    /*
    * /ayuda - Muestra ayuda según el tipo de usuario
    */
    private void cmdAyuda() {
        if (esAdmin) {
            salida.println("---------------- COMANDOS DE ADMINISTRADOR ----------------");
            salida.println("/ayuda                          - Muestra esta ayuda");
            salida.println("/quien_soy                      - Información personal");
            salida.println("/salas                          - Lista todas las salas");
            salida.println("/usuarios <sala>                - Usuarios en una sala");
            salida.println("/mensaje <usuario> <msg>        - Mensaje privado");
            salida.println("/mensaje_todos <msg>            - Mensaje a todos los usuarios");
            salida.println("/crea <sala> [aforo]            - Crear sala");
            salida.println("/elimina <sala>                 - Eliminar sala vacía");
            salida.println("/elimina_forzado <sala>         - Eliminar sala (expulsa users)");
            salida.println("/cambia_aforo <sala> <aforo>    - Cambiar aforo");
            salida.println("/expulsa <usuario> [msg]        - Expulsar usuario");
            salida.println("/info_sala <sala>               - Información de sala");
            salida.println("/info_usuario <usuario>         - Información de usuario");
            salida.println("/apaga                          - Apagar servidor");
            salida.println("/abandona                       - Desconectarse");
            salida.println("");
        } else {
            salida.println("------------------ COMANDOS DE USUARIO ------------------");
            salida.println("/ayuda                      - Muestra esta ayuda");
            salida.println("/quien_soy                  - Información personal");
            salida.println("/salas                      - Lista las salas disponibles");
            salida.println("/usuarios <sala>            - Usuarios en una sala");
            salida.println("/unirse <sala>              - Cambiar a otra sala");
            salida.println("/ping <usuario>             - Ver si está en tu sala");
            salida.println("/mensaje <usuario> <msg>    - Mensaje privado");
            salida.println("/mensaje <msg>              - Mensaje a todos en la sala");
            salida.println("/abandona                   - Desconectarse");
        }
    }

    /*
    * /quien_soy - Información del  usuario
    */
    private void cmdQuienSoy() {
        long segundosConectado = ChronoUnit.SECONDS.between(usuario.getTiempoConexion(), LocalDateTime.now());
        long minutos = segundosConectado / 60;
        long segundos = segundosConectado % 60;

        salida.println("Información personal.");
        salida.println("| Nombre:           " + usuario.getNombre());
        salida.println("| Rol:              " + usuario.getRol().toUpperCase());
        salida.println("| Sala actual:      " + usuario.getSalaActual());
        salida.println("| Tiempo conectado  " + minutos + "m " + segundos + "s");
        salida.println("");
    }

    /*
    * /salas - Lista de sala
    */
    private void cmdSalas() {
        salida.println("SALAS DISPONIBLES");

        gestorSalas.obtenerTodasLasSalas().stream()
                .filter(sala -> esAdmin || !sala.getNombre().equals(Constantes.SALA_JIUSTON))
                .map(this::formatearSala)
                .forEach(salida::println);
    }

    private String formatearSala(Sala sala) {
        String tipo = sala.isEsReservada() ? " [RESERVADA]" : "";
        return String.format("| %s%s%n|     Usuarios: %d/%d",
                sala.getNombre(),
                tipo,
                sala.getCantidadUsuario(),
                sala.getNombre()
        );
    }

    /**
     * /usuarios <sala> - Usuarios en una sala
     */
    private void cmdUsuarios(String argumento) {
        if (argumento.isBlank()) {
            salida.println("Uso: /usuarios <sala>");
            return;
        }

        String nombreSala = argumento.trim();

        if (!gestorSalas.existeSala(nombreSala)) {
            salida.println("La sala '" + nombreSala + "' no existe.");
            return;
        }

        // Usuario básico no puede ver usuarios de jiuston
        if (!esAdmin && nombreSala.equals(Constantes.SALA_JIUSTON)) {
            salida.println("No tienes permisos para ver esa sala.");
            return;
        }

        var usuarios = gestorUsuarios.obtenerUsuariosPorSala(nombreSala);

        salida.println("\n USUARIOS EN " + nombreSala.toUpperCase());
        if (usuarios.isEmpty()) {
            salida.println("│ (ninguno)");
        } else {
            for (Usuario u : usuarios) {
                salida.println("│  " + u.getNombre() + " (" + u.getRol() + ")");
            }
        }
        salida.println("");
    }
}