package temporal;

import javax.microedition.lcdui.*;

public class PostScreen extends Form implements CommandListener {
    private Temporal midlet;
    private Command backCommand;
    private StringItem contentItem;

    public PostScreen(Temporal midlet, String id, String content, String author) {
        super("&" + author + "#" + id);
        this.midlet = midlet;

        append(unescape(content));

        backCommand = new Command("back", Command.BACK, 1);
        addCommand(backCommand);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            midlet.showHome();
        }
    }

    private String unescape(String s) {
        if (s == null)
            return "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\\' && i + 1 < s.length() && s.charAt(i + 1) == 'n') {
                sb.append('\n');
                i++;
            } else if (s.charAt(i) == '\\' && i + 1 < s.length() && s.charAt(i + 1) == 'r') {
                sb.append('\r');
                i++;
            } else {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }
}
