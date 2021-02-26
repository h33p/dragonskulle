package org.dragonskulle.network.components;

import org.dragonskulle.network.ClientEars;
import org.dragonskulle.network.ClientListener;
import org.dragonskulle.network.NetworkClient;

import java.util.Scanner;

public class NetworkCommunicationsTest {


    NetworkCommunicationsTest() {
        System.out.println("A server should be setup before running. Continue?");
        new Scanner(System.in).nextLine();
        ClientListener clientListener = new ClientEars();
        NetworkClient networkClient = new NetworkClient("127.0.0.1", 7000, clientListener);
//        NetworkObject networkObject = new NetworkObject(networkClient);
    }

    public static void main(String[] args) {
        NetworkCommunicationsTest runner = new NetworkCommunicationsTest();
        new Scanner(System.in).nextLine();
        //on connect to server, the server will send us the map, it will also send us a spawn request for our captiol. our capitol will extends INetworkable
        //capitol component will have
        // SyncBool syncMe = new SyncBool(false);
        //    SyncString syncMeAlso = new SyncString("Hello World");
    }
}

