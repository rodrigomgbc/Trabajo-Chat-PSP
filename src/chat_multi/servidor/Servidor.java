package chat_multi.servidor;

/*
* Esta clase es el "servidor de la aplicacion"
* Escucha conexiones de cleintes y las atiene con ManejadorCliente en threads separados
* Utiliza ExecutorService para gestionar multiples conexiones concurrentes
*/

import chat_multi.comun.Configuracion;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Servidor {
    public static void main(String[] args) {
        String ip = Configuracion.getServidorIp();
        int puerto = Configuracion.getServidorPuerto();
        int maximoClientes = Configuracion.getServidorMaxThreads();

        // Creacion de gestores que se comparten entre los ManejadorCliente
        GestorSalas gestorSalas = new GestorSalas();
        GestorUsuarios gestorUsuarios = new GestorUsuarios();
        Logger logger = new Logger();

        // Creación del pool de hilos
        ExecutorService poolHilos = Executors.newFixedThreadPool(maximoClientes);

        try (ServerSocket servidor = new ServerSocket(puerto)) {
            System.out.println("SERVIDOR CHAT MULTI-USUARIO INICIADO");
            System.out.println("IP: " + ip);
            System.out.println("Puerto: " + puerto);
            System.out.println("Max Clientes: " + maximoClientes);
            System.out.println();

            // Bucle principal para aceptar conexiones
            while (true) {
                Socket socket = servidor.accept();
                System.out.println("[CONEXIÓN] Nueva conexión desde: " + socket.getInetAddress() + ":" + socket.getPort());

                // Creción de ManejadorCliente con los gestores compartidos
                ManejadorCliente manejador = new ManejadorCliente(socket, gestorSalas, gestorUsuarios, logger);

                // Ejecución de un thread del thread pool
                poolHilos.execute(manejador);
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        } finally {
            poolHilos.shutdown();
            System.out.println("\n[SERVIDOR] Apagado correctamente.");
        }
    }
}
