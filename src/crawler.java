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

public class crawler {
    private static int default_port = 80;
    private static String default_server_address = "icebluescion.ddns.net";
    private static String home_page_address = "";
    private static String index_page_address = "index.html";

    public static void main(String[] args) throws IOException {
        String server_address;
        int port;
        if (args.length == 0) {
            server_address = default_server_address;
            port = default_port;
            //System.out.println("Using default settings");
        } else {
            server_address = args[0];
            port = Integer.parseInt(args[1]);
        }

        int pageCount = 0;
        int errorCount = 0;

        ArrayList<String> homePage = getPage(server_address, port, index_page_address);
        ArrayList<String> homePageLinks = getLinks(homePage);
        ArrayList<String> homePageMeta = getPageMeta(homePage);
        ArrayList<String> pageLinks = new ArrayList<>();
        pageLinks.addAll(homePageLinks);
        if (isPagehtml(homePage)) {
            pageCount++;
        }
        if (homePageMeta.get(0).equals("404")) {
            System.out.println("404 Page Not Found at : " + server_address);
            errorCount++;
        }




        //Crawl through rest of website
        for (String pageIterator : homePageLinks) {
            if (!pageIterator.contains("http")) {
                ArrayList<String> secondPages = getPage(server_address, port, pageIterator);
                ArrayList<String> links = getLinks(secondPages);
                ArrayList<String> secondPagesMeta = getPageMeta(secondPages);
                if (isPagehtml(secondPages)) {
                    pageCount++;
                }
                if (secondPagesMeta.get(0).equals("404")) {
                    //System.out.println("Error Page found: " + server_address + "/" + pageIterator);
                    errorCount++;
                }
                for (String linkIterator : links) {
                    if (!pageLinks.contains(linkIterator)) {
                        pageLinks.add(linkIterator);
                    }
                }
            }
        }
        pageLinks.remove("/");
/*        Iterator i = pageLinks.iterator();
        System.out.println("The ArrayList elements are:");
        while (i.hasNext()) {
            System.out.println(i.next());
        }*/
        System.out.println("Number of not found pages: " + errorCount);
        System.out.println("Number of distinct URLs on site: " + pageLinks.size());
        System.out.println("Number of HTML pages: " + pageCount);
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
/*        System.out.println("Creating connection from " +
                web_socket.getLocalAddress() + " to " +
                web_socket.getInetAddress() + " on port " +
                web_socket.getPort() + " at page " +
                page_address);*/

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
/*        if (web_socket.isClosed()) {
            System.out.println("Socket Closed");
        } else {
            System.out.println("Socket still open");
        }*/

        return page;
    }

    private static ArrayList<String> getLinks(ArrayList<String> page) {
        ArrayList<String> linkList = new ArrayList<>();

        //Search through page for links
        for (String linkIterator : page) {
            Pattern linkPattern = Pattern.compile("<a href=\"(.*?)\"");
            Matcher linkMatcher = linkPattern.matcher(linkIterator);
            while (linkMatcher.find()) {
                if (!(linkList.contains(linkMatcher.group(1)))) {
                    linkList.add(linkMatcher.group(1));
                }
            }
        }
        return linkList;
    }

    private static ArrayList<String> getPageMeta (ArrayList<String> page) {
        ArrayList<String> metadata = new ArrayList<>();

        //Collect metadata of the page
        for (String iterator : page) {
            Pattern statusPattern = Pattern.compile("HTTP.*? (\\d*?) ");
            Matcher statusMatcher = statusPattern.matcher(iterator);
            if (statusMatcher.find()) {
                metadata.add(0, statusMatcher.group(1));
            }
            Pattern sizePattern = Pattern.compile("Content-Length: (\\d*)");
            Matcher sizeMatcher = sizePattern.matcher(iterator);
            if (sizeMatcher.find()) {
                metadata.add(1, sizeMatcher.group(1));
            }
        }
        return metadata;
    }

    private static boolean isPagehtml(ArrayList<String> page) {
        for (Object linkIterator : page) {
            if (linkIterator.toString().contains("Content-Type: text/html")) {
                return true;
            }
        }
        return false;
    }
}
