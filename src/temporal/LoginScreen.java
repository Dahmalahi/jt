package temporal;

import javax.microedition.lcdui.*;

/**
 * LoginScreen v1.2 – Custom Canvas login/register view.
 *
 * SMOOTH ROUNDED AESTHETIC:
 *  - Gradient background with floating logo
 *  - Rounded input cards with labels
 *  - Breathing submit button
 *  - Smooth mode toggle animation
 *  - Inline error messages
 *  - T9 keyboard input with caps indicator
 *  - KEY 5 = enter type mode (focus + ready to type)
 *  - KEY # = cycle input modes: abc -> ABC -> 123
 */
public class LoginScreen extends Canvas implements CommandListener {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private static final int C_BG_TOP     = 0x1976D2;
    private static final int C_BG_BOT     = 0x42A5F5;
    private static final int C_WHITE      = 0xFFFFFF;
    private static final int C_CARD       = 0xFFFFFF;
    private static final int C_CARD_SHADOW= 0xBBDEFB;
    private static final int C_BLUE_PALE  = 0xBBDEFB;
    private static final int C_BLUE_MID   = 0x1E88E5;
    private static final int C_BLUE_DARK  = 0x0D47A1;
    private static final int C_GRAY       = 0x757575;
    private static final int C_GRAY_LIGHT = 0xBDBDBD;
    private static final int C_DARK       = 0x212121;
    private static final int C_GREEN      = 0x66BB6A;
    private static final int C_RED        = 0xEF5350;
    private static final int C_ORANGE     = 0xFF8800;
    private static final int C_PURPLE     = 0x9C27B0;

    // Temporal logo icon (orbital T)
    private static final byte[] ICON_LOGO = {
        (byte)0x00, (byte)0xFE, (byte)0x10, (byte)0x38,
        (byte)0x6C, (byte)0x44, (byte)0x00, (byte)0x00
    };

    private static Image imgLogo;

    static {
        imgLogo = createImageFromBits(ICON_LOGO);
    }

    // T9 keymap (KEY 5 removed from normal input)
    private static final String[] KEY_MAP = {
        " 0", ".,!?'\"1-()@/:_", "abc2", "def3", "ghi4",
        "", "mno6", "pqrs7", "tuv8", "wxyz9"  // KEY_MAP[5] is now empty
    };

    // Numerical keymap (direct digit input)
    private static final String[] NUM_MAP = {
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
    };

    // Input mode constants
    private static final int MODE_LOWERCASE = 0;
    private static final int MODE_UPPERCASE = 1;
    private static final int MODE_NUMERIC   = 2;

    // -----------------------------------------------------------------------
    // Commands
    // -----------------------------------------------------------------------

    private final Command exitCommand;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final Temporal midlet;

    private boolean isLoginMode = true;
    private boolean submitting = false;

    private String username = "";
    private String password = "";
    private int activeField = 0; // 0=username, 1=password

    private boolean typeMode = false; // true when in focused typing mode
    private int inputMode = MODE_LOWERCASE; // 0=abc, 1=ABC, 2=123

    private String errorMessage = "";

    private Font font, headerFont, smallFont, tinyFont;
    private int fontHeight;

    // T9 state
    private int lastKeyIndex = -1;
    private int tapCount = 0;
    private long lastKeyTime = 0;
    private static final long TAP_TIMEOUT = 900;

    // Cursor blink
    private boolean cursorVisible = true;
    private long lastBlinkTime = 0;
    private static final long BLINK_INTERVAL = 500;

    // Button pulse
    private long animStartTime = 0;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public LoginScreen(Temporal midlet) {
        this.midlet = midlet;

        font       = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        headerFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_LARGE);
        smallFont  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        tinyFont   = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        fontHeight = font.getHeight();

        exitCommand = new Command("Exit", Command.EXIT, 1);
        addCommand(exitCommand);
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

        drawLogo(g, w, h);
        drawForm(g, w, h);
        drawFooter(g, w, h);

