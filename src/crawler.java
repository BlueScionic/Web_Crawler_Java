/*COMP3310/6331 2019 - Assignment 2 – Crawling the Web
u5372418
*/
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class crawler {
    //Default Variables
    private static int default_port = 7880;
    private static String default_server_address = "comp3310.ddns.net";
    private static String default_page_address = "/";
    private static boolean verbose = false;

    //Global Variables
    private static SimpleDateFormat modified_date_format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
    private static ArrayList<String> siteLinks = new ArrayList<>();
    private static ArrayList<Page> sitePages = new ArrayList<>();

    public static void main(String[] args) throws IOException, ParseException {
        String server_address;
        int port;
        if (args.length == 0) {
            server_address = default_server_address;
            port = default_port;
            System.out.println("No parameters defined, using default settings.");
        } else {
            for (String i : args){
                if (i.equals("-v")) {
                    verbose = true;
                    break;
                }
            }
            server_address = args[0];
            port = Integer.parseInt(args[1]);
        }
        System.out.println("Crawling through " + server_address + "...");

        //Begin by crawling through the home page of the host
        Page homePage = getPage(server_address, port, default_page_address);
        homePage = getPageheaders(homePage);
        ArrayList<String> homePageLinks = getLinks(homePage);
        siteLinks.addAll(homePageLinks);
        sitePages.add(homePage);

        //Begin recursively going through each found page
        crawlLinks(server_address,port,homePageLinks);

        //Variables used for reporting results back
        int pagehtmlCount = 0;
        int pagenonhtmlCount = 0;
        int max = sitePages.get(0).pagelength;
        String maxName = sitePages.get(0).url;
        int min = sitePages.get(0).pagelength;
        String minName = sitePages.get(0).url;
        Date oldest = sitePages.get(0).modified;
        String oldestName = sitePages.get(0).url;
        Date newest = sitePages.get(0).modified;
        String newestName = sitePages.get(0).url;
        ArrayList<String> invalidPagesList = new ArrayList<>();
        ArrayList<String> redirectPagesList = new ArrayList<>();

        //Perform checks to determine what to report
        for (Page pageIterator : sitePages) {
            if (pageIterator.status.equals("404")) {
                invalidPagesList.add(pageIterator.domain + "/" + pageIterator.url);
            } else if (pageIterator.status.equals("200")) {
                if (pageIterator.isPagehtml() && !pageIterator.redirected) {
                    pagehtmlCount++;
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
                } else {
                    pagenonhtmlCount++;
                }
            } else if (pageIterator.redirected) {
                redirectPagesList.add(pageIterator.url + " --> " + pageIterator.location);
            } else {
                System.out.println("Page " + pageIterator.url + " returned status: " + pageIterator.status);
            }
        }
        siteLinks.remove("/");

        //Section reporting all the found informaiton
        // 1
        System.out.println("Number of distinct URLs on site: " + siteLinks.size());
        // 2
        System.out.println("Number of HTML pages: " + pagehtmlCount);
        System.out.println("Number of non-HTML objects: " + pagenonhtmlCount);
        // 3
        System.out.println("Smallest page: " + minName + " " + min + " bytes");
        System.out.println("Largest page: " + maxName + " " + max + " bytes");
        // 4
        System.out.println("Oldest modified page: " + oldestName + " at " + oldest);
        System.out.println("Newest modified page: " + newestName + " at " + newest);
        // 5
        System.out.println("List of invalid \"404 not found\" URLs: ");
        for (String invalidPagesIterator : invalidPagesList) {
            System.out.println(invalidPagesIterator);
        }
        // 6
        System.out.println("List of redirected 30x URLs: ");
        for (String redirectedPagesIterator : redirectPagesList) {
            System.out.println(redirectedPagesIterator);
        }

    }
    private static Page getPage(String server_address, int port, String page_address) throws IOException {
        //Limit usage of connecting to a page.
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Create page object
        Page page = new Page();
        page.domain = server_address;
        page.url = page_address;

        //Create Socket
        Socket web_socket = new Socket(server_address, port);
        if (verbose) {
            System.out.println("Creating connection from " +
                    web_socket.getLocalAddress() + " to " +
                    web_socket.getInetAddress() + " on port " +
                    web_socket.getPort() + " at page " +
                    page_address);
        }
        //Send Data Output
        DataOutputStream out = new DataOutputStream(web_socket.getOutputStream());
        String request = "GET /" + page_address + " HTTP/1.0\r\n\r\n";
        //String request = "GET /" + page_address + " HTTP/1.0\n\n";
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

        return page;
    }

    private static ArrayList<String> getLinks(Page page) {
        ArrayList<String> linkList = new ArrayList<>();

        //Regex patterns used to find links
        Pattern hrefPattern = Pattern.compile("href=\"(.*?)\"");
        Pattern srcPattern = Pattern.compile("src=\"(.*?)\"");
        Pattern urlPattern = Pattern.compile("^[^:/?#]+:?//([^/?#]*)?/([^?#]*)(\\?([^#]*))?(#(.*))?");
        Pattern pagePattern = Pattern.compile("^([^/?#]*)?/([^?#]*)(\\?([^#]*))?(#(.*))?");
        Matcher pageMatcher = pagePattern.matcher(page.url);

        //Get new link if a redirect
        if (page.redirected) {
            Matcher urlMatcher = urlPattern.matcher(page.location);
            if (urlMatcher.find()) {
                if (!linkList.contains(urlMatcher.group(2))) {
                    linkList.add(urlMatcher.group(2));
                }
            }
        }

        //Search through page for href and src links
        for (String linkIterator : page.content) {
            Matcher hrefMatcher = hrefPattern.matcher(linkIterator);
            Matcher srcMatcher = srcPattern.matcher(linkIterator);
            while (hrefMatcher.find()) {
                if (!(linkList.contains(hrefMatcher.group(1)))) {
                    linkList.add(hrefMatcher.group(1));
                }
            }
            while (srcMatcher.find()) {
                if (pageMatcher.find()) {
                    linkList.add(pageMatcher.group(1) + "/" + srcMatcher.group(1));
                } else if (!(linkList.contains(srcMatcher.group(1)))) {
                    linkList.add(srcMatcher.group(1));
                }
            }
        }
        return linkList;
    }

    private static void crawlLinks(String domain, int port, ArrayList<String> links) throws IOException, ParseException {
        ArrayList<String> crawlLinks = new ArrayList<>();
        ArrayList<Page> crawlPages = new ArrayList<>();


        //Crawl through rest of website
        for (String siteLinkIterator : links) {
            if (!siteLinkIterator.contains("http")) {
                Page currentPage = getPage(domain,port,siteLinkIterator);
                currentPage = getPageheaders(currentPage);
                ArrayList<String> pagelinks = getLinks(currentPage);
                crawlPages.add(currentPage);
                for (String linkIterator : pagelinks) {
                    if (!crawlLinks.contains(linkIterator)) {
                        crawlLinks.add(linkIterator);
                    }
                }
            }
        }
        crawlLinks.removeAll(siteLinks);

        //Add any new found links to global link list
        for (String crawlLinksIterator : crawlLinks) {
            if (!siteLinks.contains(crawlLinksIterator)) {
                siteLinks.add(crawlLinksIterator);
            }
        }
        for (Page crawlPageIterator : crawlPages) {
            if (!sitePages.contains(crawlPageIterator)) {
                sitePages.add(crawlPageIterator);
            }
        }

        //Continue to crawl if there are any links not yet crawled through
        if (crawlLinks.size() > 0) {
            crawlLinks(domain, port, crawlLinks);
        }
    }

    private static Page getPageheaders (Page page) throws ParseException {
        //ArrayList<String> metadata = new ArrayList<>();

        //Regex patterns used to collect metadata of the page
        Pattern statusPattern = Pattern.compile("HTTP.*? (\\d*?) ", Pattern.CASE_INSENSITIVE);
        Pattern typePattern = Pattern.compile("Content-Type: (\\S*)", Pattern.CASE_INSENSITIVE);
        Pattern lengthPattern = Pattern.compile("Content-Length: (\\d*)", Pattern.CASE_INSENSITIVE);
        Pattern modifiedPattern = Pattern.compile("Last-Modified: (.*)", Pattern.CASE_INSENSITIVE);
        Pattern locationPattern = Pattern.compile("Location: (.*)", Pattern.CASE_INSENSITIVE);

        //Search for the header information
        for (String iterator : page.content) {
            Matcher statusMatcher = statusPattern.matcher(iterator);
            Matcher typeMatcher = typePattern.matcher(iterator);
            Matcher lengthMatcher = lengthPattern.matcher(iterator);
            Matcher modifiedMatcher = modifiedPattern.matcher(iterator);
            Matcher locationMatcher = locationPattern.matcher(iterator);

            if (statusMatcher.find()) {
                page.status = statusMatcher.group(1);
            } else if (typeMatcher.find()) {
                page.type = typeMatcher.group(1);
            } else if (lengthMatcher.find()) {
                page.pagelength = Integer.parseInt(lengthMatcher.group(1));
            } else if (modifiedMatcher.find()) {
                page.modified = modified_date_format.parse(modifiedMatcher.group(1));
            } else if (locationMatcher.find()) {
                page.location = locationMatcher.group(1);
            }
        }
        if (page.isRedirect()) {
            page.redirected = true;
        }
        return page;
    }
}

