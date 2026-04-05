package temporal;

import javax.microedition.lcdui.*;

/**
 * PostScreen v1.3 – Custom Canvas post detail view.
 *
 * SMOOTH ROUNDED AESTHETIC:
 *  - Gradient header with circular avatar
 *  - Rounded content card with soft shadow
 *  - Metadata pills (timestamp, post ID)
 *  - Floating action buttons (reply, share, delete)
 *  - Smooth scroll for long posts
 *  - Delete confirmation overlay with blur effect
 *  - Breathing button animations
 */
public class PostScreen extends Canvas implements CommandListener {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private static final int C_BG_TOP     = 0xE3F2FD;
    private static final int C_BG_BOT     = 0xBBDEFB;
    private static final int C_HEADER_TOP = 0x1976D2;
    private static final int C_HEADER_BOT = 0x42A5F5;
    private static final int C_WHITE      = 0xFFFFFF;
    private static final int C_CARD       = 0xFFFFFF;
    private static final int C_CARD_SHADOW= 0xD0D0D0;
    private static final int C_BLUE_PALE  = 0xBBDEFB;
    private static final int C_BLUE_MID   = 0x1E88E5;
    private static final int C_BLUE_DARK  = 0x0D47A1;
    private static final int C_GRAY       = 0x757575;
    private static final int C_GRAY_LIGHT = 0xBDBDBD;
    private static final int C_DARK       = 0x212121;
    private static final int C_GREEN      = 0x66BB6A;
    private static final int C_ORANGE     = 0xFF8800;
    private static final int C_RED        = 0xEF5350;
    private static final int C_PURPLE     = 0x9C27B0;
    private static final int C_CYAN       = 0x00BCD4;

    private static final int HEADER_HEIGHT = 60;
    private static final int FOOTER_HEIGHT = 56;

    // Avatar icons (same as HomeScreen)
    private static final byte[] ICON_AVATAR_A = {
        (byte)0x3C, (byte)0x42, (byte)0xA5, (byte)0x81,
        (byte)0xA5, (byte)0x99, (byte)0x42, (byte)0x3C
    };
    private static final byte[] ICON_AVATAR_B = {
        (byte)0x3C, (byte)0x7E, (byte)0xDB, (byte)0xFF,
        (byte)0xC3, (byte)0xFF, (byte)0x7E, (byte)0x3C
    };
    private static final byte[] ICON_AVATAR_C = {
        (byte)0x7E, (byte)0x81, (byte)0xA5, (byte)0x81,
        (byte)0xBD, (byte)0x81, (byte)0x81, (byte)0x7E
    };
    private static final byte[] ICON_AVATAR_D = {
        (byte)0x18, (byte)0x3C, (byte)0x7E, (byte)0xFF,
        (byte)0xDB, (byte)0xFF, (byte)0x42, (byte)0x24
    };
    private static final byte[] ICON_AVATAR_E = {
        (byte)0x08, (byte)0x49, (byte)0x2A, (byte)0x1C,
        (byte)0x1C, (byte)0x2A, (byte)0x49, (byte)0x08
    };

    private static Image imgAvatarA, imgAvatarB, imgAvatarC, imgAvatarD, imgAvatarE;

    static {
        imgAvatarA = createImageFromBits(ICON_AVATAR_A);
        imgAvatarB = createImageFromBits(ICON_AVATAR_B);
        imgAvatarC = createImageFromBits(ICON_AVATAR_C);
        imgAvatarD = createImageFromBits(ICON_AVATAR_D);
        imgAvatarE = createImageFromBits(ICON_AVATAR_E);
    }

    // -----------------------------------------------------------------------
    // Commands
    // -----------------------------------------------------------------------

    private final Command backCommand;
    private final Command replyCommand;
    private final Command shareCommand;
    private final Command deleteCommand; // null if not own post

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final Temporal midlet;
    private final String postId;
    private final String author;
    private final String rawContent;
    private final String timestamp;

    private volatile boolean deleting = false;
    private boolean showDeleteConfirm = false;

    private int scrollOffset = 0;
    private int contentHeight = 0;

    private Font font, headerFont, smallFont, tinyFont;
    private int fontHeight;

