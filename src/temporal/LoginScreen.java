package temporal;

import javax.microedition.lcdui.*;

public class LoginScreen extends Form implements CommandListener {
    private TextField usernameField;
    private TextField passwordField;
    private Command actionCommand;
    private Command toggleCommand;
    private Command exitCommand;
    private Temporal midlet;
    private boolean isLoginMode = true;

    public LoginScreen(Temporal midlet) {
        super("login");
        this.midlet = midlet;

        usernameField = new TextField("username", "", 20, TextField.ANY);
        passwordField = new TextField("password", "", 20, TextField.PASSWORD);

        append(usernameField);
        append(passwordField);

        actionCommand = new Command("login", Command.OK, 1);
        toggleCommand = new Command("register", Command.ITEM, 3);
        exitCommand = new Command("exit", Command.EXIT, 2);

        addCommand(actionCommand);
        addCommand(toggleCommand);
        addCommand(exitCommand);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == actionCommand) {
            final String user = usernameField.getString();
            final String pass = passwordField.getString();
            if (user.length() == 0 || pass.length() == 0) {
                midlet.showAlert("error", "no username and password");
                return;
            }

            String json = Service.buildJson(new String[] { "username", "password" }, new String[] { user, pass });

            if (isLoginMode) {
                Service.post("/create/token", json, null, new Service.Callback() {
                    public void onSuccess(final String response) {
                        RMSStore.saveJWT(response);
                        RMSStore.saveUsername(user);
                        midlet.getDisplay().callSerially(new Runnable() {
                            public void run() {
                                midlet.showHome();
                            }
                        });
                    }

                    public void onError(final String error) {
                        midlet.getDisplay().callSerially(new Runnable() {
                            public void run() {
                                midlet.showAlert("login failed", error);
                            }
                        });
                    }
                });
            } else {
                Service.post("/create/account", json, null, new Service.Callback() {
                    public void onSuccess(final String response) {
                        midlet.getDisplay().callSerially(new Runnable() {
                            public void run() {
                                midlet.showAlert("success", "account created");
                                toggleMode();
                            }
                        });
                    }

                    public void onError(final String error) {
                        midlet.getDisplay().callSerially(new Runnable() {
                            public void run() {
                                midlet.showAlert("registration failed", error);
                            }
                        });
                    }
                });
            }
        } else if (c == toggleCommand) {
            toggleMode();
        } else if (c == exitCommand) {
            midlet.exitMIDlet();
        }
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        setTitle(isLoginMode ? "login" : "register");
        removeCommand(actionCommand);
        removeCommand(toggleCommand);

        actionCommand = new Command(isLoginMode ? "login" : "register", Command.OK, 1);
        toggleCommand = new Command(isLoginMode ? "register" : "login", Command.ITEM, 3);

        addCommand(actionCommand);
        addCommand(toggleCommand);
    }
}
