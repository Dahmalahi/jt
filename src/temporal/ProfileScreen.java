package temporal;

import java.util.Vector;
import javax.microedition.lcdui.*;

/**
 * ProfileScreen v1.3 – Custom Canvas profile view.
 *
 * SMOOTH ROUNDED AESTHETIC:
 *  - Gradient header with large circular avatar
 *  - Stats cards (post count, joined date, etc.)
 *  - Rounded post cards (same style as HomeScreen)
 *  - Bio section with edit button
 *  - Smooth scroll
 *  - Breathing "new post" FAB
 */
public class ProfileScreen extends Canvas implements CommandListener {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    private static final int PREVIEW_LEN = 26;
    private static final int CARD_HEIGHT = 52;
    private static final int CARD_MARGIN = 6;

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
    private static final int C_PURPLE     = 0x9C27B0;

    private static final int HEADER_HEIGHT = 140;
    private static final int FOOTER_HEIGHT = 20;

    // Avatar icons
    private static final byte[] ICON_AVATAR_A = {
        (byte)0x3C, (byte)0x42, (byte)0xA5, (byte)0x81,
        (byte)0xA5, (byte)0x99, (byte)0x42, (byte)0x3C
    };

    private static Image imgAvatar;

    static {
        imgAvatar = createImageFromBits(ICON_AVATAR_A);
    }

    // -----------------------------------------------------------------------
    // Commands
    // -----------------------------------------------------------------------

    private final Command backCommand;
    private final Command refreshCommand;
    private final Command newPostCommand;
    private final Command loadMoreCommand;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final Temporal midlet;
    private final String username;

    private Vector posts;
    private volatile boolean isLoading = false;
    private int currentPage = 1;
    private int postCount = 0;

    private int scrollOffset = 0;
    private int selectedIndex = -1;

    private Font font, headerFont, smallFont, tinyFont;
    private int fontHeight;

    private long animStartTime = 0;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public ProfileScreen(Temporal midlet) {
        this.midlet = midlet;

        String u = RMSStore.getUsername();
        this.username = (u != null && u.length() > 0) ? u : "";

        font       = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        headerFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_LARGE);
        smallFont  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        tinyFont   = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        fontHeight = font.getHeight();

        backCommand     = new Command("Back",      Command.BACK, 1);
        refreshCommand  = new Command("Refresh",   Command.ITEM, 2);
        newPostCommand  = new Command("New Post",  Command.ITEM, 3);
        loadMoreCommand = new Command("Load More", Command.ITEM, 4);

        addCommand(backCommand);
        addCommand(refreshCommand);
        addCommand(newPostCommand);
        addCommand(loadMoreCommand);
        setCommandListener(this);

        setFullScreenMode(true);
        animStartTime = System.currentTimeMillis();
        startAnimationThread();
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public void loadPosts() {
        if (username.length() == 0) {
            return;
        }

        currentPage = 1;
        posts = null;
        postCount = 0;
        scrollOffset = 0;
        selectedIndex = -1;

        isLoading = true;
        repaint();

        fetchPage(1, false);
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
        drawPosts(g, w, h);
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

        // Large avatar
        int avatarSize = 72;
        int avatarX = w / 2 - avatarSize / 2;
        int avatarY = 15;

        // Avatar ring (colored)
        g.setColor(C_PURPLE);
        g.fillArc(avatarX - 3, avatarY - 3, avatarSize + 6, avatarSize + 6, 0, 360);

        // Avatar white background
        g.setColor(C_WHITE);
        g.fillArc(avatarX, avatarY, avatarSize, avatarSize, 0, 360);

        // Avatar icon
        drawPixelIconCentered(g, imgAvatar, avatarX + avatarSize / 2,
                avatarY + avatarSize / 2, 7);

        // Username
        g.setFont(headerFont);
        g.setColor(C_WHITE);
        g.drawString("@" + username, w / 2, avatarY + avatarSize + 8,
                Graphics.TOP | Graphics.HCENTER);

        // Stats card
        int statsY = avatarY + avatarSize + headerFont.getHeight() + 14;
        int statsW = w - 40;
        int statsX = 20;
        int statsH = 24;

        // Shadow
        g.setColor(C_BLUE_DARK);
        g.fillRoundRect(statsX + 1, statsY + 1, statsW, statsH, 12, 12);
        // Body
        g.setColor(C_WHITE);
        g.fillRoundRect(statsX, statsY, statsW, statsH, 12, 12);

        g.setFont(smallFont);
        g.setColor(C_BLUE_DARK);
        String stats = postCount + " posts";
        g.drawString(stats, w / 2, statsY + 4, Graphics.TOP | Graphics.HCENTER);
    }

