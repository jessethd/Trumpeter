package jthd.trumpeter;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

public class FeedActivity extends AppCompatActivity implements SubmitBarFragment.OnFragmentInteractionListener{

    private final int MAX_TRUMPETS = 1000;

    private ParseUser user;

    private Toolbar titleBar;
    private ListView feedListView;
    private SwipeRefreshLayout feedSwipeLayout;
    private TextView emptyTextView;

    boolean isScrollingUp;
    int lastFirstVisibleItem;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        feedSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.feedSwipeLayout);
        titleBar = (Toolbar) findViewById(R.id.titleBar);
        feedListView = (ListView) findViewById(R.id.feedListView);
        emptyTextView = (TextView) findViewById(R.id.emptyTextView);
        feedListView.setEmptyView(emptyTextView);
        /**
         * Waiting on FeedManager concept. Need to verify how to save List<ParseObject> data from a Callback inner class and return it in a function,
         * or I'll have to do all similar queries like this.
         */
        // FeedManager feedManager = new FeedManager();
        // List<ParseObject> trumpetList = feedManager.getTrumpets();
        setSupportActionBar(titleBar);
        feedSwipeLayout.setRefreshing(true);
        loadListViewData();
        feedSwipeLayout.setRefreshing(false);
        feedSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                                                 @Override
                                                 public void onRefresh() {
                                                     // This method performs the actual data-refresh operation.
                                                     // The method calls setRefreshing(false) when it's finished.
                                                     refreshListView();
                                                 }
                                             }
        );
        feedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ParseObject trumpet = (ParseObject)feedListView.getItemAtPosition(position);
                toViewTrumpetActivity(trumpet);
            }
        });


    }



    @Override
    protected void onResume(){
        super.onResume();
        user = ParseUser.getCurrentUser();
        Log.d("FeedActivity", user.getEmail());
        Fragment submitBarFragment = launchSubmitBarFragment();
        startScrollListener(submitBarFragment);
    }


    /**
     * Upon returning from successful Trumpet submission, refresh the feedListView and scroll to the top.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case (0) : {
                if (resultCode == Activity.RESULT_OK) {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Do something after 5s = 5000ms
                            Log.d("FeedActivity", "Returned, waiting to refresh");
                            refreshAndScrollToTop();
                        }
                    }, 300);

                }
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_view_trumpet, menu);
        return true;
    }

    /**
     * Defines the ActionBar options. Currently have Refresh (always shown as icon) and Sign Out (always overflow menu).
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settingsAction:
                // launch Settings activity
                return true;
            case R.id.signOutAction:
                ParseUser.getCurrentUser().logOut();
                toLoginActivity();
            //case R.id.profileAction:
            // launch ProfileActivity
            // return true;
            case R.id.refreshAction:
                    refreshAndScrollToTop();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSubmitBarFragmentInteraction(Uri uri) {

    }

    /**
     * Refreshes the list data using refreshListView() and scrolls the ListView to the top using smoothScrollToPosition. This function is called
     * when returning to FeedActivity after SUBMITTING a new Trumpet (NOT called when submission incomplete) and when the "refresh" ActionBar button
     * is pressed.
     */
    private void refreshAndScrollToTop(){
        feedSwipeLayout.setRefreshing(true);
        refreshListView();
        feedListView.post(new Runnable() {
            @Override
            public void run() {
                feedListView.smoothScrollToPosition(0); // TODO Double edged sword; good for submission, bad when viewing and backing
            }
        });
    }


    /**
     * Refreshes the data in the ListView's adapter and reloads the new data into the existing ListView TrumpetViews.
     * Note: Call feedSwipeLayout.setRefreshing(true) before every call to this function. This function sets refreshing to false once refreshing
     * is complete.
     */
    private void refreshListView(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Trumpet");
        query.orderByDescending("createdAt");
        query.setLimit(MAX_TRUMPETS);
        query.whereDoesNotExist("replyTrumpetID");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> trumpetList, ParseException e) {
                if (e == null) {
                    // Trumpets retrieved, get adapter and update its internal trumpetList
                    Log.d("FeedActivity", Integer.toString(trumpetList.size()));
                    Log.d("FeedActivity", "Found trumpets to refresh");
                    FeedAdapter adapter = (FeedAdapter) feedListView.getAdapter();
                    adapter.getNewData(trumpetList);
                    Log.d("FeedActivity", "Trumpets refreshed");
                    adapter.notifyDataSetChanged();
                    feedSwipeLayout.setRefreshing(false);
                } else {
                    // Error occurred retrieving Trumpets; display message to user
                    Log.d("FeedActivity", "Found no trumpets to refresh");
                }
            }
        });
    }


    /**
     * Queries Parse for desired non-reply Trumpet data and loads that Trumpet data into the Feed ListView as a custom ArrayAdapter, FeedAdapter.
     * Currently loading basically all (MAX_TRUMPET) Trumpets. This won't scale obviously so will need to future-proof at some point.
     */
    private void loadListViewData(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Trumpet");
        query.orderByDescending("createdAt");
        query.setLimit(MAX_TRUMPETS);
        query.whereDoesNotExist("replyTrumpetID");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> trumpetList, ParseException e) {
                if (e == null) {
                    // Trumpets retrieved, create FeedAdapter with this data and load the Adapter into the ListView
                    Log.d("FeedManager", "Found trumpets");
                    FeedAdapter adapter = new FeedAdapter(FeedActivity.this, R.layout.trumpet_view_layout, trumpetList);
                    feedListView.setAdapter(adapter);
                    Log.d("FeedManager", "trumpetList count " + trumpetList.size());
                } else {
                    // Error occurred retrieving Trumpets; display message to user
                    Log.d("FeedManager", "Found no trumpets");
                }
            }
        });
    }

    /**
     * Launches the ScrollListener attached to feedListView that shows and hides the SubmitBarFragment as user scrolls up and down.
     * @param submitBarFragment The Fragment at the bottom of the screen that shows and hides with user scrolling.
     */
    private void startScrollListener(final Fragment submitBarFragment) {
        feedListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // TODO Auto-generated method stub
            }

            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // TODO Auto-generated method stub

                if (scrollState == 0)
                    Log.i("ScrollListener", "scrolling stopped...");


                if (view.getId() == feedListView.getId()) {
                    final int currentFirstVisibleItem = feedListView.getFirstVisiblePosition();
                    if (currentFirstVisibleItem > lastFirstVisibleItem) {
                        isScrollingUp = false;
                        Log.i("ScrollListener", "scrolling down...");
                        hideSubmitBarFragment(submitBarFragment);
                    } else if (currentFirstVisibleItem < lastFirstVisibleItem) {
                        isScrollingUp = true;
                        Log.i("ScrollListener", "scrolling up...");
                        showSubmitBarFragment(submitBarFragment);
                    }

                    lastFirstVisibleItem = currentFirstVisibleItem;
                }
            }
        });

    }

    /**
     * Launches the Submit Bar at the bottom of the layout that allows for users to travel to SubmitTrumpetActivity and "submit" Trumpets to Parse. This
     * should run before the hide and show methods below. Saves the SubmitBarFragment under tag "SubmitBarFragment".
     * Causes the Submit Bar to display until user begins scrolling down.
     */
    private Fragment launchSubmitBarFragment(){
        Fragment submitBarFragment = new SubmitBarFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.feedLayout, submitBarFragment, "SubmitBarFragment");
        transaction.commit();
        return submitBarFragment;
    }


    /**
     * Shows the Submit Bar Fragment. Runs when user begins scrolling up.
     */
    private void showSubmitBarFragment(Fragment submitBarFragment){
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        ft.show(submitBarFragment);
        ft.commit();
    }

    /**
     * Hides the Submit Bar Fragment. Runs when user begins scrolling down.
     */
    private void hideSubmitBarFragment(Fragment submitBarFragment){
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        ft.hide(submitBarFragment);
        ft.commit();
    }

    /**
     * Launches the ViewTrumpetActivity for the Trumpet that was pressed in the feedListView.
     * @param trumpet, the Trumpet that is to be viewed in ViewTrumpetActivity.
     */
    private void toViewTrumpetActivity(ParseObject trumpet){
        Intent intent = new Intent(FeedActivity.this, ViewTrumpetActivity.class);
        intent.putExtra("objectID", trumpet.getObjectId());
        intent.putExtra("trumpetID", trumpet.getInt("trumpetID"));
        // Bool indicating that this launch of ViewTrumpetActivity is NOT post-reply submission, determines delay in loading reply data
        intent.putExtra("fromReply", false);
        startActivity(intent);
    }

    /**
     * Returns to LoginActivity after successful sign-out. Flags clear the backstack so user cannot return without logging in again.
     */
    private void toLoginActivity(){
        Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }


}
