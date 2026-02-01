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
            salida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // Validación de nombre y rol
            if (!validarAutenticacion()) {
                System.err.println("Autenticación fallida. Cerrando cliente...");
                desconectar();
                return;
            }

            // Usuario autenticado correctamente
            logger.registrarConexion(usuario.getNombre(), usuario.getRol(), usuario.getSalaActual());
            salida.println("¡Conectado a " + Constantes.SERVIDOR_NOMBRE + "!");
            salida.println("Escribe /ayuda para ver los comandos disponibles");

            // Bucle principal para recibir y procesar comandos
            String linea;
            while (conectado && (linea = entrada.readLine()) != null) {
                if (linea.trim().isEmpty()) {
                    continue;
                }

                if (linea.startsWith(Constantes.PREFIJO_COMANDO)) {
                    procesarComando(linea);
                } else {
                    // Mensaje de broadcast a la sala
                    procesarMensajeBroadcast(linea);
                }
            }

        } catch (IOException e) {
            System.err.println("Error en ManejadorCliente: " + e.getMessage());
        } finally {
            desconectar();
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
            if (!rol.equals(Constantes.ROL_BASICO.toLowerCase()) && !rol.equals(Constantes.ROL_ADMIN.toLowerCase())) {
                salida.println("Rol invalido, debes elegir 'basico' o 'admin'.");
                return false;
            }

            // Si es admin, tienes que pedir contraseña
            if (rol.equals(Constantes.ROL_ADMIN.toLowerCase())) {
                salida.println("Introduce la contraseña de administrador:");
                salida.print(">>> ");
                salida.flush();

                String password = entrada.readLine();
                String passwordCorrecta = Configuracion.getAdminPassword();

                if (!password.equals(passwordCorrecta)) {
                    salida.println("Contraseña incorrecta. Desconectando...");
                    salida.flush();
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
                if (validarAdmin()) cmdCrea(argumento);
            }
            case "elimina" -> {
                if (validarAdmin()) cmdElimina(argumento);
            }
            case "cambiar_aforo" -> {
                if (validarAdmin()) cmdCambiaAforo(argumento);
            }
            case "elimina_forzado" -> {
                if (validarAdmin()) cmdEliminaForzado(argumento);
            }
            case "expulsa" -> {
                if (validarAdmin()) cmdExpulsa(argumento);
            }
            case "info_sala" -> {
                if (validarAdmin()) cmdInfoSala(argumento);
            }
            case "info_usuario" -> {
                if (validarAdmin()) cmdInfoUsuario(argumento);
            }
            case "mensaje_todos" -> {
                if (validarAdmin()) cmdMensajeTodos(argumento);
            }
            case "apaga" -> {
                if (validarAdmin()) cmdApaga();
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
    /**
    * Método para comando /ayuda
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

    /**
    * Método para comando /quien_soy
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

    /**
     * Método para comando /salas
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
                sala.getAforo()
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

    /**
     * Método para comando /mensaje
     * /mensaje <usuario> <texto> o /mensaje <texto>
    */
    private void cmdMensaje (String argumento) {
        if (argumento.isBlank()) {
            salida.println("Hace falta argumento. Uso: /mensaje <texto> o /mensaje <usuario> <texto>.");
            return;
        }

        String[] partes = argumento.split(" ", 2);

        if (partes.length == 1) {
            // Mensaje a broadcast a la sala
            String texto = partes[0];
            Sala salaActual = gestorSalas.obtenerSala(usuario.getSalaActual());
            salaActual.notificarMensaje("[BROADCAST] " + usuario.getNombre() + ": " + texto);
        } else {
            // Mensaje privado a un usuario
            String nombreUsuario = partes[0];
            String texto = partes[1];

            Usuario destinatario;
            if (esAdmin) {
                // Los Administradores pueden enviar a cualquier usuario
                destinatario = gestorUsuarios.obtenerUsuario(nombreUsuario);
            } else {
                // Los usuarios basicos solo pueden enviar mensajes a usuarios de su misma sala
                destinatario = gestorUsuarios.obtenerUsuarioPorNombreEnSala(nombreUsuario, usuario.getSalaActual());
            }

            if (destinatario == null) {
                salida.println("Usuario no encontrado " + (esAdmin ? "." : " en esta sala."));
                return;
            }

            if (destinatario.getNombre().equals(usuario.getNombre())) {
                salida.println("No puedes mandarte mensajesa ti mismo.");
                return;
            }

            destinatario.enviarMensaje("[PRIVADO de " + usuario.getNombre() + "]: " + texto);
            salida.println("Mensaje enviado a " + nombreUsuario);
        }
    }

    /**
     * Método para comando /unirse <sala>
     * Solo para usuarios básicos
     */
    private void cmdUnirse(String argumento) {
        if (argumento.isBlank()) {
            salida.println("Uso de /unirse: /unirse <sala>");
            return;
        }

        String nombreSala = argumento.trim();

        // No se puede enstrar a Jiuston
        if (nombreSala.equals(Constantes.SALA_JIUSTON)) {
            salida.println("No tienes permisos para acceder a esta sala.");
            return;
        }

        if (!gestorSalas.existeSala(nombreSala)) {
            salida.println("La sala '" + nombreSala + "'no existe");
            return;
        }

        // No se puede cambiar de sala si ya se está en la sala
        if (usuario.getSalaActual().equals(nombreSala)) {
            salida.println("Ya te encuentras en la sala a la que te quieres unir.");
            return;
        }

        Sala salaActual = gestorSalas.obtenerSala(usuario.getSalaActual());
        Sala salaNueva = gestorSalas.obtenerSala(nombreSala);

        // Comprobación de que la sala nueva no está llena
        if (salaNueva.estaLlena()) {
            salida.println("La sala a la que te estás intentando unir está llena.");
            return;
        }

        // Quitar de la sala actual, en la que te encuentras.
        salaActual.eliminarUsuario(usuario);
        salaNueva.notificarMensaje(usuario.getNombre() + " ha abandonado la sala.");

        // Autoeliminación de la sala si se queda vacia
        gestorSalas.eliminarSiVacia(usuario.getSalaActual());

        // Agregar a la sala nueva
        salaNueva.agregarUsuario(usuario);
        usuario.setSalaActual(nombreSala);
        salaNueva.notificarMensaje(usuario.getNombre() + " ha entrado en la sala.");
        salida.println("Has cambiado a la sala " + nombreSala);
    }

    /**
     * Método para el comando ping
     * /ping <usuario> - Solo para usuarios basicos
     */
    private void cmdPing(String argumento) {
        if (argumento.isBlank()) {
            salida.println("Uso: /ping <usuario>");
            return;
        }

        String nombreUsuario = argumento.trim();
        Usuario destinatario = gestorUsuarios.obtenerUsuarioPorNombreEnSala(nombreUsuario, usuario.getSalaActual());

        if (destinatario == null) {
            salida.println("El usuario '" + nombreUsuario + "' no está en la sala.");
            return;
        }

        salida.println("POOOONG " + nombreUsuario + " está en tu sala.");
    }

    /**
     * Método para abandonar la sala
     * /abandona
     */
    private void cmdAbandona() {
        salida.println("¡Adiós!");
        conectado = false;
    }

    // Métodos exclusivos para admin
    /**
     * Método para crear una sala
     */
    private void cmdCrea(String argumento) {
        if (argumento.isBlank()) {
            salida.println("Uso: /crea <sala> [aforo]");
            return;
        }

        String[] partes = argumento.split(" ");
        String nombreSala = partes[0];

        String resultado;
        if (partes.length > 1) {
            try {
                int aforo = Integer.parseInt(partes[1]);
                resultado = gestorSalas.crearSala(nombreSala, aforo);
            } catch (NumberFormatException e) {
                salida.println("El aforo debe ser un número.");
                return;
            }
        } else {
                resultado = gestorSalas.crearSala(nombreSala);
        }

        if (resultado.equals("Sala creada con exito")) {
            salida.println("sala creada con exito");
        } else {
            salida.println("No se ha podido crear la sala");
        }
    }

    /*
    * Método para el comando para eliminar una sala
    * /elimina <sala>
    */
    private void cmdElimina(String argumento) {
        if (argumento.isBlank()) {
            salida.println("Uso: /elimina <sala>");
            return;
        }

        String nombreSala = argumento.trim();
        String resultado = gestorSalas.eliminarSala(nombreSala);

        if (resultado.equals("Sala eliminada con exito.")) {
            salida.println("Sala eliminada con éxito.");
        } else {
            salida.println("No se ha podido eliminar la sala");
        }
    }

    /*
    * Método para el comando /cambiar_aforo
    * /cambia_aforo <sala> <aforo>
    */
    private void cmdCambiaAforo(String argumento) {
        if (argumento.isBlank()) {
            salida.println("Uso incorrecto del comando. Uso: /cambia_aforo <sala> <aforo>");
            return;
        }

        String[] partes = argumento.split(" ");
        if (partes.length < 2) {
            salida.println("Uso incorrecto del comando. Uso: /cambia_aforo <sala> <aforo>");
            return;
        }

        String nombreSala = partes[0];
        try {
            int nuevoAforo = Integer.parseInt(partes[1]);
            String resultado = gestorSalas.cambiarAforo(nombreSala, nuevoAforo);

            if (resultado.equals("Aforo cambiado correctamente.")) {
                salida.println("éxito" + resultado);
            } else {
                salida.println("fracaso" + resultado);
            }
        } catch (NumberFormatException e) {
            salida.println("ERROR: El afoto debe ser un numero.");
        }
    }

    /*
    * Método para el comando /elimina_forzado, que sirve para eliminar de manera forzosa una sala.
    */
    private void cmdEliminaForzado(String argumento) {
        if (argumento.isBlank()) {
            salida.println("Uso incorrecto del comando. Uso: /elimina_forzado <sala>");
            return;
        }

        String nombreSala = argumento.trim();
        String resultado = gestorSalas.eliminarSalaForzosamente(nombreSala);

        // Revistar el tema del resultado a ver como se ve en terminal
        if (resultado.contains("exito")) {
            salida.println(resultado);
        } else {
            salida.println(resultado);
        }
    }

    /*
    * Método para el comando /expulsa, sirve para expulsar usuarios de una sala
    */
    private void cmdExpulsa(String argumento) {
        if (argumento.isBlank()) {
            salida.println("Uso incorrecto del comando. Uso: /expulsa <usuario> [mensaje]");
            return;
        }

        String[] partes = argumento.split(" ", 2);
        String nombreUsuario = partes[0];
        String razon = partes.length > 1 ? partes[1] : "Sin especificar";

        Usuario usuarioAExpulsar = gestorUsuarios.obtenerUsuario(nombreUsuario);

        if (usuarioAExpulsar == null) {
            salida.println("El usuario que estas intentado expulsar no existe");
            return;
        }

        if (usuarioAExpulsar.esAdmin()) {
            salida.println("No puedes expulsar a otro administrador");
            return;
        }

        // Expulsar al usuario si se cumplen las condiciones
        Sala salaActual = gestorSalas.obtenerSala(usuarioAExpulsar.getSalaActual());
        salaActual.eliminarUsuario(usuarioAExpulsar);
        gestorUsuarios.eliminarUsuario(nombreUsuario);

        usuarioAExpulsar.enviarMensaje("Has sido expulsado de la sala. Razon: " + razon);

        logger.registrarExpulsion(nombreUsuario, razon);
        salida.println("usuario " + nombreUsuario + " expulsado");
    }

    /*
    * Método para el comando /info_sala, este comando da informacion de la sala.
    */
    private void cmdInfoSala(String argumento) {
        if (argumento.isBlank()) {
            salida.println("Uso incorrecto del comando. Uso: /info_sala <sala>");
            return;
        }

        String nombreSala = argumento.trim();
        Sala sala = gestorSalas.obtenerSala(nombreSala);

        if (sala == null) {
            salida.println("La sala de la cual estás intentando obtener información no existe");
            return;
        }

        salida.println("INFORMACIÓN DE SALA");
        salida.println("Nombre: " + sala.getNombre());
        salida.println("Tipo: " + (sala.isEsReservada() ? "Reservada" : "Pública"));
        salida.println("Creada: " + sala.getFechaCreacion());
        salida.println("Usuarios: " + sala.getCantidadUsuario() + "/" + sala.getAforo());
        salida.println();
    }

    /*
    * Método para el comando /info_usuario <usuario>, este comando da info del usuario <usuario>
    */
    private void cmdInfoUsuario(String argumento) {
        if (argumento.isBlank()) {
            salida.println("Uso incorrecto del comando. Uso: /info_usuario <usuario>");
            return;
        }

        String nombreUsuario = argumento.trim();
        Usuario usu = gestorUsuarios.obtenerUsuario(nombreUsuario);

        if (usu == null) {
            salida.println("El usuario no existe.");
            return;
        }

        long segundosConectado = ChronoUnit.SECONDS.between(usu.getTiempoConexion(), LocalDateTime.now());
        long minutos = segundosConectado / 60;
        long segundos = segundosConectado % 60;

        salida.println("USUARIO: " + usu.getNombre() + " ---");
        salida.println("Rol: " + usu.getRol().toUpperCase());
        salida.println("Sala actual: " + usu.getSalaActual());
        salida.println("Tiempo conectado: " + minutos + "m " + segundos + "s");
        salida.println();
    }

    /*
    * Método para el comando /mensaje_todos <mensaje>. Este comando sirve para que un admin mande un mensaje a todos los usuarios de una sala.
    */
    private void cmdMensajeTodos(String argumento) {
        if (argumento.isBlank()) {
            salida.println("Uso incorrecto del comando. Uso: /mensaje_todos <mensaje>");
            return;
        }

        String mensaje = "[ADMIN A TODOS]: " + usuario.getNombre() + ": " + argumento;

        gestorUsuarios.obtenerTodosLosUsuarios().stream()
                .forEach(usu -> usu.enviarMensaje(mensaje));
    }

    /*
    * Método para el comando apagar, /apagar sirve para apagar el servidor
    */
    private void cmdApaga() {
        String mensajeDesconexion = "El servidor ha sido apagado por un administrador.";

        // Desconectar a todos los usuarios
        gestorUsuarios.desconectarTodos(mensajeDesconexion);

        salida.println("Servidor apagado.");
        conectado = false;

        // Salir del programa con exito
        System.exit(0);
    }

    // Procesamiento de mensajes
    /*
    * Método para procesar un mensaje de broadcast pero sin comando
    */
    private void procesarMensajeBroadcast(String texto) {
        Sala salaActual = gestorSalas.obtenerSala(usuario.getSalaActual());
        salaActual.notificarMensaje(usuario.getNombre() + ": " + texto);
    }

    // Desconexión
    /*
    * Desconectar al usuario y limpiar
    */
    private void desconectar() {
        if (usuario != null) {
            // Eliminar de la sala
            Sala salaActual = gestorSalas.obtenerSala(usuario.getSalaActual());
            if (salaActual != null) {
                salaActual.eliminarUsuario(usuario);
                salaActual.notificarMensaje(usuario.getNombre() + " ha abandonado la sala.");

                // Eliminar sala si se queda vacia
                gestorSalas.eliminarSiVacia(usuario.getSalaActual());
            }

            // Eliminar del gestor
            gestorUsuarios.eliminarUsuario(usuario.getNombre());

            // Registrar en el log
            logger.registrarDesconexion(usuario.getNombre());

            System.out.println("[SERVIDOR] Usuario " + usuario.getNombre() + " desconectado.");

            // Cerrar los streams y los sockets
            try {
                if (entrada != null) entrada.close();
                if (salida != null) salida.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("Error cerrando conexión: " + e.getMessage());
            }
        }
    }
}