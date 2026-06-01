package com.auction.network;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

class ClientHandlerTest {

    // Tạo socket pair local để test ClientHandler bằng socket thật
    private SocketPair socketPair() throws Exception {
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        Socket clientSocket = new Socket("127.0.0.1", port);
        Socket serverSideSocket = serverSocket.accept();

        serverSocket.close();

        clientSocket.setSoTimeout(2000);

        return new SocketPair(clientSocket, serverSideSocket);
    }

    private static class SocketPair {
        final Socket clientSocket;
        final Socket serverSideSocket;

        SocketPair(Socket clientSocket, Socket serverSideSocket) {
            this.clientSocket = clientSocket;
            this.serverSideSocket = serverSideSocket;
        }

        void closeAll() {
            try {
                clientSocket.close();
            } catch (Exception ignored) {
            }

            try {
                serverSideSocket.close();
            } catch (Exception ignored) {
            }
        }
    }

    // Đợi thread kết thúc
    private void joinAndAssertStopped(Thread thread) throws InterruptedException {
        thread.join(2000);
        assertFalse(thread.isAlive());
    }

    // =========================================================
    // run()
    // =========================================================

    // ClientHandler nhận PING từ client và trả PONG về client
    @Test
    void runShouldReturnPongWhenClientSendsPing() throws Exception {
        SocketPair pair = socketPair();

        ClientHandler handler = new ClientHandler(pair.serverSideSocket);
        Thread thread = new Thread(handler);
        thread.start();

        PrintWriter clientOut = new PrintWriter(pair.clientSocket.getOutputStream(), true);
        BufferedReader clientIn = new BufferedReader(new InputStreamReader(pair.clientSocket.getInputStream()));

        clientOut.println("""
                {
                  "action": "PING"
                }
                """.replace("\n", ""));

        String response = clientIn.readLine();

        assertNotNull(response);
        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains("\"action\":\"PING\""));
        assertTrue(response.contains("PONG"));

        pair.clientSocket.close();
        joinAndAssertStopped(thread);

        pair.closeAll();
    }

    // ClientHandler phải bỏ qua dòng rỗng rồi vẫn xử lý PING sau đó
    @Test
    void runShouldIgnoreBlankLinesAndProcessNextValidRequest() throws Exception {
        SocketPair pair = socketPair();

        ClientHandler handler = new ClientHandler(pair.serverSideSocket);
        Thread thread = new Thread(handler);
        thread.start();

        PrintWriter clientOut = new PrintWriter(pair.clientSocket.getOutputStream(), true);
        BufferedReader clientIn = new BufferedReader(new InputStreamReader(pair.clientSocket.getInputStream()));

        clientOut.println("   ");
        clientOut.println("");
        clientOut.println("""
                {
                  "action": "PING"
                }
                """.replace("\n", ""));

        String response = clientIn.readLine();

        assertNotNull(response);
        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains("PONG"));

        pair.clientSocket.close();
        joinAndAssertStopped(thread);

        pair.closeAll();
    }

    // ClientHandler xử lý được nhiều request PING liên tiếp
    @Test
    void runShouldProcessMultiplePingRequests() throws Exception {
        SocketPair pair = socketPair();

        ClientHandler handler = new ClientHandler(pair.serverSideSocket);
        Thread thread = new Thread(handler);
        thread.start();

        PrintWriter clientOut = new PrintWriter(pair.clientSocket.getOutputStream(), true);
        BufferedReader clientIn = new BufferedReader(new InputStreamReader(pair.clientSocket.getInputStream()));

        String pingJson = """
                {
                  "action": "PING"
                }
                """.replace("\n", "");

        clientOut.println(pingJson);
        clientOut.println(pingJson);
        clientOut.println(pingJson);

        String response1 = clientIn.readLine();
        String response2 = clientIn.readLine();
        String response3 = clientIn.readLine();

        assertNotNull(response1);
        assertNotNull(response2);
        assertNotNull(response3);

        assertTrue(response1.contains("PONG"));
        assertTrue(response2.contains("PONG"));
        assertTrue(response3.contains("PONG"));

        pair.clientSocket.close();
        joinAndAssertStopped(thread);

        pair.closeAll();
    }

    // ClientHandler phải set socket timeout là 60000ms
    @Test
    void runShouldSetSocketTimeoutToSixtySeconds() throws Exception {
        SocketPair pair = socketPair();

        ClientHandler handler = new ClientHandler(pair.serverSideSocket);
        Thread thread = new Thread(handler);
        thread.start();

        PrintWriter clientOut = new PrintWriter(pair.clientSocket.getOutputStream(), true);
        BufferedReader clientIn = new BufferedReader(new InputStreamReader(pair.clientSocket.getInputStream()));

        clientOut.println("""
                {
                  "action": "PING"
                }
                """.replace("\n", ""));

        String response = clientIn.readLine();

        assertNotNull(response);
        assertEquals(60000, pair.serverSideSocket.getSoTimeout());

        pair.clientSocket.close();
        joinAndAssertStopped(thread);

        pair.closeAll();
    }

    // JSON sai cú pháp vẫn được chuyển cho RequestDispatcher và trả lỗi INVALID_JSON
    @Test
    void runShouldReturnInvalidJsonResponseWhenClientSendsBadJson() throws Exception {
        SocketPair pair = socketPair();

        ClientHandler handler = new ClientHandler(pair.serverSideSocket);
        Thread thread = new Thread(handler);
        thread.start();

        PrintWriter clientOut = new PrintWriter(pair.clientSocket.getOutputStream(), true);
        BufferedReader clientIn = new BufferedReader(new InputStreamReader(pair.clientSocket.getInputStream()));

        clientOut.println("{ invalid-json");

        String response = clientIn.readLine();

        assertNotNull(response);
        assertTrue(response.contains("\"success\":false"));
        assertTrue(response.contains("INVALID_JSON"));

        pair.clientSocket.close();
        joinAndAssertStopped(thread);

        pair.closeAll();
    }

    // Client đóng kết nối ngay thì ClientHandler không được treo
    @Test
    void runShouldStopWhenClientDisconnectsImmediately() throws Exception {
        SocketPair pair = socketPair();

        ClientHandler handler = new ClientHandler(pair.serverSideSocket);
        Thread thread = new Thread(handler);
        thread.start();

        pair.clientSocket.close();

        joinAndAssertStopped(thread);

        pair.closeAll();
    }
}