package eu.faircode.netguard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "NetGuard.Database";

    private static final String DB_NAME = "Netguard";
    private static final int DB_VERSION = 7;

    private static List<LogChangedListener> logChangedListeners = new ArrayList<LogChangedListener>();

    private Context mContext;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Creating database " + DB_NAME + ":" + DB_VERSION);
        createTableLog(db);
    }

    private void createTableLog(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE log (" +
                " ID INTEGER PRIMARY KEY AUTOINCREMENT" +
                ", time INTEGER NOT NULL" +
                ", version INTEGER NULL" +
                ", protocol INTEGER NULL" +
                ", flags TEXT" +
                ", saddr TEXT" +
                ", sport INTEGER NULL" +
                ", daddr TEXT" +
                ", dport INTEGER NULL" +
                ", uid INTEGER NULL" +
                ", allowed INTEGER NULL" +
                ", connection INTEGER NULL" +
                ", interactive INTEGER NULL" +
                ");");
        db.execSQL("CREATE INDEX idx_log_time ON log(time)");
        db.execSQL("CREATE INDEX idx_log_source ON log(saddr, sport)");
        db.execSQL("CREATE INDEX idx_log_dest ON log(daddr, dport)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading from version " + oldVersion + " to " + newVersion);

        db.beginTransaction();
        try {
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE log ADD COLUMN version INTEGER NULL");
                db.execSQL("ALTER TABLE log ADD COLUMN protocol INTEGER NULL");
                db.execSQL("ALTER TABLE log ADD COLUMN uid INTEGER NULL");
                oldVersion = 2;
            }
            if (oldVersion < 3) {
                db.execSQL("ALTER TABLE log ADD COLUMN port INTEGER NULL");
                db.execSQL("ALTER TABLE log ADD COLUMN flags TEXT");
                oldVersion = 3;
            }
            if (oldVersion < 4) {
                db.execSQL("ALTER TABLE log ADD COLUMN connection INTEGER NULL");
                oldVersion = 4;
            }
            if (oldVersion < 5) {
                db.execSQL("ALTER TABLE log ADD COLUMN interactive INTEGER NULL");
                oldVersion = 5;
            }
            if (oldVersion < 6) {
                db.execSQL("ALTER TABLE log ADD COLUMN allowed INTEGER NULL");
                oldVersion = 6;
            }
            if (oldVersion < 7) {
                db.execSQL("DROP TABLE log");
                createTableLog(db);
                oldVersion = 7;
            }

            db.setVersion(DB_VERSION);

            db.setTransactionSuccessful();
        } catch (Throwable ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        } finally {
            db.endTransaction();
        }
    }

    // Location

    public DatabaseHelper insertLog(Packet packet, int connection, boolean interactive) {
        synchronized (mContext.getApplicationContext()) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues cv = new ContentValues();
            cv.put("time", packet.time);
            cv.put("version", packet.version);

            if (packet.protocol < 0)
                cv.putNull("protocol");
            else
                cv.put("protocol", packet.protocol);

            cv.put("flags", packet.flags);

            cv.put("saddr", packet.saddr);
            if (packet.sport < 0)
                cv.putNull("sport");
            else
                cv.put("sport", packet.sport);

            cv.put("daddr", packet.daddr);
            if (packet.dport < 0)
                cv.putNull("dport");
            else
                cv.put("dport", packet.dport);

            if (packet.uid < 0)
                cv.putNull("uid");
            else
                cv.put("uid", packet.uid);

            cv.put("allowed", packet.allowed ? 1 : 0);

            cv.put("connection", connection);
            cv.put("interactive", interactive ? 1 : 0);

            if (db.insert("log", null, cv) == -1)
                Log.e(TAG, "Insert log failed");
        }

        for (LogChangedListener listener : logChangedListeners)
            try {
                listener.onChanged();
            } catch (Throwable ex) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            }

        return this;
    }

    public DatabaseHelper clear() {
        synchronized (mContext.getApplicationContext()) {
            SQLiteDatabase db = this.getReadableDatabase();
            db.delete("log", null, new String[]{});
            db.execSQL("VACUUM");
        }

        for (LogChangedListener listener : logChangedListeners)
            try {
                listener.onChanged();
            } catch (Throwable ex) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            }

        return this;
    }

    public Cursor getLog() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT ID AS _id, * FROM log";
        query += " ORDER BY time DESC";
        return db.rawQuery(query, new String[]{});
    }

    public static void addLogChangedListener(LogChangedListener listener) {
        logChangedListeners.add(listener);
    }

    public static void removeLocationChangedListener(LogChangedListener listener) {
        logChangedListeners.remove(listener);
    }

    public interface LogChangedListener {
        void onChanged();
    }
}
