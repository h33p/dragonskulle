package org.dragonskulle.network;

import java.util.Scanner;

public class CreateClient {
    static String ip;
    static int port;
    static Client client;
    static String command;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter IP: ");
        ip = scanner.nextLine();
        System.out.println("Enter Port: ");
        port = scanner.nextInt();
        System.out.println("Creating client");
        ClientListener clientEars = new ClientEars();
        client = new Client(ip, port, clientEars);

        OUTER_LOOP:
        while (true) {
            System.out.println("Enter Command: ");
            command = scanner.nextLine();
            switch (command.toUpperCase()) {
                case ("K"):
                    System.out.println("Killing Client");
                    client.dispose();
                    break OUTER_LOOP;
                case ("S"):
                    System.out.println("Sending Message to server");
                    client.send("Message");
                    break;
                default:
            }
        }
    }

}

