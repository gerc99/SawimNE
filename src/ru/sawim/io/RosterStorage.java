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
        try {
            Cursor c = SawimApplication.getContext().getContentResolver()
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
            c.close();
        } catch (Exception e) {
            DebugLog.panic(e);
        }
    }

    public static String getAvatarHash(String uniqueUserId) {
        Cursor cursor = SawimApplication.getContext().getContentResolver().query(SawimProvider.AVATAR_HASHES_RESOLVER_URI, null,
                WHERE_CONTACT_ID, new String[]{uniqueUserId}, null);
        if (cursor.moveToFirst()) {
            do {
                String hash = cursor.getString(cursor.getColumnIndex(SawimProvider.AVATAR_HASH));
                return hash;
            } while (cursor.moveToNext());
        }
        cursor.close();
        return null;
    }
}
