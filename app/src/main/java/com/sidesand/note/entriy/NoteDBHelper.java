package com.sidesand.note.entriy;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NoteDBHelper extends SQLiteOpenHelper {

    private static final String TAG = "NoteDBHelper";

    private static volatile NoteDBHelper noteDBHelper;

    private SQLiteDatabase mRDB = null;
    private SQLiteDatabase mWDB = null;

    private static final String DB_NAME = "note.db";
    public static final String TABLE_NOTE = "notes";
    private static final String TABLE_NAME_VERSION = "notes_version";
    public static final String TABLE_TAG = "tags";

    // Notes 表列名
    public static final String NOTE_ID = "note_id";
    private static final String TITLE = "title";
    private static final String CONTENT = "content";
    public static final String CREATE_TIME = "create_time";
    private static final String UPDATE_TIME = "update_time";
    private static final String TAG_NAME = "tag_Name";
    private static final String TAG_ID = "tag_id";
    private static final String MODE = "mode";

    private static final String DB_TAG_ID = "tag_id";
    private static final String DB_TAG_NAME = "tag_name";


    public NoteDBHelper(@Nullable Context context) {
        super(context, DB_NAME, null, 1);
    }

    public static NoteDBHelper getInstance(Context context) {
        if (noteDBHelper == null) {
            synchronized (NoteDBHelper.class) {
                if (noteDBHelper == null) {
                    noteDBHelper = new NoteDBHelper(context);
                }
            }
        }
        return noteDBHelper;
    }

    public SQLiteDatabase openReadLink() {
        if (mRDB == null || !mRDB.isOpen()) {
            mRDB = getReadableDatabase();
        }
        return mRDB;
    }

    public SQLiteDatabase openWriteLink() {
        if (mWDB == null || !mWDB.isOpen()) {
            mWDB = getWritableDatabase();
        }
        return mWDB;
    }

    public void closeLink() {
        if (mRDB != null && mRDB.isOpen()) {
            mRDB.close();
            mRDB = null;
        }

        if (mWDB != null && mWDB.isOpen()) {
            mWDB.close();
            mWDB = null;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NOTE + "(" +
                NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                TITLE + " TEXT," +
                CONTENT + " TEXT," +
                CREATE_TIME + " TEXT," +
                UPDATE_TIME + " TEXT," +
                TAG_NAME + " TEXT," +
                TAG_ID + " INTEGER," +
                MODE + " INTEGER);";
        db.execSQL(sql);

        String sql2 = "CREATE TABLE IF NOT EXISTS " + TABLE_TAG + "(" +
                DB_TAG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                DB_TAG_NAME + " TEXT);";
        db.execSQL(sql2);

        // 检查是否存在 "未分类" 标签
        String checkSql = "SELECT COUNT(*) FROM " + TABLE_TAG + " WHERE " + DB_TAG_NAME + " = ?";
        Cursor cursor = db.rawQuery(checkSql, new String[]{"未分类"});
        if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
            // 如果不存在 "未分类" 标签，则插入
            ContentValues values = new ContentValues();
            values.put(DB_TAG_NAME, "未分类");
            db.insert(TABLE_TAG, null, values);
        }
        cursor.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @SuppressLint("Range")
    public long insertTag(String tagName) {
        if (tagName == null || tagName.isEmpty()) {
            return 0;
        }

        long id = 0;
        String sql = "SELECT * FROM " + TABLE_TAG + " WHERE " + DB_TAG_NAME + " = ?";

        try (Cursor cursor = mWDB.rawQuery(sql, new String[]{tagName})) {
            if (cursor.moveToFirst()) {
                id = cursor.getLong(cursor.getColumnIndex(DB_TAG_ID));
            } else {
                ContentValues values = new ContentValues();
                values.put(DB_TAG_NAME, tagName);
                id = mWDB.insert(TABLE_TAG, null, values);
            }
        } catch (Exception e) {
            // 处理异常，例如记录日志或抛出自定义异常
            e.printStackTrace();
            throw new RuntimeException("Database operation failed", e);
        }

        return id;
    }

    public boolean isTagExist(String tagName) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_TAG + " WHERE " + DB_TAG_NAME + " = ?";
        Cursor cursor = mWDB.rawQuery(sql, new String[]{tagName});
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count > 0;
    }


    public long insert(Note note) {
        try {
            mWDB.beginTransaction();
            ContentValues values = new ContentValues();
            values.put(TITLE, note.getTitle());
            values.put(CONTENT, note.getContent());
            values.put(CREATE_TIME, note.getCreateTime());
            values.put(UPDATE_TIME, note.getUpdateTime());
            values.put(TAG_NAME, note.getTagName());

            ContentValues tagValues = new ContentValues();
            tagValues.put(DB_TAG_NAME, note.getTagName());
            if (!isTagExist(note.getTagName())) {
                mWDB.insert(TABLE_TAG, null, tagValues);
            }
            mWDB.setTransactionSuccessful();
            //            Log.d(TAG, "insert: " + "id =" + id + ",title = " + note.getTitle() + ",content = " + note.getContent());
            return mWDB.insert(TABLE_NOTE, null, values);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mWDB.endTransaction();
        }
        return 0;
    }


/*    public boolean insert(Note note) {
        ContentValues values = new ContentValues();
        values.put(TITLE, note.getTitle());
        values.put(CONTENT, note.getContent());
        values.put(CREATE_TIME, note.getCreateTime());
        values.put(UPDATE_TIME, note.getUpdateTime());
        values.put(TAG, note.getTagName());
        values.put(TAG_ID, note.getTagId());
        values.put(MODE, note.getMode());
        try {
            mWDB.beginTransaction();
            mWDB.insert(TABLE_NAME, null, values);
            mWDB.insert(TABLE_NAME_VERSION, null, values);
            mWDB.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mWDB.endTransaction();
        }
        return true;
    }*/

    @SuppressLint("Range")
    public Note getNote(String noteId) {
        Note note = null;
        String sql = "SELECT * FROM " + TABLE_NOTE + " WHERE " + NOTE_ID + " = ?";
        Cursor cursor = mWDB.rawQuery(sql, new String[]{noteId});
        if (cursor.moveToFirst()) {
            note = new Note();
            note.setNoteId(cursor.getLong(cursor.getColumnIndex(NOTE_ID)));
            note.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
            note.setContent(cursor.getString(cursor.getColumnIndex(CONTENT)));
            note.setCreateTime(cursor.getString(cursor.getColumnIndex(CREATE_TIME)));
            note.setUpdateTime(cursor.getString(cursor.getColumnIndex(UPDATE_TIME)));
            note.setTagName(cursor.getString(cursor.getColumnIndex(TAG_NAME)));
            note.setMode(cursor.getInt(cursor.getColumnIndex(MODE)));
        }
        cursor.close();
        return note;
    }

    public long deleteById(long id) {
        return mWDB.delete(TABLE_NOTE, NOTE_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void deleteAll() {
        mWDB.delete(TABLE_NOTE, "1=1", null);
    }

    public int update(Note note) {// 只填写更新的内容
        ContentValues values = new ContentValues();
        try {
            mWDB.beginTransaction();
            values.put(TITLE, note.getTitle());
            values.put(CONTENT, note.getContent());
            values.put(UPDATE_TIME, note.getUpdateTime());
            values.put(TAG_NAME, note.getTagName());
            int id = mWDB.update(TABLE_NOTE, values, NOTE_ID + " = ?", new String[]{String.valueOf(note.getNoteId())});
//            Log.d(TAG, "update: " + "id =" + note.getNoteId() + ",title = " + note.getTitle() + ",content = " + note.getContent());
            mWDB.setTransactionSuccessful();
            return id;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mWDB.endTransaction();
        }
        return 0;
    }


    @SuppressLint("Range")
    public List<Note> getAllNotes() {
        List<Note> noteList = new ArrayList<>();
        Cursor cursor = mWDB.query(TABLE_NOTE, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            Note note = new Note();
            note.setNoteId(cursor.getLong(cursor.getColumnIndex(NOTE_ID)));
            note.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
            note.setContent(cursor.getString(cursor.getColumnIndex(CONTENT)));
            note.setCreateTime(cursor.getString(cursor.getColumnIndex(CREATE_TIME)));
            note.setUpdateTime(cursor.getString(cursor.getColumnIndex(UPDATE_TIME)));
            note.setTagName(cursor.getString(cursor.getColumnIndex(TAG_NAME)));
            note.setMode(cursor.getInt(cursor.getColumnIndex(MODE)));
            noteList.add(note);
        }
        cursor.close();
        return noteList;

    }


    @SuppressLint("Range")
    public List<String> getAllTags() {
        List<String> tagList = new ArrayList<>();
        Cursor cursor = mWDB.query(TABLE_TAG, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            String tagName = cursor.getString(cursor.getColumnIndex("tag_name"));
            if (tagName != null && !tagName.isEmpty()) {
                tagList.add(tagName);
            }
        }
        cursor.close();
        return tagList;
    }

    @SuppressLint("Range")
    public List<Note> searchNotesByTitleOrContent(String query) {
        List<Note> noteList = new ArrayList<>();
        String[] columns = {NOTE_ID, TITLE, CONTENT, CREATE_TIME, UPDATE_TIME, TAG_NAME, MODE};
        String selection = TITLE + " LIKE ? OR " + CONTENT + " LIKE ?";
        String[] selectionArgs = {"%" + query + "%", "%" + query + "%"};

        try (Cursor cursor = mWDB.query(TABLE_NOTE, columns, selection, selectionArgs, null, null, null)) {
            while (cursor.moveToNext()) {
                Note note = new Note();
                note.setNoteId(cursor.getLong(cursor.getColumnIndex(NOTE_ID)));
                note.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
                note.setContent(cursor.getString(cursor.getColumnIndex(CONTENT)));
                note.setCreateTime(cursor.getString(cursor.getColumnIndex(CREATE_TIME)));
                note.setUpdateTime(cursor.getString(cursor.getColumnIndex(UPDATE_TIME)));
                note.setTagName(cursor.getString(cursor.getColumnIndex(TAG_NAME)));
                note.setMode(cursor.getInt(cursor.getColumnIndex(MODE)));
                noteList.add(note);
            }
        }
        return noteList;
    }


    public void deleteTag(String tagToDelete) {
        String sql = "DELETE FROM " + TABLE_TAG + " WHERE " + DB_TAG_NAME + " = ?";
        mWDB.execSQL(sql, new String[]{tagToDelete});
    }
}
