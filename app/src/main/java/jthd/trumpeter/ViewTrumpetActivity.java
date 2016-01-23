package jthd.trumpeter;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;


/**
 *
 * NOTE: Unable to pass along ParseObject directly, as it isn't serializable. ParseProxyObject solution is available but results in nasty code,
 * requiring a function for PPO in TrumpetView. Currently just re-retrieving by objectID; should be fairly quick lookup. Making this a fragment
 * may solve this issue entirely, but first examine what new issues it creates.
 */

public class ViewTrumpetActivity extends AppCompatActivity {

    private TrumpetView detailedTrumpetView;
    private ListView replyFeedListView;
    private Toolbar titleBar;

    private int detailedTrumpetObjectID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_trumpet);
        Intent intent = getIntent();
        detailedTrumpetObjectID = intent.getIntExtra("objectID", -1);
        detailedTrumpetView = (TrumpetView)findViewById(R.id.detailedItemLayout);
        replyFeedListView = (ListView) findViewById(R.id.replyFeedListView);
        // TODO Experimenting here by setting views in onCreate; seems like a better idea. If no issues, do it this way in all other activities where it makes sense
        setViews();
        titleBar = (Toolbar) findViewById(R.id.titleBar);
        setSupportActionBar(titleBar);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_view_trumpet, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settingsAction:
                // launch Settings activity
                return true;
            case R.id.refreshAction:
                //feedSwipeLayout.setRefreshing(true);
                //refreshListView();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Retrieve the calling Trumpet (given its object ID), load the detailed Trumpet View with its information, then query that Trumpet's replies and load the ListView
     * with those reply Trumpets in ascending (most recent last) order.
     * As stated in class documentation, this function currently does an (inexpensive) lookup that isn't really necessary. Performance impact should be minor but
     * continue to think about options. Lack of TrumpetID also forces a nested query, which is a bit weird.
     */
    private void setViews(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Trumpet");
        query.whereEqualTo("objectId", detailedTrumpetObjectID);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject trumpet, ParseException e) {
                if (e == null) {
                    detailedTrumpetView.showDetailedTrumpet(trumpet);
                    loadReplyListViewData(trumpet.getInt("trumpetID"));
                } else {

                }
            }
        });
    }

    /**
     * Loads the list of Trumpets that are marked as reply Trumpets to the Trumpet being viewed. This relationship is maintained through the attribute replyTrumpetID,
     * which points to the trumpetID of the Trumpet being viewed.
     * @param trumpetID The TrumpetID of the Trumpet that is being viewed, for use in matching with replyTrumpetIDs.
     */
    private void loadReplyListViewData(int trumpetID){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Trumpet");
        query.orderByAscending("createdAt");
        query.whereEqualTo("replyTrumpetID", trumpetID);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> trumpetList, ParseException e) {
                if (e == null) {
                    // Trumpets retrieved, create FeedAdapter with this data and load the Adapter into the ListView
                    Log.d("FeedManager", "Found trumpets");
                    FeedAdapter adapter = new FeedAdapter(ViewTrumpetActivity.this, R.layout.trumpet_view_layout, trumpetList);
                    replyFeedListView.setAdapter(adapter);
                    Log.d("FeedManager", "trumpetList count " + trumpetList.size());
                } else {
                    // Error occurred retrieving Trumpets; display message to user
                    Log.d("FeedManager", "Found no trumpets");
                }
            }
        });
    }
}
