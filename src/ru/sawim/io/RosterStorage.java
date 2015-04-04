package ru.sawim.io;

import android.content.ContentValues;
import android.database.Cursor;
import ru.sawim.SawimApplication;
import ru.sawim.modules.DebugLog;

/**
 * Created by gerc on 18.09.2014.
 */
public class RosterStorage {

    private static final String WHERE_CONTACT_ID = SawimProvider.CONTACT_ID + " = ?";

    private RosterStorage() {
    }

    public static void updateAvatarHash(String uniqueUserId, String hash) {
        Cursor c = null;
        try {
            c = SawimApplication.getContext().getContentResolver()
                    .query(SawimProvider.AVATAR_HASHES_RESOLVER_URI, null, WHERE_CONTACT_ID,
                            new String[]{uniqueUserId}, null);
            if (c.getCount() > 0) {
                ContentValues values = new ContentValues();
                values.put(SawimProvider.AVATAR_HASH, hash);
                SawimApplication.getContext().getContentResolver()
                        .update(SawimProvider.AVATAR_HASHES_RESOLVER_URI, values, WHERE_CONTACT_ID, new String[]{uniqueUserId});
            } else {
                ContentValues values = new ContentValues();
                values.put(SawimProvider.CONTACT_ID, uniqueUserId);
                values.put(SawimProvider.AVATAR_HASH, hash);
                SawimApplication.getContext().getContentResolver().insert(SawimProvider.AVATAR_HASHES_RESOLVER_URI, values);
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public static String getAvatarHash(String uniqueUserId) {
        String hash = null;
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getContext().getContentResolver().query(SawimProvider.AVATAR_HASHES_RESOLVER_URI, null,
                    WHERE_CONTACT_ID, new String[]{uniqueUserId}, null);

            if (cursor.moveToFirst()) {
                do {
                    hash = cursor.getString(cursor.getColumnIndex(SawimProvider.AVATAR_HASH));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (cursor != null){
                cursor.close();
            }
        }
        return hash;
    }
}
