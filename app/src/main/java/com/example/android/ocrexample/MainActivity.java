package com.example.android.ocrexample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_PICTURE = 1;

    ProcessTextWithOCR textProcessorTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        textProcessorTask = new ProcessTextWithOCR();
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void openGallery(View view) {

        if (textProcessorTask.getStatus() == AsyncTask.Status.RUNNING) {
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

        if (textProcessorTask.getStatus() == AsyncTask.Status.RUNNING) {
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
                textProcessorTask = new ProcessTextWithOCR();
                textProcessorTask.execute(selectedImageUri);
            }
        }
    }

    private class ProcessTextWithOCR extends AsyncTask<Uri, Void, String> {

        @Override
        protected String doInBackground(Uri... uris) {

            String textInImage = "";

            if (uris.length == 1) {
                TessBaseAPI ocrAPI = new TessBaseAPI();

                try {
                    InputStream imageStream = getContentResolver().openInputStream(uris[0]);
                    Bitmap bitmapFile = BitmapFactory.decodeStream(imageStream);
                    imageStream.close();

                    String str = Environment.getExternalStorageDirectory().toString();
                    ocrAPI.init(Environment.getExternalStorageDirectory().toString(), "eng");
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
        }

        @Override
        protected void onPostExecute(String textInImage) {
            TextView resulTextView = (TextView) findViewById(R.id.resulTextView);
            resulTextView.setText(textInImage);
        }
    }
}
