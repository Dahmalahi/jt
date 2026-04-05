package temporal;

import javax.microedition.lcdui.*;

/**
 * CreatePostScreen v1.3 – Custom Canvas composer with organic UI.
 *
 * SMOOTH ROUNDED AESTHETIC:
 *  - Gradient header with character counter pill
 *  - Multi-line text area with rounded bubble frame
 *  - Animated character counter (color shifts on threshold)
 *  - Soft keyboard hints at bottom
 *  - Breathing "post" button when ready
 *  - Draft auto-save on cancel
 *  - T9-style multi-tap input OR full QWERTY (device-dependent)
 *  - Smooth cursor blink animation
 */
public class CreatePostScreen extends Canvas implements CommandListener {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private static final int MAX_CHARS = 500;
    private static final int MIN_CHARS = 2;
    private static final int WARN_THRESHOLD = 400; // 80%

    private static final int C_BG_TOP     = 0xE3F2FD;
    private static final int C_BG_BOT     = 0xBBDEFB;
    private static final int C_HEADER_TOP = 0x1976D2;
    private static final int C_HEADER_BOT = 0x42A5F5;
    private static final int C_WHITE      = 0xFFFFFF;
    private static final int C_BLUE_PALE  = 0xBBDEFB;
    private static final int C_BLUE_MID   = 0x1E88E5;
    private static final int C_BLUE_DARK  = 0x0D47A1;
    private static final int C_GRAY       = 0x757575;
    private static final int C_GRAY_LIGHT = 0xBDBDBD;
    private static final int C_DARK       = 0x212121;
    private static final int C_GREEN      = 0x66BB6A;
    private static final int C_ORANGE     = 0xFF8800;
    private static final int C_RED        = 0xEF5350;

    private static final int HEADER_HEIGHT = 50;
    private static final int FOOTER_HEIGHT = 44;

    // T9 keymap
    private static final String[] KEY_MAP = {
        " 0", ".,!?'\"1-()@/:_", "abc2", "def3", "ghi4",
        "jkl5", "mno6", "pqrs7", "tuv8", "wxyz9"
    };

    // -----------------------------------------------------------------------
    // Commands
    // -----------------------------------------------------------------------

    private final Command postCommand;
    private final Command cancelCommand;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final Temporal midlet;
    private String inputText = "";
    private volatile boolean submitting = false;

    private Font font, headerFont, smallFont, tinyFont;
    private int fontHeight;

    // T9 state
    private int lastKeyIndex = -1;
    private int tapCount = 0;
    private long lastKeyTime = 0;
    private static final long TAP_TIMEOUT = 900;
    private boolean capsOn = false;

    // Cursor blink
    private boolean cursorVisible = true;
    private long lastBlinkTime = 0;
    private static final long BLINK_INTERVAL = 500;

    // Button pulse
    private long animStartTime = 0;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public CreatePostScreen(Temporal midlet, String prefill) {
        this.midlet = midlet;

        font       = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        headerFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
        smallFont  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        tinyFont   = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        fontHeight = font.getHeight();

        // Restore draft or use prefill
        String initial = (prefill != null && prefill.length() > 0) ? prefill : "";
        if (initial.length() == 0) {
            String draft = RMSStore.getDraft();
            if (draft != null && draft.length() > 0) {
                initial = draft;
            }
        }
        if (initial.length() > MAX_CHARS) {
            initial = initial.substring(0, MAX_CHARS);
        }
        inputText = initial;

        postCommand   = new Command("Post",   Command.OK,     1);
        cancelCommand = new Command("Cancel", Command.CANCEL, 2);

        addCommand(postCommand);
        addCommand(cancelCommand);
        setCommandListener(this);

        setFullScreenMode(true);
        animStartTime = System.currentTimeMillis();
        startAnimationThread();
    }

    // -----------------------------------------------------------------------
    // Paint
    // -----------------------------------------------------------------------

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        // Gradient background
        for (int y = 0; y < h; y++) {
            int ratio = (y * 255) / h;
            int r = ((C_BG_TOP >> 16) & 0xFF) * (255 - ratio) / 255 +
                    ((C_BG_BOT >> 16) & 0xFF) * ratio / 255;
            int gv = ((C_BG_TOP >> 8) & 0xFF) * (255 - ratio) / 255 +
                     ((C_BG_BOT >> 8) & 0xFF) * ratio / 255;
            int b = (C_BG_TOP & 0xFF) * (255 - ratio) / 255 +
                    (C_BG_BOT & 0xFF) * ratio / 255;
            g.setColor((r << 16) | (gv << 8) | b);
            g.drawLine(0, y, w, y);
        }