        if (errorMessage.length() > 0) {
            drawError(g, w, h);
        }
    }

    private void drawLogo(Graphics g, int w, int h) {
        // Large logo at top
        int logoSize = 48;
        int logoX = w / 2;
        int logoY = 30;

        // Glow effect
        g.setColor(C_BLUE_PALE);
        g.fillArc(logoX - logoSize / 2 - 4, logoY - logoSize / 2 - 4,
                logoSize + 8, logoSize + 8, 0, 360);

        // White circle
        g.setColor(C_WHITE);
        g.fillArc(logoX - logoSize / 2, logoY - logoSize / 2,
                logoSize, logoSize, 0, 360);

        // Logo icon
        drawPixelIconCentered(g, imgLogo, logoX, logoY, 5);

        // App name
        g.setFont(headerFont);
        g.setColor(C_WHITE);
        g.drawString("temporal", w / 2, logoY + logoSize / 2 + 10,
                Graphics.TOP | Graphics.HCENTER);

        g.setFont(smallFont);
        g.drawString(isLoginMode ? "welcome back" : "create account",
                w / 2, logoY + logoSize / 2 + 10 + headerFont.getHeight() + 4,
                Graphics.TOP | Graphics.HCENTER);
    }

    private void drawForm(Graphics g, int w, int h) {
        int formY = 140;
        int cardW = w - 40;
        int cardX = 20;
        int cardH = 50;
        int spacing = 12;

        // Username field
        drawInputCard(g, cardX, formY, cardW, cardH, "username", username,
                activeField == 0 && typeMode);

        // Password field
        drawInputCard(g, cardX, formY + cardH + spacing, cardW, cardH,
                "password", maskPassword(password), activeField == 1 && typeMode);

        // Submit button
        int buttonY = formY + (cardH + spacing) * 2 + 20;
        int buttonW = 100;
        int buttonH = 40;
        int buttonX = w / 2 - buttonW / 2;

        boolean canSubmit = username.trim().length() > 0 && password.length() > 0;

        if (canSubmit && !submitting) {
            long elapsed = System.currentTimeMillis() - animStartTime;
            int pulse = (int) ((Math.sin(elapsed / 400.0) + 1) * 2);
            buttonW += pulse;
            buttonH += pulse / 2;
            buttonX = w / 2 - buttonW / 2;
            buttonY -= pulse / 4;
        }

        // Shadow
        g.setColor(C_BLUE_DARK);
        g.fillRoundRect(buttonX + 2, buttonY + 2, buttonW, buttonH, 20, 20);

        // Body
        if (submitting) {
            g.setColor(C_GRAY);
        } else if (canSubmit) {
            g.setColor(C_GREEN);
        } else {
            g.setColor(C_GRAY_LIGHT);
        }
        g.fillRoundRect(buttonX, buttonY, buttonW, buttonH, 20, 20);

        // Label
        g.setFont(font);
        g.setColor(C_WHITE);
        String label = submitting ? "..." : (isLoginMode ? "login" : "register");
        g.drawString(label, buttonX + buttonW / 2,
                buttonY + buttonH / 2 - font.getHeight() / 2,
                Graphics.TOP | Graphics.HCENTER);

        // Toggle link
        if (!submitting) {
            g.setFont(smallFont);
            g.setColor(C_WHITE);
            String toggle = isLoginMode ? "need an account? press 1" : "have an account? press 1";
            g.drawString(toggle, w / 2, buttonY + buttonH + 16,
                    Graphics.TOP | Graphics.HCENTER);
        }
    }

    private void drawInputCard(Graphics g, int x, int y, int w, int h,
                                 String label, String value, boolean active) {
        // Shadow
        g.setColor(C_CARD_SHADOW);
        g.fillRoundRect(x + 2, y + 2, w, h, 12, 12);

        // Body
        g.setColor(C_CARD);
        g.fillRoundRect(x, y, w, h, 12, 12);

        // Border (active & typeMode = bright blue with pulse, inactive = pale)
        if (active) {
            // Border color changes based on input mode
            int borderColor = C_BLUE_MID;
            if (inputMode == MODE_UPPERCASE) {
                borderColor = C_ORANGE;
            } else if (inputMode == MODE_NUMERIC) {
                borderColor = C_PURPLE;
            }

            // Pulsing border when in type mode
            long elapsed = System.currentTimeMillis() - animStartTime;
            int pulse = (int) ((Math.sin(elapsed / 300.0) + 1) * 20); // 0-40
            int finalColor = interpolateColor(borderColor, C_WHITE, pulse, 40);
            g.setColor(finalColor);
            g.drawRoundRect(x, y, w, h, 12, 12);
            // Draw thicker border
            g.drawRoundRect(x + 1, y + 1, w - 2, h - 2, 12, 12);
        } else {
            g.setColor(C_BLUE_PALE);
            g.drawRoundRect(x, y, w, h, 12, 12);
        }

        // Label
        g.setFont(tinyFont);
        g.setColor(active ? C_BLUE_DARK : C_GRAY);
        g.drawString(label, x + 10, y + 4, Graphics.TOP | Graphics.LEFT);

        // Value
        g.setFont(font);
        g.setColor(C_DARK);
        if (value.length() == 0) {
            g.setColor(C_GRAY_LIGHT);
            g.drawString(active ? "type here..." : "...", x + 10, y + 4 + tinyFont.getHeight() + 4,
                    Graphics.TOP | Graphics.LEFT);
        } else {
            g.drawString(value, x + 10, y + 4 + tinyFont.getHeight() + 4,
                    Graphics.TOP | Graphics.LEFT);
        }

        // Cursor
        if (active && cursorVisible && !submitting) {
            int cursorX = x + 10 + font.stringWidth(value);
            int cursorY = y + 4 + tinyFont.getHeight() + 4;
            // Cursor color matches input mode
            if (inputMode == MODE_NUMERIC) {
                g.setColor(C_PURPLE);
            } else if (inputMode == MODE_UPPERCASE) {
                g.setColor(C_ORANGE);
            } else {
                g.setColor(C_BLUE_MID);
            }
            g.fillRect(cursorX, cursorY, 2, fontHeight);
        }
    }

    private void drawFooter(Graphics g, int w, int h) {
        int footerH = 26;
        int y = h - footerH;

        g.setFont(tinyFont);
        g.setColor(C_WHITE);

        if (typeMode) {
            // In type mode - show typing instructions
            String fieldName = activeField == 0 ? "username" : "password";
            g.drawString("[typing " + fieldName + "]", 4, y + 2, Graphics.TOP | Graphics.LEFT);

            // Input mode indicator (color-coded)
            String modeLabel;
            int modeColor;
            if (inputMode == MODE_NUMERIC) {
                modeLabel = "123";
                modeColor = C_PURPLE;
            } else if (inputMode == MODE_UPPERCASE) {
                modeLabel = "ABC";
                modeColor = C_ORANGE;
            } else {
                modeLabel = "abc";
                modeColor = C_BLUE_PALE;
            }

            g.setColor(modeColor);
            int modeW = tinyFont.stringWidth(modeLabel) + 8;
            g.fillRoundRect(w / 2 - modeW / 2, y, modeW, 14, 7, 7);
            g.setColor(C_WHITE);
            g.drawString(modeLabel, w / 2, y + 2, Graphics.TOP | Graphics.HCENTER);

            g.setColor(C_WHITE);
            g.drawString("*=del #=mode", w - 4, y + 2, Graphics.TOP | Graphics.RIGHT);
            g.drawString("FIRE=done", w - 4, y + 2 + tinyFont.getHeight() + 2,
                    Graphics.TOP | Graphics.RIGHT);
        } else {
            // Navigation mode - show field selection
            String fieldHint = activeField == 0 ? "[username]" : "[password]";
            g.drawString(fieldHint, 4, y + 2, Graphics.TOP | Graphics.LEFT);

            g.drawString("5=type", w / 2, y + 2, Graphics.TOP | Graphics.HCENTER);

            g.drawString("UP/DN=switch", w - 4, y + 2, Graphics.TOP | Graphics.RIGHT);
            g.drawString("FIRE=submit", w - 4, y + 2 + tinyFont.getHeight() + 2,
                    Graphics.TOP | Graphics.RIGHT);
        }
    }

    private void drawError(Graphics g, int w, int h) {
        int msgH = 60;
        int msgW = w - 40;
        int msgX = 20;
        int msgY = h - msgH - 40;

        // Shadow
        g.setColor(0x000000);
        g.fillRoundRect(msgX + 2, msgY + 2, msgW, msgH, 12, 12);

        // Body (green for success, red for error)
        boolean isSuccess = errorMessage.startsWith("account created");
        g.setColor(isSuccess ? C_GREEN : C_RED);
        g.fillRoundRect(msgX, msgY, msgW, msgH, 12, 12);

        // Text
        g.setFont(smallFont);
        g.setColor(C_WHITE);
        drawWrappedText(g, errorMessage, msgX + 10, msgY + 10, msgW - 20);
    }

    // -----------------------------------------------------------------------
    // Input handling
    // -----------------------------------------------------------------------

    protected void keyPressed(int keyCode) {
        if (submitting) return;

        int action = -1;
        try { action = getGameAction(keyCode); } catch (Exception e) { /* ok */ }

        // ===== NAVIGATION MODE (typeMode = false) =====
        if (!typeMode) {
            // KEY 5 = ENTER TYPE MODE
            if (keyCode == Canvas.KEY_NUM5) {
                typeMode = true;
                confirmPendingChar();
                repaint();
                return;
            }

            // UP/DOWN = switch field
            if (action == UP) {
                activeField = 0;
                repaint();
                return;
            }
            if (action == DOWN) {
                activeField = 1;
                repaint();
                return;
            }

            // FIRE = submit
            if (action == FIRE) {
                handleSubmit();
                return;
            }

            // KEY 1 = toggle mode (login <-> register)
            if (keyCode == Canvas.KEY_NUM1) {
                toggleMode();
                return;
            }

            return; // ignore other keys in navigation mode
        }

        // ===== TYPE MODE (typeMode = true) =====

        // FIRE = exit type mode
        if (action == FIRE) {
            typeMode = false;
            confirmPendingChar();
            repaint();
            return;
        }

        // * = backspace
        if (keyCode == Canvas.KEY_STAR) {
            if (activeField == 0 && username.length() > 0) {
                username = username.substring(0, username.length() - 1);
            } else if (activeField == 1 && password.length() > 0) {
                password = password.substring(0, password.length() - 1);
            }
            confirmPendingChar();
            repaint();
            return;
        }

        // # = cycle input mode (abc -> ABC -> 123)
        if (keyCode == Canvas.KEY_POUND) {
            inputMode = (inputMode + 1) % 3;
            confirmPendingChar();
            repaint();
            return;
        }

        // Number keys
        int ki = getKeyIndex(keyCode);
        if (ki >= 0) {
            char ch = getCharForKey(ki);
            if (ch != '\0') {
                if (activeField == 0 && username.length() < 32) {
                    username += ch;
                } else if (activeField == 1 && password.length() < 64) {
                    password += ch;
                }
                repaint();
            }
            return;
        }

        // QWERTY fallback
        if (keyCode >= 32 && keyCode < 127) {
            confirmPendingChar();
            char ch = (char) keyCode;
            if (inputMode == MODE_UPPERCASE && ch >= 'a' && ch <= 'z') {
                ch = (char) (ch - 32);
            }

            if (activeField == 0 && username.length() < 32) {
                username += ch;
            } else if (activeField == 1 && password.length() < 64) {
                password += ch;
            }
            repaint();
        }
    }

    private char getCharForKey(int keyIndex) {
        if (keyIndex < 0 || keyIndex > 9) return '\0';

        // In numeric mode, return digit directly (no multi-tap)
        if (inputMode == MODE_NUMERIC) {
            confirmPendingChar();
            return NUM_MAP[keyIndex].charAt(0);
        }

        // Skip key 5 (it's reserved for mode toggle)
        if (keyIndex == 5) return '\0';

        // T9 multi-tap logic
        String chars = KEY_MAP[keyIndex];
        if (chars.length() == 0) return '\0';

        long now = System.currentTimeMillis();

        if (keyIndex == lastKeyIndex && (now - lastKeyTime) < TAP_TIMEOUT) {
            tapCount++;
            // Remove last char for multi-tap replacement
            if (activeField == 0 && username.length() > 0) {
                username = username.substring(0, username.length() - 1);
            } else if (activeField == 1 && password.length() > 0) {
                password = password.substring(0, password.length() - 1);
            }
        } else {
            confirmPendingChar();
            tapCount = 0;
        }

        char ch = chars.charAt(tapCount % chars.length());

        // Apply uppercase in MODE_UPPERCASE
        if (inputMode == MODE_UPPERCASE && ch >= 'a' && ch <= 'z') {
            ch = (char) (ch - 32);
        }

        lastKeyIndex = keyIndex;
        lastKeyTime = now;

        return ch;
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
    // Submit / Toggle
    // -----------------------------------------------------------------------

    private void handleSubmit() {
        final String user = username.trim();
        final String pass = password;

        if (user.length() == 0 || pass.length() == 0) {
            showError("username and password required");
            return;
        }

        submitting = true;
        errorMessage = "";
        typeMode = false; // exit type mode when submitting
        repaint();

        String json = Service.buildJson(
                new String[]{"username", "password"},
                new String[]{user, pass});

        if (isLoginMode) {
            Service.post("/create/token", json, null, new Service.Callback() {
                public void onSuccess(final String response) {
                    RMSStore.saveJWT(response);
                    RMSStore.saveUsername(user);
                    midlet.getDisplay().callSerially(new Runnable() {
                        public void run() {
                            submitting = false;
                            midlet.showHome();
                        }
                    });
                }

                public void onError(final String error) {
                    midlet.getDisplay().callSerially(new Runnable() {
                        public void run() {
                            submitting = false;
                            showError(error != null ? error : "login failed");
                        }
                    });
                }
            });
        } else {
            Service.post("/create/account", json, null, new Service.Callback() {
                public void onSuccess(final String response) {
                    midlet.getDisplay().callSerially(new Runnable() {
                        public void run() {
                            submitting = false;
                            username = "";
                            password = "";
                            activeField = 0;
                            isLoginMode = true;
                            showError("account created! please log in.");
                            // Auto-clear success message after 3s
                            new Thread() {
                                public void run() {
                                    try { Thread.sleep(3000); }
                                    catch (InterruptedException e) { /* ok */ }
                                    errorMessage = "";
                                    repaint();
                                }
                            }.start();
                        }
                    });
                }

                public void onError(final String error) {
                    midlet.getDisplay().callSerially(new Runnable() {
                        public void run() {
                            submitting = false;
                            showError(error != null ? error : "registration failed");
                        }
                    });
                }
            });
        }
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        username = "";
        password = "";
        activeField = 0;
        typeMode = false;
        inputMode = MODE_LOWERCASE;
        errorMessage = "";
        confirmPendingChar();
        repaint();
    }

    private void showError(String msg) {
        errorMessage = msg;
        repaint();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String maskPassword(String pass) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < pass.length(); i++) {
            sb.append('*');
        }
        return sb.toString();
    }

    private int interpolateColor(int c1, int c2, int value, int max) {
        int ratio = (value * 255) / max;
        int r = ((c1 >> 16) & 0xFF) * (255 - ratio) / 255 + ((c2 >> 16) & 0xFF) * ratio / 255;
        int g = ((c1 >> 8) & 0xFF) * (255 - ratio) / 255 + ((c2 >> 8) & 0xFF) * ratio / 255;
        int b = (c1 & 0xFF) * (255 - ratio) / 255 + (c2 & 0xFF) * ratio / 255;
        return (r << 16) | (g << 8) | b;
    }

    private void drawWrappedText(Graphics g, String text, int x, int y, int maxW) {
        int lineY = y;
        int start = 0;

        while (start < text.length()) {
            int end = start;
            String line = "";

            while (end < text.length()) {
                char ch = text.charAt(end);
                if (ch == '\n') {
                    end++;
                    break;
                }
                String test = line + ch;
                if (g.getFont().stringWidth(test) > maxW && line.length() > 0) {
                    break;
                }
                line += ch;
                end++;
            }

            g.drawString(line, x, lineY, Graphics.TOP | Graphics.LEFT);
            lineY += g.getFont().getHeight() + 2;
            start = end;
        }
    }

    private void drawPixelIconCentered(Graphics g, Image img, int cx, int cy, int scale) {
        if (img == null) return;
        int w = img.getWidth() * scale;
        int h = img.getHeight() * scale;
        int x = cx - w / 2;
        int y = cy - h / 2;

        int iw = img.getWidth();
        int ih = img.getHeight();
        int[] rgb = new int[iw * ih];
        img.getRGB(rgb, 0, iw, 0, 0, iw, ih);

        for (int row = 0; row < ih; row++) {
            for (int col = 0; col < iw; col++) {
                int pixel = rgb[row * iw + col];
                if ((pixel & 0xFF000000) != 0) {
                    g.setColor(pixel & 0xFFFFFF);
                    g.fillRect(x + col * scale, y + row * scale, scale, scale);
                }
            }
        }
    }

    private static Image createImageFromBits(byte[] bits) {
        int[] rgb = new int[64];
        for (int row = 0; row < 8; row++) {
            byte b = bits[row];
            for (int col = 0; col < 8; col++) {
                int mask = 1 << (7 - col);
                boolean on = (b & mask) != 0;
                rgb[row * 8 + col] = on ? 0xFF000000 : 0xFFFFFFFF;
            }
        }
        return Image.createRGBImage(rgb, 8, 8, true);
    }

    // -----------------------------------------------------------------------
    // Animation
    // -----------------------------------------------------------------------

    private void startAnimationThread() {
        new Thread() {
            public void run() {
                while (true) {
                    try { Thread.sleep(100); } catch (InterruptedException e) { break; }

                    long now = System.currentTimeMillis();

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
    // CommandListener
    // -----------------------------------------------------------------------

    public void commandAction(Command c, Displayable d) {
        if (c == exitCommand) {
            midlet.exitMIDlet();
        }
    }
}