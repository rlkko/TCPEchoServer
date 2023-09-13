/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package personal.playground;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author ricar
 */
public class newFile {

    private static final int TELNET_PORT = 8080;
    private static final int BUFLEN = 1000;

    public static void main(String[] args) throws SocketException, IOException {
        final List<Socket> users = new ArrayList<>();

        //create server socket
        final ServerSocket server = new ServerSocket(TELNET_PORT);
        //Instance the ThreadPool class with 5 fixed threads to be used by client connections
        final ExecutorService executorService = Executors.newFixedThreadPool(5);

        final PipedInputStream pipedInputStream = new PipedInputStream();
        final PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);

        //Message Broadcaster
        new Thread(() -> {
            try {
                //template buffer
                byte[] buf = new byte[BUFLEN];
                while (!server.isClosed()) {
                    //retrieve data
                    int len = pipedInputStream.read(buf);

                    //broadcast to all connected users
                    users.forEach((user) -> {
                        try {
                            user.getOutputStream().write(buf, 0, len);
                        } catch (IOException ex) {
                            ex.printStackTrace(System.out);
                        }
                    });

                }
            } catch (IOException ex) {
                ex.printStackTrace(System.out);
            }

        }).start();

        while (true) {
            //create client object
            Socket newClient = server.accept();
            //store client object's IP information
            InetSocketAddress remote = (InetSocketAddress) newClient.getLocalSocketAddress();

            //create thread for each client
            executorService.submit(() -> {

                InputStream in;
                try {
                    //add a timeout for efficiency
                    newClient.setSoTimeout(5000);

                    //alert for new user connection
                    System.out.println(remote.getHostName() + " connected with port " + remote.getPort());
                    users.add(newClient);

                    //retrieve input and output streams to send and recieve data
                    in = newClient.getInputStream();

                    //create connection loop
                    byte[] buffer = new byte[1000];
                    while (newClient.isConnected() && in.read(buffer) > 0) {
                        //build and send string
                        byte[] message = (remote.getHostName() + " : " + new String(buffer)).getBytes();
                        pipedOutputStream.write(message);
                    }

                } catch (SocketException | SocketTimeoutException ex) {
                    System.out.println(remote.getHostName() + " timed out!");
                    users.remove(newClient);
                } catch (IOException ex) {
                    ex.printStackTrace(System.out);
                }

            }
            );

        }

    }
}