    private void drawPosts(Graphics g, int w, int h) {
        int top = HEADER_HEIGHT + 10;
        int bot = h - FOOTER_HEIGHT - 10;
        int contentH = bot - top;

        if (isLoading && (posts == null || posts.size() == 0)) {
            // Spinning loader
            long elapsed = System.currentTimeMillis() - animStartTime;
            int angle = (int) ((elapsed / 10) % 360);

            int cx = w / 2;
            int cy = top + contentH / 2;

            g.setColor(C_BLUE_PALE);
            g.fillArc(cx - 20, cy - 20, 40, 40, 0, 360);
            g.setColor(C_BG_BOT);
            g.fillArc(cx - 14, cy - 14, 28, 28, 0, 360);
            g.setColor(C_BLUE_MID);
            g.fillArc(cx - 20, cy - 20, 40, 40, angle, 90);

            g.setFont(smallFont);
            g.setColor(C_GRAY);
            g.drawString("loading", cx, cy + 30, Graphics.TOP | Graphics.HCENTER);
            return;
        }

        if (posts == null || posts.size() == 0) {
            g.setColor(C_GRAY_LIGHT);
            g.fillRoundRect(w / 2 - 60, top + contentH / 2 - 30, 120, 60, 20, 20);
            g.setColor(C_GRAY);
            g.setFont(font);
            g.drawString("no posts yet", w / 2, top + contentH / 2,
                    Graphics.BASELINE | Graphics.HCENTER);
            return;
        }

        int y = top - scrollOffset;

        for (int i = 0; i < posts.size(); i++) {
            String[] post = (String[]) posts.elementAt(i);

            String content = (post[1] != null) ? post[1] : "";
            String ts = (post.length > 3 && post[3] != null) ? post[3] : "";

            content = content.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
            if (content.length() > PREVIEW_LEN) {
                content = content.substring(0, PREVIEW_LEN) + "..";
            }

            String relTime = toRelativeTime(ts);

            int cardX = CARD_MARGIN;
            int cardW = w - CARD_MARGIN * 2;
            int cardH = CARD_HEIGHT;

            if (y + cardH < top || y > bot) {
                y += cardH + CARD_MARGIN;
                continue;
            }

            // Card shadow
            g.setColor(C_CARD_SHADOW);
            g.fillRoundRect(cardX + 2, y + 2, cardW, cardH, 16, 16);

            // Card body
            if (i == selectedIndex) {
                g.setColor(0xFFF9C4);
            } else {
                g.setColor(C_CARD);
            }
            g.fillRoundRect(cardX, y, cardW, cardH, 16, 16);

            g.setColor(C_BLUE_PALE);
            g.drawRoundRect(cardX, y, cardW, cardH, 16, 16);

            // Content
            g.setFont(font);
            g.setColor(C_DARK);
            g.drawString(content, cardX + 12, y + 10, Graphics.TOP | Graphics.LEFT);

            // Timestamp
            if (relTime.length() > 0) {
                g.setFont(tinyFont);
                g.setColor(C_GRAY_LIGHT);
                int timeW = tinyFont.stringWidth(relTime) + 8;
                g.fillRoundRect(cardX + cardW - timeW - 6, y + cardH - 18, timeW, 14, 7, 7);
                g.setColor(C_GRAY);
                g.drawString(relTime, cardX + cardW - timeW / 2 - 6, y + cardH - 16,
                        Graphics.TOP | Graphics.HCENTER);
            }

            y += cardH + CARD_MARGIN;
        }

        // Scrollbar
        int totalH = posts.size() * (CARD_HEIGHT + CARD_MARGIN);
        if (totalH > contentH) {
            int barH = Math.max(20, (contentH * contentH) / totalH);
            int barY = top + (scrollOffset * (contentH - barH)) /
                    Math.max(1, totalH - contentH);

            g.setColor(C_GRAY_LIGHT);
            g.fillRoundRect(w - 7, barY + 1, 5, barH, 3, 3);
            g.setColor(C_ORANGE);
            g.fillRoundRect(w - 8, barY, 5, barH, 3, 3);
        }
    }

    private void drawFooter(Graphics g, int w, int h) {
        int y = h - FOOTER_HEIGHT;

        g.setColor(C_WHITE);
        g.fillArc(0, y - 10, 20, 20, 180, 90);
        g.fillArc(w - 20, y - 10, 20, 20, 270, 90);
        g.fillRect(10, y, w - 20, FOOTER_HEIGHT);

        g.setFont(tinyFont);
        g.setColor(C_BLUE_MID);
        g.drawString("Back", 6, y + 3, Graphics.TOP | Graphics.LEFT);
        g.drawString("Menu", w - 6, y + 3, Graphics.TOP | Graphics.RIGHT);
    }

    // -----------------------------------------------------------------------
    // Network
    // -----------------------------------------------------------------------

    private void fetchPage(final int page, final boolean append) {
        final String jwt = RMSStore.getJWT();
        final String endpoint = "/u/" + username + "?page=" + page;

        Service.get(endpoint, jwt, new Service.Callback() {

            public void onSuccess(final String response) {
                final Vector newPosts = JSON.parseArray(
                        response, new String[]{"id", "content", "author", "ts"});

                midlet.getDisplay().callSerially(new Runnable() {
                    public void run() {
                        isLoading = false;

                        if (!append) {
                            posts = new Vector();
                        } else {
                            if (posts == null) posts = new Vector();
                        }

                        int newCount = newPosts.size();
                        for (int i = 0; i < newCount; i++) {
                            posts.addElement(newPosts.elementAt(i));
                        }

                        postCount = posts.size();
                        repaint();
                    }
                });
            }

            public void onError(final String error) {
                midlet.getDisplay().callSerially(new Runnable() {
                    public void run() {
                        isLoading = false;
                        repaint();
                        String msg = (error != null && error.length() > 0)
                                ? error : "unknown error";
                        midlet.showAlert("Error", msg);
                    }
                });
            }
        });
    }

