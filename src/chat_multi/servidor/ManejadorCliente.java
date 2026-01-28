package chat_multi.servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ManejadorCliente implements Runnable {
    private static final String COMANDO_FIN = "fin";
    private final Socket socket;

    public ManejadorCliente(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                // lo que se recibe del chat_multi.cliente
                BufferedReader entrada = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );
                // lo que se envía al chat_multi.cliente
                PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // mensaje de bienvenida, que se envía nada más hacerse cargo del chat_multi.cliente un ManejadorCliente
            salida.printf("Bienvenido al chat_multi.servidor concurrente. Escribe '%s' para salir %n", COMANDO_FIN);

            String linea;
            while ((linea = entrada.readLine()) != null) {
                System.out.printf("[%s] %s%n", socket.getInetAddress(), linea);

                if (linea.equalsIgnoreCase(COMANDO_FIN)) {
                    salida.println("Adiós!");
                    break;
                }

                // respuesta simulada del chat_multi.servidor
                salida.println("Servidor recibió: " + linea.toUpperCase());
            }

        } catch (IOException e) {
            System.err.printf("Error con el chat_multi.cliente %s: %s %n",
                    socket.getInetAddress(), e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
            System.out.printf("Cliente desconectado: %s %n", socket.getInetAddress());
        }
    }
}