    private long animStartTime = 0;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public PostScreen(Temporal midlet, String id, String content, String author) {
        this.midlet = midlet;
        this.postId = (id != null) ? id : "";
        this.author = (author != null) ? author : "";
        this.rawContent = (content != null) ? content : "";
        this.timestamp = ""; // TODO: pass timestamp from feed

        font       = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        headerFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
        smallFont  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        tinyFont   = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        fontHeight = font.getHeight();

        backCommand  = new Command("Back",  Command.BACK, 1);
        replyCommand = new Command("Reply", Command.ITEM, 2);
        shareCommand = new Command("Share", Command.ITEM, 3);

        addCommand(backCommand);
        addCommand(replyCommand);
        addCommand(shareCommand);

        String me = RMSStore.getUsername();
        if (me != null && me.length() > 0 && me.equals(this.author)) {
            deleteCommand = new Command("Delete", Command.ITEM, 4);
            addCommand(deleteCommand);
        } else {
            deleteCommand = null;
        }

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
        drawContent(g, w, h);
        drawFooter(g, w, h);

        if (showDeleteConfirm) {
            drawDeleteConfirmOverlay(g, w, h);
        }
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

        // Avatar circle
        int avatarSize = 44;
        int avatarX = 10;
        int avatarY = HEADER_HEIGHT / 2 - avatarSize / 2;

        int ringColor = pickAvatarColor(author);
        g.setColor(ringColor);
        g.fillArc(avatarX - 2, avatarY - 2, avatarSize + 4, avatarSize + 4, 0, 360);

        g.setColor(C_WHITE);
        g.fillArc(avatarX, avatarY, avatarSize, avatarSize, 0, 360);

        Image avatar = pickAvatarIcon(author);
        drawPixelIconCentered(g, avatar, avatarX + avatarSize / 2,
                avatarY + avatarSize / 2, 4);

        // Author name
        g.setFont(headerFont);
        g.setColor(C_WHITE);
        g.drawString("@" + author, avatarX + avatarSize + 10, 10,
                Graphics.TOP | Graphics.LEFT);

        // Post ID pill
        g.setFont(tinyFont);
        String idText = "id: " + (postId.length() > 8 ? postId.substring(0, 8) + ".." : postId);
        int pillW = tinyFont.stringWidth(idText) + 12;
        int pillX = avatarX + avatarSize + 10;
        int pillY = 10 + headerFont.getHeight() + 4;

        g.setColor(C_BLUE_DARK);
        g.fillRoundRect(pillX, pillY, pillW, 14, 7, 7);
        g.setColor(C_BLUE_PALE);
        g.drawString(idText, pillX + 6, pillY + 2, Graphics.TOP | Graphics.LEFT);
    }

    private void drawContent(Graphics g, int w, int h) {
        int top = HEADER_HEIGHT + 10;
        int bot = h - FOOTER_HEIGHT - 10;
        int areaH = bot - top;

        int margin = 10;
        int cardX = margin;
        int cardW = w - margin * 2;

        // Calculate content height
        String text = unescape(rawContent);
        contentHeight = calculateTextHeight(g, text, cardW - 24);

        int cardH = Math.min(contentHeight + 24, areaH);

        // Card shadow
        g.setColor(C_CARD_SHADOW);
        g.fillRoundRect(cardX + 2, top + 2, cardW, cardH, 16, 16);

        // Card body
        g.setColor(C_CARD);
        g.fillRoundRect(cardX, top, cardW, cardH, 16, 16);

        // Border
        g.setColor(C_BLUE_PALE);
        g.drawRoundRect(cardX, top, cardW, cardH, 16, 16);

        // Content text with scroll
        g.setClip(cardX + 12, top + 12, cardW - 24, cardH - 24);
        g.setFont(font);
        g.setColor(C_DARK);
        drawWrappedText(g, text, cardX + 12, top + 12 - scrollOffset, cardW - 24);
        g.setClip(0, 0, w, h);

        // Scrollbar if needed
        if (contentHeight > cardH - 24) {
            int barH = Math.max(20, (cardH * cardH) / contentHeight);
            int barY = top + 12 + (scrollOffset * (cardH - barH - 24)) /
                    Math.max(1, contentHeight - (cardH - 24));

            g.setColor(C_GRAY_LIGHT);
            g.fillRoundRect(cardX + cardW - 8, barY + 1, 4, barH, 2, 2);
            g.setColor(C_ORANGE);
            g.fillRoundRect(cardX + cardW - 9, barY, 4, barH, 2, 2);
        }
    }

