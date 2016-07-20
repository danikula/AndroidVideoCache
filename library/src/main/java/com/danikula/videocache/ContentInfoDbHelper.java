package com.danikula.videocache;

import com.danikula.videocache.ContentInfoContract.ContentInfoEntry;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * A {@link SQLiteOpenHelper) for managing the databases and tables related to caching content info
 * into a {@link SQLiteDatabase).
 *
 * @author Gareth Blake Hall (gbhall.com).
 */
public class ContentInfoDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "ContentInfo.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + ContentInfoEntry.TABLE_NAME + " (" +
                    ContentInfoEntry._ID + " INTEGER PRIMARY KEY," +
                    ContentInfoEntry.COLUMN_NAME_URL + TEXT_TYPE + COMMA_SEP +
                    ContentInfoEntry.COLUMN_NAME_MIME + TEXT_TYPE + COMMA_SEP +
                    ContentInfoEntry.COLUMN_NAME_LENGTH + TEXT_TYPE +
            " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + ContentInfoEntry.TABLE_NAME;

    public ContentInfoDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