    // -----------------------------------------------------------------------
    // Time helpers
    // -----------------------------------------------------------------------

    private String toRelativeTime(String iso) {
        try {
            String s = iso;
            int dotIdx = s.indexOf('.');
            if (dotIdx > 0) {
                int zIdx = s.indexOf('Z', dotIdx);
                if (zIdx > 0) s = s.substring(0, dotIdx) + "Z";
            }
            if (s.length() < 19) return "";

            int year   = Integer.parseInt(s.substring(0, 4));
            int month  = Integer.parseInt(s.substring(5, 7));
            int day    = Integer.parseInt(s.substring(8, 10));
            int hour   = Integer.parseInt(s.substring(11, 13));
            int minute = Integer.parseInt(s.substring(14, 16));
            int second = Integer.parseInt(s.substring(17, 19));

            long postEpoch = approxEpochMs(year, month, day, hour, minute, second);
            long diffMs = System.currentTimeMillis() - postEpoch;

            if (diffMs < 0) return "now";
            long diffMin = diffMs / 60000L;
            long diffHour = diffMin / 60L;
            long diffDay = diffHour / 24L;

            if (diffDay > 0) return diffDay + "d";
            if (diffHour > 0) return diffHour + "h";
            if (diffMin > 0) return diffMin + "m";
            return "now";

        } catch (Exception e) {
            return "";
        }
    }

    private long approxEpochMs(int year, int month, int day, int hour, int min, int sec) {
        long days = 0;
        for (int y = 1970; y < year; y++) {
            days += isLeapYear(y) ? 366 : 365;
        }
        int[] dim = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (isLeapYear(year)) dim[1] = 29;
        for (int m = 1; m < month; m++) days += dim[m - 1];
        days += (day - 1);
        return (days * 86400L + hour * 3600L + min * 60L + sec) * 1000L;
    }

    private boolean isLeapYear(int y) {
        return (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0);
    }

    // -----------------------------------------------------------------------
    // Input
    // -----------------------------------------------------------------------

    protected void keyPressed(int keyCode) {
        int action = -1;
        try { action = getGameAction(keyCode); }
        catch (Exception e) { /* ok */ }

        if (action == FIRE) {
            if (selectedIndex >= 0 && selectedIndex < posts.size()) {
                String[] post = (String[]) posts.elementAt(selectedIndex);
                midlet.showPost(post[0], post[1], post[2]);
            }
            return;
        }

        if (action == UP) {
            if (posts != null && posts.size() > 0) {
                if (selectedIndex < 0) selectedIndex = 0;
                else if (selectedIndex > 0) selectedIndex--;
                ensureVisible(selectedIndex);
            }
            repaint();
            return;
        }

        if (action == DOWN) {
            if (posts != null && posts.size() > 0) {
                if (selectedIndex < 0) selectedIndex = 0;
                else if (selectedIndex < posts.size() - 1) selectedIndex++;
                ensureVisible(selectedIndex);
            }
            repaint();
            return;
        }

        if (keyCode == Canvas.KEY_NUM1) { midlet.showCreatePost(); }
        else if (keyCode == Canvas.KEY_NUM2) { loadPosts(); }
        else if (keyCode == Canvas.KEY_NUM3) { loadMorePage(); }
    }

    private void ensureVisible(int idx) {
        if (posts == null || idx < 0 || idx >= posts.size()) return;
        int top = HEADER_HEIGHT + 10;
        int bot = getHeight() - FOOTER_HEIGHT - 10;
        int contentH = bot - top;

        int itemY = idx * (CARD_HEIGHT + CARD_MARGIN);
        if (itemY < scrollOffset) {
            scrollOffset = itemY;
        } else if (itemY + CARD_HEIGHT > scrollOffset + contentH) {
            scrollOffset = itemY + CARD_HEIGHT - contentH;
        }
        if (scrollOffset < 0) scrollOffset = 0;
    }

    private void loadMorePage() {
        if (!isLoading && posts != null && posts.size() > 0) {
            currentPage++;
            isLoading = true;
            repaint();
            fetchPage(currentPage, true);
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
    // Animation
    // -----------------------------------------------------------------------

    private void startAnimationThread() {
        new Thread() {
            public void run() {
                while (true) {
                    try { Thread.sleep(30); } catch (InterruptedException e) { break; }
                    if (isLoading) repaint();
                }
            }
        }.start();
    }

    // -----------------------------------------------------------------------
    // CommandListener
    // -----------------------------------------------------------------------

    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            midlet.showHome();
        } else if (c == refreshCommand) {
            loadPosts();
        } else if (c == newPostCommand) {
            midlet.showCreatePost();
        } else if (c == loadMoreCommand) {
            loadMorePage();
        }
    }
}