    private void drawFooter(Graphics g, int w, int h) {
        int y = h - FOOTER_HEIGHT;

        // Curved top
        g.setColor(C_WHITE);
        g.fillArc(0, y - 10, 20, 20, 180, 90);
        g.fillArc(w - 20, y - 10, 20, 20, 270, 90);
        g.fillRect(10, y, w - 20, FOOTER_HEIGHT);

        // Action buttons (breathing animation)
        long elapsed = System.currentTimeMillis() - animStartTime;
        int pulse = (int) ((Math.sin(elapsed / 400.0) + 1) * 1); // 0-2

        int buttonSize = 36 + pulse;
        int spacing = (w - 40) / 3;

        // Reply button
        int replyX = 20;
        drawActionButton(g, replyX, y + 10, buttonSize, C_GREEN, "R");

        // Share button
        int shareX = 20 + spacing;
        drawActionButton(g, shareX, y + 10, buttonSize, C_BLUE_MID, "S");

        // Delete button (if own post)
        if (deleteCommand != null) {
            int deleteX = 20 + spacing * 2;
            drawActionButton(g, deleteX, y + 10, buttonSize, C_RED, "D");
        }
    }

    private void drawActionButton(Graphics g, int x, int y, int size, int color, String label) {
        // Shadow
        g.setColor(C_GRAY);
        g.fillArc(x + 2, y + 2, size, size, 0, 360);
        // Body
        g.setColor(color);
        g.fillArc(x, y, size, size, 0, 360);
        // Label
        g.setFont(smallFont);
        g.setColor(C_WHITE);
        g.drawString(label, x + size / 2, y + size / 2 - smallFont.getHeight() / 2,
                Graphics.TOP | Graphics.HCENTER);
    }

    private void drawDeleteConfirmOverlay(Graphics g, int w, int h) {
        // Semi-transparent overlay
        g.setColor(0x000000);
        for (int y = 0; y < h; y += 2) {
            g.drawLine(0, y, w, y);
        }

        // Confirmation dialog
        int dialogW = w - 40;
        int dialogH = 100;
        int dialogX = 20;
        int dialogY = h / 2 - dialogH / 2;

        // Dialog shadow
        g.setColor(0x000000);
        g.fillRoundRect(dialogX + 3, dialogY + 3, dialogW, dialogH, 20, 20);

        // Dialog body
        g.setColor(C_WHITE);
        g.fillRoundRect(dialogX, dialogY, dialogW, dialogH, 20, 20);

        // Warning text
        g.setFont(font);
        g.setColor(C_RED);
        g.drawString("Delete this post?", dialogX + dialogW / 2, dialogY + 15,
                Graphics.TOP | Graphics.HCENTER);

        g.setFont(smallFont);
        g.setColor(C_GRAY);
        g.drawString("This cannot be undone", dialogX + dialogW / 2, dialogY + 40,
                Graphics.TOP | Graphics.HCENTER);

        // Yes/No buttons
        int btnW = 60;
        int btnH = 28;
        int btnY = dialogY + dialogH - btnH - 10;

        // No button
        int noX = dialogX + 20;
        g.setColor(C_GRAY);
        g.fillRoundRect(noX, btnY, btnW, btnH, 14, 14);
        g.setColor(C_WHITE);
        g.drawString("No", noX + btnW / 2, btnY + btnH / 2 - smallFont.getHeight() / 2,
                Graphics.TOP | Graphics.HCENTER);

        // Yes button
        int yesX = dialogX + dialogW - btnW - 20;
        g.setColor(C_RED);
        g.fillRoundRect(yesX, btnY, btnW, btnH, 14, 14);
        g.setColor(C_WHITE);
        g.drawString("Yes", yesX + btnW / 2, btnY + btnH / 2 - smallFont.getHeight() / 2,
                Graphics.TOP | Graphics.HCENTER);
    }

    // -----------------------------------------------------------------------
    // Text helpers
    // -----------------------------------------------------------------------

    private int calculateTextHeight(Graphics g, String text, int maxW) {
        g.setFont(font);
        int lineY = 0;
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
                if (font.stringWidth(test) > maxW && line.length() > 0) {
                    break;
                }
                line += ch;
                end++;
            }

