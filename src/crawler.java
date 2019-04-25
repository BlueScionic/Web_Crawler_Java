import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class crawler {
    private static int default_port = 80;
    private static String default_server_address = "icebluescion.ddns.net";
    private static String default_page_address = "/";
    //private static String index_page_address = "index.html";
    private static SimpleDateFormat modified_date_format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);

    public static void main(String[] args) throws IOException, ParseException {
        String server_address;
        int port;
        if (args.length == 0) {
            server_address = default_server_address;
            port = default_port;
            //System.out.println("No parameters defined, using default settings.");
        } else {
            server_address = args[0];
            port = Integer.parseInt(args[1]);
        }
        System.out.println("Crawling through " + server_address + "...");

        Page homePage = getPage(server_address, port, default_page_address);
        ArrayList<String> homePageLinks = getLinks(homePage);
        homePage = getPageheaders(homePage);

        ArrayList<String> siteLinks = new ArrayList<>();
        siteLinks.addAll(homePageLinks);
        ArrayList<Page> pageList = new ArrayList<>();
        pageList.add(homePage);

        //Crawl through rest of website
        for (String pageIterator : homePageLinks) {
            if (!pageIterator.contains("http")) {
                Page secondPages = getPage(server_address, port, pageIterator);
                ArrayList<String> links = getLinks(secondPages);
                secondPages = getPageheaders(secondPages);
                pageList.add(secondPages);
                for (String linkIterator : links) {
                    if (!siteLinks.contains(linkIterator)) {
                        siteLinks.add(linkIterator);
                    }
                }
            }
        }
        ArrayList<String> siteLinksIterator = new ArrayList<>();
        siteLinksIterator.addAll(siteLinks);
        for (String pageIterator : siteLinksIterator) {
            if (!pageIterator.contains("http")) {
                Page thirdPages = getPage(server_address, port, pageIterator);
                ArrayList<String> links = getLinks(thirdPages);
                thirdPages = getPageheaders(thirdPages);
                pageList.add(thirdPages);
                for (String linkIterator : links) {
                    if (!siteLinks.contains(linkIterator)) {
                        siteLinks.add(linkIterator);
                    }
                }
            }
        }
        //siteLinks.remove("/");


        int pageCount = 0;
        int errorCount = 0;
        int max = pageList.get(0).pagelength;
        String maxName = pageList.get(0).url;
        int min = pageList.get(0).pagelength;
        String minName = pageList.get(0).url;
        Date oldest = pageList.get(0).modified;
        String oldestName = pageList.get(0).url;
        Date newest = pageList.get(0).modified;
        String newestName = pageList.get(0).url;
        for (Page pageIterator : pageList) {
            if (pageIterator.status.equals("404")) {
                System.out.println("Page not found at: " + pageIterator.domain + "/" + pageIterator.url);
                errorCount++;
            } else {
                if (pageIterator.isPagehtml()) {
                    pageCount++;
                }
                if (pageIterator.pagelength > max) {
                    max = pageIterator.pagelength;
                    maxName = pageIterator.url;
                }
                if (pageIterator.pagelength < min) {
                    min = pageIterator.pagelength;
                    minName = pageIterator.url;
                }
                if (pageIterator.modified.before(oldest)) {
                    oldest = pageIterator.modified;
                    oldestName = pageIterator.url;
                }
                if (pageIterator.modified.after(newest)) {
                    newest = pageIterator.modified;
                    newestName = pageIterator.url;
                }
            }
        }

/*        Iterator i = siteLinks.iterator();
        System.out.println("The ArrayList elements are:");
        while (i.hasNext()) {
            System.out.println(i.next());
        }*/

        // 1
        System.out.println("Number of distinct URLs on site: " + siteLinks.size());
        // 2
        System.out.println("Number of HTML pages: " + pageCount);
        // 3
        System.out.println("Smallest page: " + minName + " " + min + " bytes");
        System.out.println("Largest page: " + maxName + " " + max + " bytes");
        // 4
        System.out.println("Oldest modified page: " + oldestName + " at " + oldest);
        System.out.println("Newest modified page: " + newestName + " at " + newest);
        // 5
        System.out.println("Number of invalid \"404 not found\" URLs: " + errorCount);
        // 6

    }
    private static Page getPage(String server_address, int port, String page_address) throws IOException {
        //Limit usage of connecting to a page.
/*        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        //Create page object
        Page page = new Page();
        page.domain = server_address;
        page.url = page_address;

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
            page.content.add(client_received);
        }
        //Close Connection
        web_socket.close();
/*        if (web_socket.isClosed()) {
            System.out.println("Socket Closed");
        } else {
            System.out.println("Socket still open");
        }*/

        return page;
    }

    private static ArrayList<String> getLinks(Page page) {
        ArrayList<String> linkList = new ArrayList<>();

        //Search through page for links
        for (String linkIterator : page.content) {
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

    private static Page getPageheaders (Page page) throws ParseException {
        //ArrayList<String> metadata = new ArrayList<>();

        //Collect metadata of the page
        Pattern statusPattern = Pattern.compile("HTTP.*? (\\d*?) ", Pattern.CASE_INSENSITIVE);
        Pattern typePattern = Pattern.compile("Content-Type: (\\S*)", Pattern.CASE_INSENSITIVE);
        Pattern lengthPattern = Pattern.compile("Content-Length: (\\d*)", Pattern.CASE_INSENSITIVE);
        Pattern modifiedPattern = Pattern.compile("Last-Modified: (.*)", Pattern.CASE_INSENSITIVE);



        //SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        //Date d = format.parse(dateString);

        for (String iterator : page.content) {
            Matcher statusMatcher = statusPattern.matcher(iterator);
            Matcher typeMatcher = typePattern.matcher(iterator);
            Matcher lengthMatcher = lengthPattern.matcher(iterator);
            Matcher modifiedMatcher = modifiedPattern.matcher(iterator);

            if (statusMatcher.find()) {
                page.status = statusMatcher.group(1);
            } else if (typeMatcher.find()) {
                page.type = typeMatcher.group(1);
            } else if (lengthMatcher.find()) {
                int length = Integer.parseInt(lengthMatcher.group(1));
                page.pagelength = length;
            } else if (modifiedMatcher.find()) {
                page.modified = modified_date_format.parse(modifiedMatcher.group(1));
            }
        }
        return page;
    }
}

class Page {
    String domain, url, status, type;
    int pagelength;
    Date modified;
    //modified;
    ArrayList<String> content;

    public boolean isPagehtml() {
        if (type.contains("html")) {
            return true;
        } else {
            return false;
        }
    }

    public Page() {
        this.url = "";
        this.status = "";
        this.type = "";
        this.pagelength = 0;
        this.modified = new Date();
        this.domain = "";
        this.content = new ArrayList<>();
    }
}