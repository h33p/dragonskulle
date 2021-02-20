package org.dragonskulle.network.components;

import org.dragonskulle.network.ClientEars;
import org.dragonskulle.network.ClientListener;
import org.dragonskulle.network.NetworkClient;

import java.util.Scanner;

public class NetworkedComponentExample extends INetworkable{

    SyncBool syncMe = new SyncBool(false);
    SyncString syncMeAlso = new SyncString("Hello World");

    NetworkedComponentExample(NetworkObject networkObject) {
        super(networkObject);
    }

    void dispose(){
        super.dispose();
    }
    public static void  main(String[] args) {
        System.out.println("A server should be setup before running. Continue?");
        new Scanner(System.in).nextLine();
        ClientListener clientListener = new ClientEars();
        NetworkClient networkClient = new NetworkClient("127.0.0.1", 7000, clientListener);
        NetworkObject networkObject = new NetworkObject(networkClient);
        NetworkedComponentExample component = new NetworkedComponentExample(networkObject);
        try {
            component.connectSyncVars();
            new Scanner(System.in).nextLine();
            component.dispose();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}

