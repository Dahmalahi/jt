package temporal;

import javax.microedition.lcdui.*;

public class CreatePostScreen extends TextBox implements CommandListener {
    private Temporal midlet;
    private Command submitCommand;
    private Command cancelCommand;

    public CreatePostScreen(Temporal midlet) {
        super("what do you want to share?", "", 10000, TextField.ANY);
        this.midlet = midlet;

        submitCommand = new Command("post", Command.OK, 1);
        cancelCommand = new Command("cancel", Command.CANCEL, 2);

        addCommand(submitCommand);
        addCommand(cancelCommand);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == submitCommand) {
            String content = getString();
            if (content.length() == 0) {
                midlet.showAlert("error", "post cannot be empty");
                return;
            }

            String json = Service.buildJson(new String[] { "content" }, new String[] { content });
            String jwt = RMSStore.getJWT();

            Service.post("/create/post", json, jwt, new Service.Callback() {
                public void onSuccess(String response) {
                    midlet.getDisplay().callSerially(new Runnable() {
                        public void run() {
                            midlet.showHome();
                        }
                    });
                }

                public void onError(final String error) {
                    midlet.getDisplay().callSerially(new Runnable() {
                        public void run() {
                            midlet.showAlert("failed to post", error);
                        }
                    });
                }
            });
        } else if (c == cancelCommand) {
            midlet.showHome();
        }
    }
}