            lineY += fontHeight + 4;
            start = end;
        }

        return lineY;
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
                if (font.stringWidth(test) > maxW && line.length() > 0) {
                    break;
                }
                line += ch;
                end++;
            }

            g.drawString(line, x, lineY, Graphics.TOP | Graphics.LEFT);
            lineY += fontHeight + 4;
            start = end;
        }
    }

    private String unescape(String s) {
        if (s == null) return "";
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case 'n':  sb.append('\n'); i++; break;
                    case 'r':  sb.append('\r'); i++; break;
                    case 't':  sb.append('\t'); i++; break;
                    case '"':  sb.append('"');  i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    default: sb.append(ch); break;
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Avatar helpers
    // -----------------------------------------------------------------------

    private Image pickAvatarIcon(String author) {
        if (author == null || author.length() == 0) return imgAvatarA;
        int hash = Math.abs(author.hashCode()) % 5;
        switch (hash) {
            case 0: return imgAvatarA;
            case 1: return imgAvatarB;
            case 2: return imgAvatarC;
            case 3: return imgAvatarD;
            default: return imgAvatarE;
        }
    }

    private int pickAvatarColor(String author) {
        if (author == null || author.length() == 0) return C_BLUE_MID;
        int hash = Math.abs(author.hashCode()) % 5;
        switch (hash) {
            case 0: return C_BLUE_MID;
            case 1: return C_GREEN;
            case 2: return C_PURPLE;
            case 3: return C_ORANGE;
            default: return C_CYAN;
        }
    }

    // -----------------------------------------------------------------------
    // Icon rendering
    // -----------------------------------------------------------------------

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
    // Input handling
    // -----------------------------------------------------------------------

    protected void keyPressed(int keyCode) {
        int action = -1;
        try { action = getGameAction(keyCode); } catch (Exception e) { /* ok */ }

        if (showDeleteConfirm) {
            if (keyCode == Canvas.KEY_NUM1 || action == FIRE) {
                // Yes - execute delete
                showDeleteConfirm = false;
                executeDelete();
            } else if (keyCode == Canvas.KEY_NUM2 || keyCode == Canvas.KEY_NUM0) {
                // No - cancel
                showDeleteConfirm = false;
                repaint();
            }
            return;
        }

        if (action == UP) {
            if (scrollOffset > 0) {
                scrollOffset -= 10;
                repaint();
            }
        } else if (action == DOWN) {
            int maxScroll = Math.max(0, contentHeight - (getHeight() - HEADER_HEIGHT - FOOTER_HEIGHT - 44));
            if (scrollOffset < maxScroll) {
                scrollOffset += 10;
                repaint();
            }
        } else if (keyCode == Canvas.KEY_NUM1) {
            // Reply
            midlet.showCreatePostWithMention("@" + author + " ");
        } else if (keyCode == Canvas.KEY_NUM2) {
            // Share
            showShareSheet();
        } else if (keyCode == Canvas.KEY_NUM3 && deleteCommand != null) {
            // Delete
            showDeleteConfirm = true;
            repaint();
        }
    }

    // -----------------------------------------------------------------------
    // Animation thread
    // -----------------------------------------------------------------------

    private void startAnimationThread() {
        new Thread() {
            public void run() {
                while (!deleting) {
                    try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                    repaint();
                }
            }
        }.start();
    }

    // -----------------------------------------------------------------------
    // Delete execution
    // -----------------------------------------------------------------------

    private void executeDelete() {
        if (deleting) return;
        deleting = true;
        repaint();

        final String jwt = RMSStore.getJWT();
        Service.delete("/post/" + postId, jwt, new Service.Callback() {

            public void onSuccess(final String response) {
                midlet.getDisplay().callSerially(new Runnable() {
                    public void run() {
                        deleting = false;
                        String last = RMSStore.getLastPostId();
                        if (postId.equals(last)) {
                            RMSStore.saveLastPostId(null);
                        }
                        midlet.invalidateHome();
                        midlet.showHome();
                    }
                });
            }

            public void onError(final String error) {
                midlet.getDisplay().callSerially(new Runnable() {
                    public void run() {
                        deleting = false;
                        repaint();
                        String msg = (error != null && error.length() > 0)
                                ? error : "unknown error";
                        midlet.showAlert("delete failed", msg, 3000);
                    }
                });
            }
        });
    }

    // -----------------------------------------------------------------------
    // Share sheet
    // -----------------------------------------------------------------------

    private void showShareSheet() {
        String shareText = "@" + author + ":\n" + unescape(rawContent);
        final Form shareForm = new Form("share post");
        TextField tf = new TextField(null, shareText, shareText.length() + 10, TextField.ANY);
        shareForm.append(tf);

        Command closeCmd = new Command("close", Command.BACK, 1);
        shareForm.addCommand(closeCmd);
        shareForm.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                midlet.getDisplay().setCurrent(PostScreen.this);
            }
        });

        midlet.getDisplay().setCurrent(shareForm);
    }

    // -----------------------------------------------------------------------
    // CommandListener
    // -----------------------------------------------------------------------

    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            midlet.showHome();
        } else if (c == replyCommand) {
            midlet.showCreatePostWithMention("@" + author + " ");
        } else if (c == shareCommand) {
            showShareSheet();
        } else if (c == deleteCommand) {
            showDeleteConfirm = true;
            repaint();
        }
    }
}