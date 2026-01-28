package chat_multi.cliente;

import chat_multi.comun.Configuracion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {

    private static final String COMANDO_FIN = "fin";

    public static void main(String[] args) {

        String host = Configuracion.getServidorIp();      // o cliente.host si quieres otro getter
        int puerto = Configuracion.getServidorPuerto();   // o cliente.puerto

        try (Socket socket = new Socket(host, puerto);
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)
        ) {
            System.out.println(entrada.readLine());

            String mensaje;
            while (true) {
                System.out.print("> ");
                mensaje = scanner.nextLine();
                salida.println(mensaje);

                if (mensaje.equalsIgnoreCase(COMANDO_FIN))
                    break;

                System.out.println("Respuesta: " + entrada.readLine());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
