package temporal;

import javax.microedition.rms.RecordStore;

/**
 * Persistent key-value store backed by MIDP RMS.
 *
 * v1.2 UPDATE UI S additions:
 *  - saveDraft() / getDraft() for compose-screen draft persistence.
 *  - flush() no-op stub for Temporal.pauseApp() compatibility.
 *
 * Record layout:
 *   1 = JWT token
 *   2 = username
 *   3 = last viewed post ID (for resume)
 *   4 = compose draft (unsent post text)
 */
public class RMSStore {

    private static final String STORE_NAME = "auth";

    // ---- JWT ----------------------------------------------------------------

    public static void saveJWT(String jwt) {
        saveRecord(1, jwt);
    }

    public static String getJWT() {
        return getRecord(1);
    }

    // ---- Username -----------------------------------------------------------

    public static void saveUsername(String username) {
        saveRecord(2, username);
    }

    public static String getUsername() {
        return getRecord(2);
    }

    // ---- Last post ID (for "continue reading") ------------------------------

    public static void saveLastPostId(String postId) {
        saveRecord(3, postId);
    }

    public static String getLastPostId() {
        return getRecord(3);
    }

    // ---- Draft (unsent compose text) ----------------------------------------

    /**
     * Save a compose-screen draft.
     * Pass {@code null} or an empty string to clear the draft.
     */
    public static void saveDraft(String draft) {
        saveRecord(4, draft);
    }

    /**
     * Retrieve the saved draft, or {@code null} if none exists.
     */
    public static String getDraft() {
        return getRecord(4);
    }

    // ---- Auth check helper --------------------------------------------------

    /** Returns true if a JWT is stored and non-empty. */
    public static boolean isLoggedIn() {
        String jwt = getJWT();
        return jwt != null && jwt.length() > 0;
    }

    // ---- Clear all (logout) -------------------------------------------------

    public static void clearAll() {
        try {
            RecordStore.deleteRecordStore(STORE_NAME);
        } catch (Exception e) {
            // Store may not exist; that's fine
        }
    }

    // ---- Flush (no-op for RMS) ----------------------------------------------

    /**
     * No-op flush — RMS writes are synchronous in MIDP 2.0.
     * Provided so Temporal.pauseApp() compiles without error.
     */
    public static void flush() {
        // RecordStore commits are immediate; nothing to flush.
    }

    // ---- Internal helpers ---------------------------------------------------

    private static void saveRecord(int id, String dataStr) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE_NAME, true);
            byte[] data = (dataStr != null ? dataStr : "").getBytes("UTF-8");
            // Pad store to required size
            while (rs.getNumRecords() < id) {
                rs.addRecord(new byte[0], 0, 0);
            }
            rs.setRecord(id, data, 0, data.length);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception ignored) {}
            }
        }
    }

    private static String getRecord(int id) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE_NAME, false);
            if (rs.getNumRecords() >= id) {
                byte[] data = rs.getRecord(id);
                if (data != null && data.length > 0) {
                    return new String(data, "UTF-8");
                }
            }
        } catch (Exception e) {
            // Store doesn't exist yet or record missing — normal first run
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception ignored) {}
            }
        }
        return null;
    }
}