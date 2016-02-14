package com.example.android.ocrexample;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;

/**
 * Created by hadas.sayag on 14/02/2016.
 */
public class DBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "OCRAppDb.db";
    public static final int DATABASE_VERSION = 1;
    public static final String DICTIONARY_TABLE_NAME = "heb_ara_dictionary";
    public static final String DICTIONARY_COLUMN_ID = "id";
    public static final String DICTIONARY_COLUMN_HEBREW_WORD = "hebrew_word";
    public static final String DICTIONARY_COLUMN_ARABIC_WORD = "arabic_word";

    private HashMap hp;

    public DBHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(
                "CREATE TABLE " + DICTIONARY_TABLE_NAME +
                        "(" + DICTIONARY_COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        DICTIONARY_COLUMN_HEBREW_WORD + " text, " +
                        DICTIONARY_COLUMN_ARABIC_WORD + " text)"
        );

        initDB(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS " + DICTIONARY_TABLE_NAME);
        onCreate(db);
    }

    private void initDB(SQLiteDatabase db) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DICTIONARY_COLUMN_HEBREW_WORD, "shalom");
        contentValues.put(DICTIONARY_COLUMN_ARABIC_WORD, "salam");
        db.insert(DICTIONARY_TABLE_NAME, null, contentValues);
    }

    public String getHebrewTranslation(String arabicWord){
        SQLiteDatabase db = this.getReadableDatabase();
        String hebrewWord = "No translation found";

        try {
            Cursor resultCursor = db.rawQuery("SELECT * FROM " + DICTIONARY_TABLE_NAME + " WHERE " +
                    DICTIONARY_COLUMN_ARABIC_WORD + " = \"" + arabicWord + "\"", null);


            // if Cursor is contains results
            if (resultCursor != null) {
                // move cursor to first row - Return first translation found
                if (resultCursor.moveToFirst()) {
                    // Get String from Cursor
                    hebrewWord = resultCursor.getString(resultCursor.getColumnIndex(DICTIONARY_COLUMN_HEBREW_WORD));
                }
            }
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return hebrewWord;
    }

    public String getArabicTranslation(String hebrewWord){
        SQLiteDatabase db = this.getReadableDatabase();
        String arabicWord = "No translation found";

        try {
            Cursor resultCursor = db.rawQuery("SELECT * FROM " + DICTIONARY_TABLE_NAME + " WHERE " +
                    DICTIONARY_COLUMN_HEBREW_WORD + " = \"" + hebrewWord + "\"", null);


            // if Cursor is contains results
            if (resultCursor != null) {
                // move cursor to first row - Return first translation found
                if (resultCursor.moveToFirst()) {
                    // Get String from Cursor
                    arabicWord = resultCursor.getString(resultCursor.getColumnIndex(DICTIONARY_COLUMN_ARABIC_WORD));
                }
            }
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return arabicWord;
    }
}
