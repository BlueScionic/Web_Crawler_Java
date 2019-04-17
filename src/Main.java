import java.io.*;
import java.net.*;

public class Main {
    private static int default_port = 80;
    private static String default_server_address = "icebluescion.ddns.net";

    public static void main(String[] args) throws IOException{
        String server_address = default_server_address;
        int port = default_port;
        Socket web_socket = new Socket(server_address, port);
        System.out.println("Creating connection from " +
                web_socket.getLocalAddress() + " to " +
                web_socket.getInetAddress() + " on port " +
                web_socket.getPort());

        int links = 0;

        //Data Output
        DataOutputStream out = new DataOutputStream(web_socket.getOutputStream());
        out.write("GET /index.html HTTP/1.0\r\n\r\n".getBytes());
        //String client_send = "GET /index.html HTTP/1.0\r\n\r\n";
        //out.writeUTF(client_send);
        System.out.println("Sending: " + out.toString());

        //Data Input
        InputStreamReader in = new InputStreamReader(web_socket.getInputStream());
        BufferedReader in_buffer = new BufferedReader(in);
        String client_received;

        while ((client_received = in_buffer.readLine()) != null) {
            System.out.println(client_received);
            if (client_received.contains("<a href=")) {
                links++;
            }
        }
        //String client_receive = in.readUTF();
        //System.out.println("Received: " + client_received);


        web_socket.close();
        System.out.println("Closing connection to " + web_socket.getInetAddress());
        System.out.println("Number of web links on page: " + links);
    }

}
