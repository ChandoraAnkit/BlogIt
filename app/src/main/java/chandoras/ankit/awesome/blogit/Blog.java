package chandoras.ankit.awesome.blogit;

/**
 * Created by chandoras on 10/1/17.
 */

public class Blog {
    private String title, descp, image, username;

    public Blog() {

    }

    public Blog(String title, String descp, String image, String username) {

        this.title = title;
        this.descp = descp;
        this.image = image;
        this.username = username;

    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescp() {
        return descp;
    }

    public void setDescp(String descp) {
        this.descp = descp;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
