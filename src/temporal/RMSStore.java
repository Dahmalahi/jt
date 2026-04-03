package temporal;

import javax.microedition.rms.RecordStore;

public class RMSStore {

    private static final String STORE_NAME = "auth";

    public static void saveJWT(String jwt) {
        saveRecord(1, jwt);
    }

    public static String getJWT() {
        return getRecord(1);
    }

    public static void saveUsername(String username) {
        saveRecord(2, username);
    }

    public static String getUsername() {
        return getRecord(2);
    }

    private static void saveRecord(int id, String dataStr) {
        try {
            RecordStore rs = RecordStore.openRecordStore(STORE_NAME, true);
            byte[] data = dataStr.getBytes("UTF-8");
            while (rs.getNumRecords() < id) {
                rs.addRecord(new byte[0], 0, 0);
            }
            rs.setRecord(id, data, 0, data.length);
            rs.closeRecordStore();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getRecord(int id) {
        try {
            RecordStore rs = RecordStore.openRecordStore(STORE_NAME, false);
            if (rs != null) {
                if (rs.getNumRecords() >= id) {
                    byte[] data = rs.getRecord(id);
                    rs.closeRecordStore();
                    if (data != null && data.length > 0) {
                        return new String(data, "UTF-8");
                    }
                }
                rs.closeRecordStore();
            }
        } catch (Exception e) {}
        return null;
    }

    public static void clearAll() {
        try {
            RecordStore.deleteRecordStore(STORE_NAME);
        } catch (Exception e) {}
    }
}
