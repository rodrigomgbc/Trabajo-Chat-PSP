package chat_multi.servidor;

import chat_multi.comun.Configuracion;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Servidor {

    public static void main(String[] args) {
        int puerto = Configuracion.getServidorPuerto();
        int maximoClientes = Configuracion.getServidorMaxThreads();

        ExecutorService poolHilos = Executors.newFixedThreadPool(maximoClientes);

        try (ServerSocket servidor = new ServerSocket(puerto)) {
            System.out.println("Servidor iniciado en el puerto " + puerto);
            System.out.printf("Soy capaz de atender a %d clientes a la vez%n", maximoClientes);

            while (true) {
                Socket socket = servidor.accept();
                System.out.println("Nueva conexión desde: " + socket.getInetAddress());
                poolHilos.execute(new ManejadorCliente(socket));
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            poolHilos.shutdown();
        }
    }
}
