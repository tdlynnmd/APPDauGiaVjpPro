package com.auction.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientNetworkManager {
    private static ClientNetworkManager instance;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private ClientNetworkManager() {
        try {
            this.socket = new Socket("localhost", 5555);
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static ClientNetworkManager getInstance() {
        if (instance == null) instance = new ClientNetworkManager();
        return instance;
    }

    public PrintWriter getWriter() { return writer; }
    public BufferedReader getReader() { return reader; }
}