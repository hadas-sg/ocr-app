package com.example.android.ocrexample;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.StringTokenizer;

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

        // Open dictionary file
        File dictionaryFile = new File(Environment.getExternalStorageDirectory().toString() + "/tessdata/dictionaryFile.txt");
        try {
            BufferedReader dictionaryReader = new BufferedReader(new FileReader(dictionaryFile));
            String line = dictionaryReader.readLine();

            while (line != null) {

                // Save current line to the db
                String[] words = line.split(" ");

                if (words.length == 2) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DICTIONARY_COLUMN_HEBREW_WORD, words[1]);
                    contentValues.put(DICTIONARY_COLUMN_ARABIC_WORD, words[0]);
                    db.insert(DICTIONARY_TABLE_NAME, null, contentValues);
                }

                line = dictionaryReader.readLine();
            }

            dictionaryReader.close();
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public String getHebrewTranslation(String arabicWord){
        SQLiteDatabase db = this.getReadableDatabase();
        String hebrewWord = "";
        String dlim = " .!@#$%^&*()_+-=?/~";

        try {
            //split text to lines
            String[] lines = arabicWord.split("\n\n");
            for (int i = 0; i < lines.length; i++)
            {
                //go over each word
                StringTokenizer st = new StringTokenizer(lines[i], dlim, true);
                while (st.hasMoreTokens()){
                    String word = st.nextToken();
                    if (!dlim.contains(word)) // actual word and not one of the delimiters
                    {
                        Cursor resultCursor = db.rawQuery("SELECT * FROM " + DICTIONARY_TABLE_NAME + " WHERE " +
                                DICTIONARY_COLUMN_ARABIC_WORD + " = \"" + word + "\"", null);
                        if (resultCursor != null) {
                            // move cursor to first row - Return first translation found
                            if (resultCursor.moveToFirst()) {
                                // Get String from Cursor
                                word = resultCursor.getString(resultCursor.getColumnIndex(DICTIONARY_COLUMN_HEBREW_WORD));
                            }
                        }

                    }
                    hebrewWord += word;
                }
                if (i > 0)
                    hebrewWord += "\n\n";
            }

            // if Cursor is contains results
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return hebrewWord;
    }
}
