package org.dragonskulle.network.components;

import org.dragonskulle.network.ClientEars;
import org.dragonskulle.network.ClientListener;
import org.dragonskulle.network.NetworkClient;

import java.util.Scanner;

public class NetworkedComponentExample extends INetworkable{

    SyncBool syncMe = new SyncBool(false);
    SyncString syncMeAlso = new SyncString();

    NetworkedComponentExample(NetworkObject networkObject) {
        super(networkObject);
    }

    public static void  main(String[] args) {
//        System.out.println("A server should be setup before running. Continue?");
//        new Scanner(System.in).nextLine();
//        ClientListener clientListener = new ClientEars();
//        NetworkClient networkClient = new NetworkClient("127.0.0.1", 7000, clientListener);
        NetworkedComponentExample component = new NetworkedComponentExample(null);
        try {
            component.connectSyncVars();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}

