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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int SELECT_PICTURE = 1;
    private static final int TAKE_PICTURE = 2;
    private static final int REQUEST_PERMISSIONS = 1;

    ProcessTextWithOCR textProcessorTask;

    private ProgressBar progressBar;
    private int progressStatus = 0;
    private TextView textView;
    private Handler handler = new Handler();
    private String mCurrentPhotoPath;

    DBHelper translationDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create DB for translations
        translationDB = new DBHelper(this);

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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                    {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                        REQUEST_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {

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
            TextView pictureTextViewTitle = (TextView) findViewById(R.id.pictureTextViewTitle);
            pictureTextViewTitle.setText("Still processing image... Please wait for process to finish before changing image");
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
            TextView pictureTextViewTitle = (TextView) findViewById(R.id.pictureTextViewTitle);
            pictureTextViewTitle.setText("Still processing image... Please wait for process to finish before changing image");
        }
        else {
            // TODO: Open Camera...
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, TAKE_PICTURE);
            }
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
            else if (requestCode == TAKE_PICTURE)
            {
                File f = new File(mCurrentPhotoPath);
                Uri contentUri = Uri.parse(f.toString());
                ImageView imageViewToUpdate = (ImageView)findViewById(R.id.mainImageView);
                imageViewToUpdate.setImageURI(contentUri);

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);

                textProcessorTask = new ProcessTextWithOCR(this);
                textProcessorTask.execute(contentUri);

            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
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

    private class ProcessTextWithOCR extends AsyncTask<Uri, Void, String[]> {

        private Context context;
        private String language;

        public ProcessTextWithOCR(Context context) {
            this.context = context;
        }

        @Override
        protected String[] doInBackground(Uri... uris) {

            // Get text from image
            String textInImage = "";

            if (uris.length == 1) {

                TessBaseAPI ocrAPI = new TessBaseAPI(new TessBaseAPI.ProgressNotifier() {
                    @Override
                    public void onProgressValues(TessBaseAPI.ProgressValues progressValues) {
                        progressBar = (ProgressBar) findViewById(R.id.progressBar1);
                        progressBar.setProgress(progressValues.getPercent());
                        textView = (TextView) findViewById(R.id.textView1);
                        textView.setText(progressValues.getPercent() + "/" + progressBar.getMax());
                    }
                });

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

            // Check for translation in the dictionary
            String translation = "";

            // Only english translation is supported
            if (!language.equals("eng")) {
                translation = "Translation of current language is not supported.";
            }
            else {
                translation = translationDB.getHebrewTranslation(textInImage);
            }

            String[] resultsArray = {textInImage, translation};
            return resultsArray;
        }

        @Override
        protected void onPreExecute() {
            updateTextViews("Processing image to get text...", "", "", "");

            // Get the language from the settings
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.context);
            language = sharedPrefs.getString(getResources().getString(R.string.pref_lang_key),
                    getResources().getString(R.string.pref_lang_default));
        }

        @Override
        protected void onPostExecute(String[] resultsArray) {
            updateTextViews(getResources().getString(R.string.picture_text),
                    resultsArray[0],
                    getResources().getString(R.string.translation_text),
                    resultsArray[1]);
        }

        private void updateTextViews(String pictureTextTitle,
                                     String textInImage,
                                     String translationTextTitle,
                                     String translation) {

            TextView pictureTextViewTitle = (TextView) findViewById(R.id.pictureTextViewTitle);
            pictureTextViewTitle.setText(pictureTextTitle);

            TextView pictureTextView = (TextView) findViewById(R.id.pictureTextView);
            pictureTextView.setText(textInImage);

            TextView translationTextViewTitle = (TextView) findViewById(R.id.translationTextViewTitle);
            translationTextViewTitle.setText(translationTextTitle);

            TextView translationTextView = (TextView) findViewById(R.id.translationTextView);
            translationTextView.setText(translation);

        }
    }
}
