package com.example.android.ocrexample;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int SELECT_PICTURE = 1;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1;

    ProcessTextWithOCR textProcessorTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set settings listener
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        setCurrLanguage(sharedPreferences);

        textProcessorTask = null;

        // Check permissions to sdcard in android 6
        checkIfStoragePermissionGranted();
    }

    public void checkIfStoragePermissionGranted() {

        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_READ_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Restart the process to add the permissions (Known Bug of android 6)
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
                else
                {
                    // permission denied, exit the application
                    AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
                    dlgAlert.setMessage("This application can't run without permissions to the external storage.\n" +
                            "If you marked \"Never ask again\" at the permissions dialog you should reinstall the " +
                            "application to grant permissions.\n" +
                            "Press OK to exit the application.");
                    dlgAlert.setTitle("Permission Denied");
                    dlgAlert.setPositiveButton("Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //dismiss the dialog
                                    dialog.cancel();
                                    MainActivity.this.finish();
                                    System.exit(0);
                                }
                            });
                    dlgAlert.setCancelable(false);
                    dlgAlert.create().show();
                }
                return;
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getResources().getString(R.string.pref_lang_key).equals(key)) {
            setCurrLanguage(sharedPreferences);
        }
    }

    public void openGallery(View view) {

        if (textProcessorTask != null && textProcessorTask.getStatus() == AsyncTask.Status.RUNNING) {
            TextView resulTextView = (TextView) findViewById(R.id.resulTextView);
            resulTextView.setText("Still processing image... Please wait for process to finish before changing image");
        }
        else {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
        }
    }

    public void openCamera(View view) {

        if (textProcessorTask != null && textProcessorTask.getStatus() == AsyncTask.Status.RUNNING) {
            TextView resulTextView = (TextView) findViewById(R.id.resulTextView);
            resulTextView.setText("Still processing image... Please wait for process to finish before changing image");
        }
        else {
            // TODO: Open Camera...

            TextView textViewToUpdate = (TextView) findViewById(R.id.resulTextView);
            textViewToUpdate.setText("Not Implemented!");
            ImageView imageViewToUpdate = (ImageView) findViewById(R.id.mainImageView);
            imageViewToUpdate.setImageResource(android.R.color.transparent);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                ImageView imageViewToUpdate = (ImageView)findViewById(R.id.mainImageView);
                imageViewToUpdate.setImageURI(selectedImageUri);
                textProcessorTask = new ProcessTextWithOCR(this);
                textProcessorTask.execute(selectedImageUri);
            }
        }
    }

    private void setCurrLanguage(SharedPreferences sharedPreferences) {
        TextView currentLanguageTextView = (TextView)findViewById(R.id.textViewCurrLanguage);
        String currLanguageString = getResources().getString(R.string.current_language_text);
        String languageCode = sharedPreferences.getString(getResources().getString(R.string.pref_lang_key),
                getResources().getString(R.string.pref_lang_default));

        // Get the arrays used by the ListPreference
        CharSequence[] languageCodesArray = getApplicationContext().getResources().getTextArray(R.array.language_code);
        CharSequence[] languageValuesArray = getApplicationContext().getResources().getTextArray(R.array.language);

        // Loop and find index to get the title of the language code
        int len = languageCodesArray.length;
        for (int i = 0; i < len; i++) {
            if (languageCodesArray[i].equals(languageCode)) {
                currLanguageString += (String) languageValuesArray[i];
            }
        }

        currentLanguageTextView.setText(currLanguageString);
    }

    private class ProcessTextWithOCR extends AsyncTask<Uri, Void, String> {

        private Context context;
        private String language;

        public ProcessTextWithOCR(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(Uri... uris) {

            String textInImage = "";

            if (uris.length == 1) {
                TessBaseAPI ocrAPI = new TessBaseAPI();

                try {
                    InputStream imageStream = getContentResolver().openInputStream(uris[0]);
                    Bitmap bitmapFile = BitmapFactory.decodeStream(imageStream);
                    imageStream.close();

                    ocrAPI.init(Environment.getExternalStorageDirectory().getAbsolutePath(), language);
                    ocrAPI.setImage(bitmapFile);
                    textInImage = ocrAPI.getUTF8Text();

                }
                catch (Exception e) {
                    System.err.println(e.getMessage());
                }
                finally {

                    ocrAPI.end();
                }
            }

            return textInImage;
        }

        @Override
        protected void onPreExecute() {
            TextView resulTextView = (TextView)findViewById(R.id.resulTextView);
            resulTextView.setText("Processing image to get text...");

            // Get the language from the settings
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.context);
            language = sharedPrefs.getString(getResources().getString(R.string.pref_lang_key),
                    getResources().getString(R.string.pref_lang_default));
        }

        @Override
        protected void onPostExecute(String textInImage) {
            TextView resulTextView = (TextView) findViewById(R.id.resulTextView);
            resulTextView.setText(textInImage);
        }
    }
}
