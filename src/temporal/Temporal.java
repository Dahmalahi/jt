package temporal;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

/**
 * Main MIDlet entry point for Temporal.
 *
 * v1.2 UPDATE UI S changes:
 *  - showCreatePostWithMention(String) added for reply flow from PostScreen.
 *  - showAlert() overload with custom timeout added.
 *  - showHome() only calls loadPosts() when homeScreen is newly created or
 *    explicitly stale, preventing a redundant reload on every back-navigation.
 *  - Screen invalidation helpers: invalidateHome(), invalidateProfile().
 *  - Null-safe getDisplay() guard (returns early if display not yet assigned).
 *  - pauseApp() persists lightweight state via RMSStore.
 *  - destroyApp() calls notifyDestroyed() only when unconditional is true,
 *    matching the MIDP 2.0 lifecycle contract correctly.
 *  - exitMIDlet() separated from destroyApp() to avoid double-notification.
 *  - All navigation methods guard against null display.
 */
public class Temporal extends MIDlet {

    // -----------------------------------------------------------------------
    // Screen instances  (null = not yet created / invalidated)
    // -----------------------------------------------------------------------

    private Display       display;
    private LoginScreen   loginScreen;
    private HomeScreen    homeScreen;
    private ProfileScreen profileScreen;
    private AboutScreen   aboutScreen;

    /**
     * True when homeScreen exists but its post list is known to be stale
     * (e.g. after the user creates or deletes a post).
     */
    private boolean homeStale = false;

    // -----------------------------------------------------------------------
    // MIDlet lifecycle
    // -----------------------------------------------------------------------

    public void startApp() {
        // display is assigned once; startApp() may be called multiple times
        // if the MIDlet is paused and resumed.
        if (display == null) {
            display = Display.getDisplay(this);
        }

        if (RMSStore.isLoggedIn()) {
            showHome();
        } else {
            showLogin();
        }
    }

    public void pauseApp() {
        // Persist any in-memory state that is cheap to save.
        // RMSStore.flush() is a no-op stub if the implementation writes
        // through immediately, but included for completeness.
        RMSStore.flush();
    }

    public void destroyApp(boolean unconditional) {
        // Release screen references so the GC can collect them.
        loginScreen   = null;
        homeScreen    = null;
        profileScreen = null;
        aboutScreen   = null;

        // Per MIDP 2.0 spec: call notifyDestroyed() only when the platform
        // requested unconditional termination, or from exitMIDlet().
        // Calling it here for unconditional avoids a double-call when
        // exitMIDlet() is the initiator (it calls destroyApp then notifies).
        if (unconditional) {
            notifyDestroyed();
        }
    }

    // -----------------------------------------------------------------------
    // Display accessor
    // -----------------------------------------------------------------------

    /**
     * Returns the current {@link Display}.
     * May return {@code null} before {@link #startApp()} has been called.
     */
    public Display getDisplay() {
        return display;
    }

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    /** Show the login screen and invalidate user-specific screens. */
    public void showLogin() {
        if (display == null) return;

        // Invalidate screens that contain user-specific data.
        homeScreen    = null;
        profileScreen = null;
        homeStale     = false;

        if (loginScreen == null) {
            loginScreen = new LoginScreen(this);
        }
        display.setCurrent(loginScreen);
    }

    /**
     * Show the home / timeline screen.
     *
     * Posts are loaded only when:
     *   (a) the HomeScreen has just been constructed, or
     *   (b) {@link #invalidateHome()} has been called since the last load.
     */
    public void showHome() {
        if (display == null) return;

        boolean freshlyCreated = (homeScreen == null);
        if (freshlyCreated) {
            homeScreen = new HomeScreen(this);
        }

        if (freshlyCreated || homeStale) {
            homeScreen.loadPosts();
            homeStale = false;
        }

        display.setCurrent(homeScreen);
    }

    /**
     * Mark the home timeline as stale so the next call to {@link #showHome()}
     * triggers a network reload.  Call this after creating or deleting a post.
     */
    public void invalidateHome() {
        homeStale = true;
    }

    /**
     * Show a fresh {@link PostScreen} for the given post.
     * Always creates a new instance because content is post-specific.
     */
    public void showPost(String id, String content, String author) {
        if (display == null) return;
        PostScreen postScreen = new PostScreen(this, id, content, author);
        display.setCurrent(postScreen);
    }

    /**
     * Show the compose screen with an empty body.
     */
    public void showCreatePost() {
        if (display == null) return;
        CreatePostScreen createPostScreen = new CreatePostScreen(this, "");
        display.setCurrent(createPostScreen);
    }

    /**
     * Show the compose screen pre-filled with a mention (for the reply flow).
     *
     * @param mention Text to pre-fill, e.g. {@code "@alice "}.
     */
    public void showCreatePostWithMention(String mention) {
        if (display == null) return;
        String pre = (mention != null) ? mention : "";
        CreatePostScreen createPostScreen = new CreatePostScreen(this, pre);
        display.setCurrent(createPostScreen);
    }

    /**
     * Show the logged-in user's profile screen.
     * Re-uses the cached instance; reloads posts on every visit so the
     * profile always reflects the latest state.
     */
    public void showProfile() {
        if (display == null) return;
        if (profileScreen == null) {
            profileScreen = new ProfileScreen(this);
        }
        profileScreen.loadPosts();
        display.setCurrent(profileScreen);
    }

    /**
     * Invalidate the cached profile screen so it is rebuilt on the next visit.
     * Call this after a username / bio change.
     */
    public void invalidateProfile() {
        profileScreen = null;
    }

    /** Show the static About screen. */
    public void showAbout() {
        if (display == null) return;
        if (aboutScreen == null) {
            aboutScreen = new AboutScreen(this);
        }
        display.setCurrent(aboutScreen);
    }

    // -----------------------------------------------------------------------
    // Alert helpers
    // -----------------------------------------------------------------------

    /**
     * Show a modal informational alert that requires user dismissal.
     * Returns to whatever screen is currently displayed when dismissed.
     *
     * @param title Short alert title.
     * @param msg   Body message.
     */
    public void showAlert(String title, String msg) {
        showAlert(title, msg, Alert.FOREVER);
    }

    /**
     * Show a timed informational alert.
     *
     * @param title   Short alert title.
     * @param msg     Body message.
     * @param timeout Milliseconds before auto-dismiss, or {@link Alert#FOREVER}.
     */
    public void showAlert(String title, String msg, int timeout) {
        if (display == null) return;

        // Clamp: Alert.FOREVER is -1; any other negative value is nonsensical.
        int safeTimeout = (timeout < 0) ? Alert.FOREVER : timeout;

        Alert alert = new Alert(
                (title != null) ? title : "",
                (msg   != null) ? msg   : "",
                null,
                AlertType.INFO);
        alert.setTimeout(safeTimeout);

        // Return to whichever screen is currently active after dismissal.
        Displayable current = display.getCurrent();
        if (current != null && !(current instanceof Alert)) {
            display.setCurrent(alert, current);
        } else {
            // Fallback: no sensible "next" screen available; just show the alert.
            display.setCurrent(alert);
        }
    }

    // -----------------------------------------------------------------------
    // Exit
    // -----------------------------------------------------------------------

    /**
     * Cleanly terminate the MIDlet.
     * Calls {@link #destroyApp(boolean)} with {@code false} so that method
     * does NOT call {@link #notifyDestroyed()} a second time.
     */
    public void exitMIDlet() {
        destroyApp(false);   // false = we handle notifyDestroyed() ourselves
        notifyDestroyed();
    }
}