package temporal;

import java.util.Vector;
import javax.microedition.lcdui.*;

public class HomeScreen extends List implements CommandListener {
    private Temporal midlet;
    private Command newPostCommand;
    private Command refreshCommand;
    private Command logoutCommand;
    private Vector posts;

    public HomeScreen(Temporal midlet) {
        super("temporal", List.IMPLICIT);
        this.midlet = midlet;

        newPostCommand = new Command("new post", Command.ITEM, 1);
        refreshCommand = new Command("refresh", Command.ITEM, 2);
        logoutCommand = new Command("logout", Command.EXIT, 3);

        addCommand(newPostCommand);
        addCommand(refreshCommand);
        addCommand(logoutCommand);
        setCommandListener(this);
    }

    public void loadPosts() {
        deleteAll();
        append("loading...", null);
        String jwt = RMSStore.getJWT();
        String userStr = RMSStore.getUsername();
        String endpoint = (userStr != null && userStr.length() > 0) ? "/u/" + userStr : "/posts";

        Service.get(endpoint, jwt, new Service.Callback() {
            public void onSuccess(String response) {
                posts = JSON.parseArray(response, new String[] { "id", "content", "author" });
                midlet.getDisplay().callSerially(new Runnable() {
                    public void run() {
                        deleteAll();
                        if (posts.size() == 0) {
                            append("no posts found", null);
                        } else {
                            for (int i = 0; i < posts.size(); i++) {
                                String[] post = (String[]) posts.elementAt(i);
                                String content = post[1];
                                if (content.length() > 20) {
                                    content = content.substring(0, 20) + "...";
                                }
                                append(post[2] + ": " + content, null);
                            }
                        }
                    }
                });
            }

            public void onError(final String error) {
                midlet.getDisplay().callSerially(new Runnable() {
                    public void run() {
                        deleteAll();
                        append("error: api error", null);
                    }
                });
            }
        });
    }

    public void commandAction(Command c, Displayable d) {
        if (c == List.SELECT_COMMAND) {
            int idx = getSelectedIndex();
            if (posts != null && idx >= 0 && idx < posts.size()) {
                String[] post = (String[]) posts.elementAt(idx);
                midlet.showPost(idx + "", post[1], post[2]);
            }
        } else if (c == newPostCommand) {
            midlet.showCreatePost();
        } else if (c == refreshCommand) {
            loadPosts();
        } else if (c == logoutCommand) {
            RMSStore.clearAll();
            midlet.showLogin();
        }
    }
}
