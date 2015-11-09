package com.teo.ttasks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import timber.log.Timber;

/**
 * Created by Teo on 2015-02-03.
 * TasksDBAdapter
 * Based on: http://www.mysamplecode.com/2012/07/android-listview-cursoradapter-sqlite.html
 */

public class TasksDBAdapter {
    public static final String KEY_ROWID = "_id";
    public static final String KEY_USER = "user";
    public static final String KEY_TASKLIST = "tasklist";
    public static final String KEY_TITLE = "title";
    public static final String KEY_STATUS = "status";
    public static final String KEY_DUE = "due";

    private static final String DATABASE_NAME = "Tasks";
    private static final String SQLITE_TABLE = "TasksTable";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_CREATE =
            "CREATE TABLE if not exists " + SQLITE_TABLE + " (" +
                    KEY_ROWID + " integer PRIMARY KEY autoincrement," +
                    KEY_USER + "," +
                    KEY_TASKLIST + "," +
                    KEY_TITLE + "," +
                    KEY_STATUS + "," +
                    KEY_DUE + "," +
                    " UNIQUE (" + KEY_TITLE + "));";
    private final Context mCtx;
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    public TasksDBAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    public TasksDBAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        if (mDbHelper != null) {
            mDbHelper.close();
        }
    }

    public long insertTask(String user, String tasklist, String title, String status, String due) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_USER, user);
        initialValues.put(KEY_TASKLIST, tasklist);
        initialValues.put(KEY_TITLE, title);
        initialValues.put(KEY_STATUS, status);
        initialValues.put(KEY_DUE, due);
        return mDb.insert(SQLITE_TABLE, null, initialValues);
    }

    public boolean deleteAllTasks() {
        int doneDelete = 0;
        doneDelete = mDb.delete(SQLITE_TABLE, null, null);
        // Log number of tasks deleted
        //Log.w(TAG, Integer.toString(doneDelete));
        return doneDelete > 0;
    }

    public Cursor fetchTasksByUser(String user) throws SQLException {
        Timber.w(user);
        Cursor mCursor;
        if (user == null || user.length() == 0) {
            mCursor = mDb.query(SQLITE_TABLE, new String[]{
                    KEY_ROWID,
                    KEY_USER,
                    KEY_TASKLIST,
                    KEY_TITLE,
                    KEY_STATUS,
                    KEY_DUE
            }, null, null, null, null, null);
        } else {
            mCursor = mDb.query(true, SQLITE_TABLE, new String[]{
                    KEY_ROWID,
                    KEY_USER,
                    KEY_TASKLIST,
                    KEY_TITLE,
                    KEY_STATUS,
                    KEY_DUE
            }, KEY_USER + " like '%" + user + "%'", null, null, null, null, null);
        }
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public Cursor fetchAllTasks() {
        Cursor mCursor = mDb.query(SQLITE_TABLE, new String[]{
                KEY_ROWID,
                KEY_USER,
                KEY_TASKLIST,
                KEY_TITLE,
                KEY_STATUS,
                KEY_DUE
        }, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Timber.w(DATABASE_CREATE);
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Timber.w("Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + SQLITE_TABLE);
            onCreate(db);
        }
    }
}