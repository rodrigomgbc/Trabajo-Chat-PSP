package chat_multi.cliente;

import chat_multi.comun.Configuracion;
import chat_multi.comun.Constantes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/*
* Este es el cliente del chat multi-usuario
* Se conecta al servidor y permite enviar/recibir mensajes
* Implementa reconexion automatica si falla la conexión
*/
public class Cliente {

    public static void main(String[] args) {
        String host = Configuracion.getServidorIp();
        int puerto = Configuracion.getServidorPuerto();

        int intentos = 0;
        boolean conectado = false;

        // Intentar conectarse con reintentos
        while (intentos < Constantes.MAX_REINTENTOS && !conectado) {
            try {
                conectarAlServidor(host, puerto);
                conectado = true;
            } catch (IOException e) {
                intentos++;
                System.err.println("Error de conexión (intento " + intentos + "/" + Constantes.MAX_REINTENTOS + "): " + e.getMessage());

                if (intentos < Constantes.MAX_REINTENTOS) {
                    System.out.println("Reintentando en 10 segundos...");
                    try {
                        Thread.sleep(Constantes.TIEMPO_ESPERA_REINTENTOS);
                    } catch (InterruptedException ie) {
                        System.err.println("Interrupción durante espera: " + ie.getMessage());
                        Thread.currentThread().interrupt();
                        intentos = Constantes.MAX_REINTENTOS;  // Fuerza la salida del while
                    }
                }
            }
        }

        if (!conectado) {
            System.err.println("No se pudo conectar al servidor despues de " + Constantes.MAX_REINTENTOS + " intentos.");
            System.exit(1);
        }
    }

    // Metodo para conectarse al servidor
    private static void conectarAlServidor(String host, int puerto) throws IOException {
        try (Socket socket = new Socket(host, puerto);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Conectado al servidor en " + host + ":" + puerto);
            System.out.println();

            // Thread para recibir mensajes del servidor
            Thread receptorThread = new Thread(() -> {
                try {
                    String mensaje;
                    while ((mensaje = entrada.readLine()) != null) {
                        System.out.println("\r" + mensaje);
                        System.out.print("> ");
                        System.out.flush();
                    }
                } catch (IOException e) {
                    System.err.println("Conexión cerrada por el servidor.");
                }
            });

            receptorThread.setDaemon(true);
            receptorThread.start();

            // Thread principal para enviar mensajes
            String linea;
            boolean desconectar = false;

            while (!desconectar) {
                System.out.print("> ");
                linea = scanner.nextLine();

                if (linea.isEmpty()) {
                    continue;
                }

                salida.println(linea);

                if (linea.equalsIgnoreCase(Constantes.COMANDO_ABANDONA)) {
                    System.out.println("\n Desconectando...");
                    desconectar = true;
                }
            }
        } catch (IOException e) {
            throw new IOException("Error de conexion con el servidor: " + e.getMessage(), e);
        }
    }
}
