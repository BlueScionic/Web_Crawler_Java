import javax.print.DocFlavor;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static int default_port = 80;
    private static String default_server_address = "icebluescion.ddns.net";
    private static String home_page_address = "";
    private static String index_page_address = "index.html";

    public static void main(String[] args) throws IOException {
        String server_address = "";
        int port = 0;
        if (args.length == 0) {
            server_address = default_server_address;
            port = default_port;
        } else {
            server_address = args[0];
            port = Integer.parseInt(args[1]);
        }

        //ArrayList pageInfo = getPageInfo(server_address, port);

        ArrayList<String> homePage = getPage(server_address, port, index_page_address);
        ArrayList<String> homePageLinks = getLinks(homePage);
        //ArrayList<String> allLinks = getLinks(homePage);
        ArrayList<String> pageLinks = new ArrayList<>();
        pageLinks.addAll(homePageLinks);

        for (Object pageIterator : homePageLinks) {
            if (!pageIterator.toString().contains("http")) {
                String page_address = pageIterator.toString();
                ArrayList<String> secondPages = getPage(server_address, port, page_address);
                ArrayList<String> links = getLinks(secondPages);
                for (Object linkIterator : links) {
                    if (!pageLinks.contains(linkIterator)) {
                        pageLinks.add(linkIterator.toString());
                    }
                }
            }
        }
        pageLinks.remove("/");
        Iterator i = pageLinks.iterator();
        System.out.println("The ArrayList elements are:");
        while (i.hasNext()) {
            System.out.println(i.next());
        }
        System.out.println("Number of web links on page: " + pageLinks.size());
    }
    private static ArrayList<String> getPage(String server_address, int port, String page_address) throws IOException {
/*        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        ArrayList<String> page = new ArrayList<>();

        //Create Socket
        Socket web_socket = new Socket(server_address, port);
        System.out.println("Creating connection from " +
                web_socket.getLocalAddress() + " to " +
                web_socket.getInetAddress() + " on port " +
                web_socket.getPort() + " at page " +
                page_address);

        //Send Data Output
        DataOutputStream out = new DataOutputStream(web_socket.getOutputStream());
        String request = "GET /" + page_address + " HTTP/1.0\r\n\r\n";
        out.write(request.getBytes());

        //Receive Data Input
        InputStreamReader in = new InputStreamReader(web_socket.getInputStream());
        BufferedReader in_buffer = new BufferedReader(in);
        String client_received;

        //Save Page
        while ((client_received = in_buffer.readLine()) != null) {
            page.add(client_received);
        }

        //End
        web_socket.close();
        if (web_socket.isClosed()) {
            System.out.println("Socket Closed");
        } else {
            System.out.println("Socket still open");
        }
        return page;
    }

    private static ArrayList<String> getLinks(ArrayList<String> page) {
        ArrayList<String> linkList = new ArrayList<>();

        //Search through page for links
        for (Object linkIterator : page) {
            String linkString = linkIterator.toString();
            Pattern linkPattern = Pattern.compile("<a href=\"(.*?)\"");
            Matcher linkMatcher = linkPattern.matcher(linkString);
            while (linkMatcher.find()) {
                if (!(linkList.contains(linkMatcher.group(1)))) {
                    linkList.add(linkMatcher.group(1));
                }
            }
        }
        return linkList;
    }
}
