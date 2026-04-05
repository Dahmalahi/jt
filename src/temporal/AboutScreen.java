package temporal;

import javax.microedition.lcdui.*;

/**
 * About screen — app info, credits, and links.
 * v1.1 new screen.
 */
public class AboutScreen extends Form implements CommandListener {

    private Temporal midlet;
    private Command backCommand;

    public AboutScreen(Temporal midlet) {
        super("about temporal");
        this.midlet = midlet;

        // App info
        append(makeSection("temporal v1.1",
                "a micro-blogging client for J2ME devices.\n" +
                "runs on CLDC 1.1 / MIDP 2.0.\n"));

        // Developer credit
        append(makeSection("developer",
                "created by aditya\n"));

        // Dash Animation V2 channel credit
        append(makeSection("dash animation v2",
                "check out the channel for animations,\n" +
                "projects, and more!\n\n" +
                "youtube.com/@dash______animationv2\n"));

        // API backend
        append(makeSection("backend",
                "temporal.dedomil.workers.dev\n"));

        // Build info
        append(makeSection("build",
                "v1.1  |  j2me / cldc 1.1\n" +
                "open source — hack freely.\n"));

        backCommand = new Command("back", Command.BACK, 1);
        addCommand(backCommand);
        setCommandListener(this);
    }

    private StringItem makeSection(String label, String text) {
        return new StringItem(label, text);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            midlet.showHome();
        }
    }
}
