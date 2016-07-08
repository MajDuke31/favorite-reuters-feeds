package com.example.student.favoritereutersfeeds;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Arrays;  //used to hold sorted tags
import android.app.AlertDialog;  //used to display dialogs
import android.content.Context;  //provides access to information about the environment in which the app is running and allows you to access various Android services.  We’ll be using a constant from this class with a LayoutInflater to help load new GUI components dynamically
import android.content.DialogInterface;  //contains the nested interface OnClickListener.  We will implement this interface to handle the events that occur when the user touches a button on an AlertDialog
import android.content.Intent;  //enables us to work with Intents – specifying an action to be performed and the data to be acted upon.  Android uses Intents to launch the appropriate activities.
import android.content.SharedPreferences;  //used to manipulate persistent key/value pairs that are stored in files associated with the app.
import android.net.Uri;  //enables us to convert an Internet URL into the format required by an Intent that launces the device’s web browser.
import android.view.LayoutInflater;  //enables us to inflate an XML Layout file dynamically to create the layout’s GUI components.
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethod;
import android.view.inputmethod.InputMethodManager;  //enables us to hide the soft keyboard when the user saves a search.
import android.widget.Button;   //contains the widget for a simple push button
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;

public class FavoriteReutersFeeds extends AppCompatActivity {
    private SharedPreferences savedFeeds; // user's favorite feeds
    private TableLayout queryTableLayout; // shows the search buttons
    private EditText queryEditText; // where the user enters queries
    private EditText tagEditText; // where the user enters a query's tag

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_reuters_feeds);

        // get the SharedPreferences that contain the saved feeds
        savedFeeds = getSharedPreferences("feeds", MODE_PRIVATE);

        // get a reference to the query table layout and other ui elements
        queryTableLayout = (TableLayout) findViewById(R.id.queryTableLayout);
        queryEditText = (EditText) findViewById(R.id.queryEditText);
        tagEditText = (EditText) findViewById(R.id.tagEditText);

        // register listeners
        Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(saveButtonListener);
        Button clearTagsButton = (Button) findViewById(R.id.clearTagsButton);
        clearTagsButton.setOnClickListener(clearTagsButtonListener);

        refreshButtons(null); // add previously saved feeds to GUI
    }

    private void refreshButtons(String newTag) {
        // store saved tags in the tags array
        String[] tags = savedFeeds.getAll().keySet().toArray(new String[0]);
        Arrays.sort(tags, String.CASE_INSENSITIVE_ORDER); // sort by tag name

        // if a new tag is added (newTag is not null), insert it
        if (newTag != null) {
            makeTagGUI(newTag, Arrays.binarySearch(tags, newTag));
        } else {
            // display all tags
            for (int index= 0; index < tags.length; index++) {
                makeTagGUI(tags[index], index);
            }
        }
    }

    private void makeTag(String query, String tag) {
        String originalQuery = savedFeeds.getString(tag, null);

        // to change sharedPreferences you need an Editor
        SharedPreferences.Editor preferencesEditor = savedFeeds.edit();
        preferencesEditor.putString(tag, query); // store the current search
        preferencesEditor.apply();

        // if a new query, add its gui
        if (originalQuery == null) {
            refreshButtons(tag);
        }
    }

    private void makeTagGUI(String tag, int index) {
        LayoutInflater inflater =
                (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View newTagView = inflater.inflate(R.layout.new_tag_view, null);
        Button newTagButton = (Button) newTagView.findViewById(R.id.newTagButton);
        newTagButton.setText(tag);
        newTagButton.setOnClickListener(queryButtonListener);

        Button newEditButton = (Button) newTagView.findViewById(R.id.newEditButton);
        newEditButton.setOnClickListener(editButtonListener);

        queryTableLayout.addView(newTagView, index);
    }

    public OnClickListener saveButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            // Create a tag if both queryEditText and tagEditText have values
            if (queryEditText.getText().length() > 0 &&
                    tagEditText.getText().length() > 0) {
                makeTag(queryEditText.getText().toString(),
                        tagEditText.getText().toString());
                queryEditText.setText("");
                tagEditText.setText("");

                // hide the soft keyboard
                ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(tagEditText.getWindowToken(), 0);
            } else {
                // display a message to tell user to enter info
                AlertDialog.Builder builder = new AlertDialog.Builder(FavoriteReutersFeeds.this);
                builder.setTitle(R.string.missingTitle);
                builder.setPositiveButton(R.string.OK, null);
                builder.setMessage(R.string.missingMessage);

                AlertDialog errorDialog = builder.create();
                errorDialog.show();
            }
        } // End onClick
    }; // End saveButtonListener

    public OnClickListener clearTagsButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(FavoriteReutersFeeds.this);
            builder.setTitle(R.string.confirmTitle);
            builder.setPositiveButton(R.string.erase,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            clearButtons();
                            SharedPreferences.Editor preferencesEditor =
                                    savedFeeds.edit();
                            preferencesEditor.clear();
                            preferencesEditor.apply();
                        } // ok onClick
                    } // dialog interface listener
            ); // positive button
            builder.setCancelable(true);
            builder.setNegativeButton(R.string.cancel, null);
            builder.setMessage(R.string.confirmMessage);
            AlertDialog confirmDialog = builder.create();
            confirmDialog.show();
        } // onClick
    }; // clearTagsButtonListener

    // Remove all saved search buttons
    private void clearButtons() {
        queryTableLayout.removeAllViews();
    }

    // Load selected search in a browser
    public OnClickListener queryButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            // Get the query
            String buttonText = ((Button)v).getText().toString();
            String query = savedFeeds.getString(buttonText, "");

            // Create url
            String urlString = getString(R.string.searchURL) + query;

            // create an Intent to launch a web browser
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
            startActivity(webIntent);
        }
    };

    public OnClickListener editButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            // get all necessary GUI components
            TableRow buttonTableRow = (TableRow) v.getParent();
            Button searchButton = (Button) buttonTableRow.findViewById(R.id.newTagButton);
            String tag = searchButton.getText().toString();

            // Set EditTexts to match the chosen tag and query
            tagEditText.setText(tag);
            queryEditText.setText(savedFeeds.getString(tag, ""));
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_favorite_reuters_feeds, menu);
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
}
