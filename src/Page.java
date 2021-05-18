import java.util.ArrayList;
import java.util.Date;

//Page Object
class Page {
    String domain, url, status, type, location;
    int pagelength;
    Date modified;
    ArrayList<String> content;
    Boolean redirected;

    //Checks if page returns as an html type
    public boolean isPagehtml() {
        return type.contains("html");
    }

    //Checks if page is a redirect page
    public boolean isRedirect() {
        String redirects[] = {"300", "301", "302", "303", "304", "305", "306", "307", "308"};
        for (String code : redirects) {
            if (status.equals(code)) {
                return true;
            }
        }
        return false;
    }

    public Page() {
        this.url = "";
        this.status = "";
        this.type = "";
        this.pagelength = 0;
        this.modified = new Date();
        this.domain = "";
        this.content = new ArrayList<>();
        this.location = "";
        this.redirected = false;
    }
}