        drawHeader(g, w);
        drawTextArea(g, w, h);
        drawFooter(g, w, h);
    }

    private void drawHeader(Graphics g, int w) {
        // Gradient header
        for (int y = 0; y < HEADER_HEIGHT; y++) {
            int ratio = (y * 255) / HEADER_HEIGHT;
            int r = ((C_HEADER_TOP >> 16) & 0xFF) * (255 - ratio) / 255 +
                    ((C_HEADER_BOT >> 16) & 0xFF) * ratio / 255;
            int gv = ((C_HEADER_TOP >> 8) & 0xFF) * (255 - ratio) / 255 +
                     ((C_HEADER_BOT >> 8) & 0xFF) * ratio / 255;
            int b = (C_HEADER_TOP & 0xFF) * (255 - ratio) / 255 +
                    (C_HEADER_BOT & 0xFF) * ratio / 255;
            g.setColor((r << 16) | (gv << 8) | b);
            g.drawLine(0, y, w, y);
        }

        // Curved bottom
        g.setColor(C_HEADER_BOT);
        g.fillArc(0, HEADER_HEIGHT - 10, 20, 20, 90, 90);
        g.fillArc(w - 20, HEADER_HEIGHT - 10, 20, 20, 0, 90);

        // Title
        g.setFont(headerFont);
        g.setColor(C_WHITE);
        g.drawString(submitting ? "posting..." : "compose", 10, 8,
                Graphics.TOP | Graphics.LEFT);

        // Character counter pill
        int count = inputText.length();
        String counterText = count + "/" + MAX_CHARS;
        int pillW = smallFont.stringWidth(counterText) + 20;
        int pillX = w - pillW - 10;
        int pillY = HEADER_HEIGHT - 20;

        // Determine color
        int pillColor;
        if (count >= MAX_CHARS) {
            pillColor = C_RED;
        } else if (count >= WARN_THRESHOLD) {
            pillColor = C_ORANGE;
        } else {
            pillColor = C_GREEN;
        }

        // Pill shadow
        g.setColor(C_BLUE_DARK);
        g.fillRoundRect(pillX + 1, pillY + 1, pillW, 16, 8, 8);
        // Pill body
        g.setColor(pillColor);
        g.fillRoundRect(pillX, pillY, pillW, 16, 8, 8);

        g.setFont(smallFont);
        g.setColor(C_WHITE);
        g.drawString(counterText, pillX + pillW / 2, pillY + 2,
                Graphics.TOP | Graphics.HCENTER);
    }

    private void drawTextArea(Graphics g, int w, int h) {
        int top = HEADER_HEIGHT + 10;
        int bot = h - FOOTER_HEIGHT - 10;
        int areaH = bot - top;

        int margin = 10;
        int areaX = margin;
        int areaW = w - margin * 2;
        int areaY = top;

        // Text bubble shadow
        g.setColor(0xD0D0D0);
        g.fillRoundRect(areaX + 2, areaY + 2, areaW, areaH, 16, 16);

        // Text bubble body
        g.setColor(C_WHITE);
        g.fillRoundRect(areaX, areaY, areaW, areaH, 16, 16);

        // Border
        g.setColor(C_BLUE_PALE);
        g.drawRoundRect(areaX, areaY, areaW, areaH, 16, 16);

        // Text content
        g.setFont(font);
        g.setColor(C_DARK);
        g.setClip(areaX + 8, areaY + 8, areaW - 16, areaH - 16);

        if (inputText.length() == 0) {
            g.setColor(C_GRAY_LIGHT);
            g.drawString("What's happening?", areaX + 12, areaY + 12,
                    Graphics.TOP | Graphics.LEFT);
        } else {
            drawWrappedText(g, inputText, areaX + 12, areaY + 12, areaW - 24);
        }

        // Cursor
        if (cursorVisible && !submitting) {
            int cursorX = areaX + 12 + font.stringWidth(getLastLineText());
            int cursorY = areaY + 12 + (getLineCount() - 1) * (fontHeight + 2);
            g.setColor(C_BLUE_MID);
            g.fillRect(cursorX, cursorY, 2, fontHeight);
        }

        g.setClip(0, 0, w, h);
    }

    private void drawFooter(Graphics g, int w, int h) {
        int y = h - FOOTER_HEIGHT;

        // Curved top
        g.setColor(C_WHITE);
        g.fillArc(0, y - 10, 20, 20, 180, 90);
        g.fillArc(w - 20, y - 10, 20, 20, 270, 90);
        g.fillRect(10, y, w - 20, FOOTER_HEIGHT);

        // Post button (breathing animation)
        int buttonW = 80;
        int buttonH = 32;
        int buttonX = w / 2 - buttonW / 2;
        int buttonY = y + 6;

        boolean canPost = inputText.trim().length() >= MIN_CHARS &&
                          inputText.length() <= MAX_CHARS;

        if (canPost && !submitting) {
            // Breathing effect
            long elapsed = System.currentTimeMillis() - animStartTime;
            int pulse = (int) ((Math.sin(elapsed / 400.0) + 1) * 2); // 0-4
            buttonW += pulse;
            buttonH += pulse / 2;
            buttonX = w / 2 - buttonW / 2;
            buttonY = y + 6 - pulse / 4;
        }

        // Button shadow
        g.setColor(C_BLUE_DARK);
        g.fillRoundRect(buttonX + 2, buttonY + 2, buttonW, buttonH, 16, 16);

        // Button body
        if (submitting) {
            g.setColor(C_GRAY);
        } else if (canPost) {
            g.setColor(C_GREEN);
        } else {
            g.setColor(C_GRAY_LIGHT);
        }
        g.fillRoundRect(buttonX, buttonY, buttonW, buttonH, 16, 16);

        // Button text
        g.setFont(smallFont);
        g.setColor(C_WHITE);
        g.drawString(submitting ? "..." : "Post", buttonX + buttonW / 2,
                buttonY + buttonH / 2 - smallFont.getHeight() / 2,
                Graphics.TOP | Graphics.HCENTER);

        // Keyboard hint
        if (!submitting) {
            g.setFont(tinyFont);
            g.setColor(C_GRAY);
            String hint = capsOn ? "ABC" : "abc";
            g.drawString(hint, 6, y + 4, Graphics.TOP | Graphics.LEFT);
            g.drawString("# = caps", 6, y + 4 + tinyFont.getHeight() + 2,
                    Graphics.TOP | Graphics.LEFT);
        }
    }

    // -----------------------------------------------------------------------
    // Text rendering helpers
    // -----------------------------------------------------------------------

    private void drawWrappedText(Graphics g, String text, int x, int y, int maxW) {
        int lineY = y;
        int start = 0;

        while (start < text.length()) {
            int end = start;
            String line = "";

            // Build line word by word
            while (end < text.length()) {
                char ch = text.charAt(end);
                if (ch == '\n') {
                    end++;
                    break;
                }
                String test = line + ch;
                if (font.stringWidth(test) > maxW && line.length() > 0) {
                    break;
                }
                line += ch;
                end++;
            }

            g.drawString(line, x, lineY, Graphics.TOP | Graphics.LEFT);
            lineY += fontHeight + 2;
            start = end;

            // Stop if out of visible area
            if (lineY > getHeight() - FOOTER_HEIGHT) break;
        }
    }

    private int getLineCount() {
        if (inputText.length() == 0) return 1;
        int lines = 1;
        for (int i = 0; i < inputText.length(); i++) {
            if (inputText.charAt(i) == '\n') lines++;
        }
        return lines;
    }

    private String getLastLineText() {
        int lastNewline = inputText.lastIndexOf('\n');
        if (lastNewline < 0) return inputText;
        return inputText.substring(lastNewline + 1);
    }

    // -----------------------------------------------------------------------
    // Animation thread
    // -----------------------------------------------------------------------

    private void startAnimationThread() {
        new Thread() {
            public void run() {
                while (!submitting) {
                    try { Thread.sleep(100); } catch (InterruptedException e) { break; }

                    long now = System.currentTimeMillis();

                    // Cursor blink
                    if (now - lastBlinkTime > BLINK_INTERVAL) {
                        cursorVisible = !cursorVisible;
                        lastBlinkTime = now;
                    }

                    repaint();
                }
            }
        }.start();
    }

    // -----------------------------------------------------------------------
    // Input handling
    // -----------------------------------------------------------------------

    protected void keyPressed(int keyCode) {
        if (submitting) return;

        int action = -1;
        try { action = getGameAction(keyCode); } catch (Exception e) { /* ok */ }

        // FIRE = submit
        if (action == FIRE || keyCode == Canvas.KEY_POUND) {
            handleSubmit();
            return;
        }

        // * = backspace
        if (keyCode == Canvas.KEY_STAR) {
            if (inputText.length() > 0) {
                inputText = inputText.substring(0, inputText.length() - 1);
            }
            confirmPendingChar();
            repaint();
            return;
        }

        // # = caps toggle
        if (keyCode == Canvas.KEY_POUND) {
            capsOn = !capsOn;
            confirmPendingChar();
            repaint();
            return;
        }

        // Number keys (T9 multi-tap)
        int ki = getKeyIndex(keyCode);
        if (ki >= 0) {
            long now = System.currentTimeMillis();

            if (ki == lastKeyIndex && (now - lastKeyTime) < TAP_TIMEOUT) {
                tapCount++;
                if (inputText.length() > 0) {
                    inputText = inputText.substring(0, inputText.length() - 1);
                }
            } else {
                confirmPendingChar();
                tapCount = 0;
            }

            if (inputText.length() < MAX_CHARS) {
                String chars = KEY_MAP[ki];
                char ch = chars.charAt(tapCount % chars.length());
                if (capsOn && ch >= 'a' && ch <= 'z') {
                    ch = (char) (ch - 32);
                }
                inputText += ch;
            }

            lastKeyIndex = ki;
            lastKeyTime = now;
            repaint();
            return;
        }

        // QWERTY fallback
        if (keyCode >= 32 && keyCode < 127) {
            confirmPendingChar();
            if (inputText.length() < MAX_CHARS) {
                char ch = (char) keyCode;
                if (capsOn && ch >= 'a' && ch <= 'z') {
                    ch = (char) (ch - 32);
                }
                inputText += ch;
            }
            repaint();
        }
    }

    private void confirmPendingChar() {
        lastKeyIndex = -1;
        tapCount = 0;
    }

    private int getKeyIndex(int k) {
        switch (k) {
            case Canvas.KEY_NUM0: return 0;
            case Canvas.KEY_NUM1: return 1;
            case Canvas.KEY_NUM2: return 2;
            case Canvas.KEY_NUM3: return 3;
            case Canvas.KEY_NUM4: return 4;
            case Canvas.KEY_NUM5: return 5;
            case Canvas.KEY_NUM6: return 6;
            case Canvas.KEY_NUM7: return 7;
            case Canvas.KEY_NUM8: return 8;
            case Canvas.KEY_NUM9: return 9;
            default: return -1;
        }
    }

    // -----------------------------------------------------------------------
    // Submit / Cancel
    // -----------------------------------------------------------------------

    private void handleSubmit() {
        if (submitting) return;

        String content = inputText.trim();

        if (content.length() < MIN_CHARS) {
            midlet.showAlert("too short", "minimum " + MIN_CHARS + " characters", 2000);
            return;
        }

        if (content.length() > MAX_CHARS) {
            midlet.showAlert("too long", "maximum " + MAX_CHARS + " characters", 2000);
            return;
        }

        submitting = true;
        RMSStore.saveDraft(null);
        repaint();

        String json = Service.buildJson(
                new String[]{"content"},
                new String[]{content});
        String jwt = RMSStore.getJWT();

        Service.post("/create/post", json, jwt, new Service.Callback() {

            public void onSuccess(final String response) {
                midlet.getDisplay().callSerially(new Runnable() {
                    public void run() {
                        submitting = false;
                        midlet.invalidateHome();
                        midlet.showHome();
                    }
                });
            }

            public void onError(final String error) {
                midlet.getDisplay().callSerially(new Runnable() {
                    public void run() {
                        submitting = false;
                        repaint();
                        String msg = (error != null && error.length() > 0)
                                ? error : "unknown error";
                        midlet.showAlert("post failed", msg, 3000);
                    }
                });
            }
        });
    }

    private void handleCancel() {
        String current = inputText.trim();
        if (current.length() >= MIN_CHARS) {
            RMSStore.saveDraft(current);
        } else {
            RMSStore.saveDraft(null);
        }
        midlet.showHome();
    }

    // -----------------------------------------------------------------------
    // CommandListener
    // -----------------------------------------------------------------------

    public void commandAction(Command c, Displayable d) {
        if (c == postCommand) {
            handleSubmit();
        } else if (c == cancelCommand) {
            handleCancel();
        }
    }
}