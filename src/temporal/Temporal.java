package temporal;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

public class Temporal extends MIDlet {
    private Display display;
    private LoginScreen loginScreen;
    private HomeScreen homeScreen;

    public void startApp() {
        if (display == null) {
            display = Display.getDisplay(this);
            String jwt = RMSStore.getJWT();
            if (jwt != null && jwt.length() > 0) {
                showHome();
            } else {
                showLogin();
            }
        }
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }

    public Display getDisplay() {
        return display;
    }

    public void showLogin() {
        if (loginScreen == null) {
            loginScreen = new LoginScreen(this);
        }
        display.setCurrent(loginScreen);
    }

    public void showHome() {
        if (homeScreen == null) {
            homeScreen = new HomeScreen(this);
        }
        homeScreen.loadPosts();
        display.setCurrent(homeScreen);
    }

    public void showPost(String id, String content, String author) {
        PostScreen postScreen = new PostScreen(this, id, content, author);
        display.setCurrent(postScreen);
    }

    public void showCreatePost() {
        CreatePostScreen createPostScreen = new CreatePostScreen(this);
        display.setCurrent(createPostScreen);
    }

    public void showAlert(String title, String msg) {
        Alert alert = new Alert(title, msg, null, AlertType.INFO);
        alert.setTimeout(Alert.FOREVER);
        display.setCurrent(alert, display.getCurrent());
    }

    public void exitMIDlet() {
        destroyApp(true);
        notifyDestroyed();
    }
}
