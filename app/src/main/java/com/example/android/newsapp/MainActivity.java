package com.example.android.newsapp;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        LoaderManager.LoaderCallbacks<List<News>> {

    /**
     * Tag for the log messages.
     */
    private static final String LOG_TAG = MainActivity.class.getName();

    /**
     * URL for news data from the Guardian data set.
     */
    private static final String BASE_REQUEST_URL = "https://content.guardianapis.com/search";

    /**
     * Data set URL parameter section which filter different topics of news.
     */
    private String section = null;

    /**
     * Data set URL parameter current page which request different page of news.
     */
    private int requestPage = 1;

    /**
     * Constant value for the book loader ID, which can be any integer.
     * There are two ID, one for book, the other for image.
     */
    private static final int NEWS_LOADER_ID = 1;

    /**
     * Adapter for the list of news.
     */
    private NewsAdapter mAdapter;

    /**
     * {@link LinearLayoutManager} for {@link RecyclerView}.
     */
    private LinearLayoutManager layoutManager;

    /**
     * Indicator whether the app is loading more data when users reach the bottom of the list.
     */
    private boolean isLoading = false;

    /**
     * These three integers help the app estimates whether the list reach bottom or not.
     */
    private int pastVisibleItems, visibleItemCount, totalItemCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Find a reference to the {@link Toolbar}.
        Toolbar toolbar = findViewById(R.id.toolbar);
        // Restore toolbar title when users rotate their device.
        if (savedInstanceState != null) {
            toolbar.setTitle(savedInstanceState.getString("toolbarTitle"));
        }
        // After setting the title, then set the toolbar. Otherwise, title setting won't work.
        setSupportActionBar(toolbar);
        // Find a reference to the {@link DrawerLayout} and set {@link ActionBarDrawerToggle}
        // to tie together the functionality of DrawerLayout and the framework ActionBar
        // to implement the recommended design for navigation drawers.
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        // Find a reference to the {@link NavigationView} and set the overview item to checked.
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setCheckedItem(R.id.nav_overview);
        // set the item listener to this aka. {@link MainActivity},
        // meaning there is an inner class method handles navigation view item click events.
        navigationView.setNavigationItemSelectedListener(this);

        // Find a reference to the {@link RecyclerView} in the layout,
        // which display main contents of the app.
        RecyclerView recyclerView = findViewById(R.id.list);
        // Create an {@link NewsAdapter}, whose data source is a list of {@link News}.
        mAdapter = new NewsAdapter(this, new ArrayList<News>());
        // Setup {@link DefaultItemAnimator} for the ItemAnimator of RecyclerView.
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        // Setup the {@link LinearLayoutManager} for the LayoutManager of RecyclerView.
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        // Make the {@link RecycleView} use the {@link NewsAdapter} created above, so that the
        // {@link RecycleView} will display list items for each {@link News} in the list.
        recyclerView.setAdapter(mAdapter);
        // Add a divider between items in RecyclerView, using {@link DividerItemDecoration}.
        DividerItemDecoration dividerItemDecoration =
                new DividerItemDecoration(recyclerView.getContext(), layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        // Setup an OnItemClickListener to handle the click event of the RecyclerView item.
        mAdapter.setOnItemClickListener(new NewsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
            }
        });
        // Implement OnScrollListener to implement endless list with RecyclerView.
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                // If the list is loading, return early.
                if (isLoading) {
                    return;
                }


                //check for scroll down.
                if (dy > 0) {
                    visibleItemCount = layoutManager.getChildCount();
                    totalItemCount = layoutManager.getItemCount();
                    pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                        // Only when there is Internet Connection, load more data.
                        if (isConnected()) {
                            // Set the loading indicator to true, because it begins to load more data.
                            isLoading = true;
                            // Request next page of news.
                            requestPage = QueryUtils.currentPage + 1;
                            // Get a reference to the LoaderManager and restart the loader.
                            getLoaderManager().restartLoader(NEWS_LOADER_ID, null, MainActivity.this);
                        } else {
                            // Make a toast to inform users that the device is disconnected.
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    getString(R.string.no_connection), Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    }
                }
            }
        });

        // When there are contents on screen, maintain them when users rotate their device.
        int listItemCount = 0;
        if (savedInstanceState != null) {
            listItemCount = savedInstanceState.getInt("listItemCount");
        }

        // Lookup the swipe container view.
        final SwipeRefreshLayout swipeContainer = findViewById(R.id.swipe_container);
        // Setup refresh listener which triggers new data loading.
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Only when there is internet connection and not empty list, fresh the data.
                if (!isConnected()) {
                    // Call setRefreshing(false) to signal refresh has finished.
                    swipeContainer.setRefreshing(false);
                    // Make a toast to inform users that the device is disconnected.
                    Toast toast = Toast.makeText(getApplicationContext(),
                            getString(R.string.no_connection), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                } else {
                    // Set news page back to one.
                    requestPage = 1;
                    // Get a reference to the LoaderManager and restart the loader.
                    getLoaderManager().restartLoader(NEWS_LOADER_ID, null, MainActivity.this);
                }
            }
        });
        // Configure the refreshing colors.
        swipeContainer.setColorSchemeResources(
                android.R.color.holo_blue_light,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        if (!isConnected() && listItemCount == 0) {
            // If there is no internet connection, display error.
            // Update empty state with no connection error message.
            setEmptyView(true, R.string.no_connection, R.drawable.no_connection);
        } else {
            // Set the refreshing indicator to true, when it begins to load data.
            swipeContainer.setRefreshing(true);
            // Get a reference to the LoaderManager, in order to interact with loaders.
            // And initiate the loader to begin fetching data from Internet.
            getLoaderManager().initLoader(NEWS_LOADER_ID, null, this);
        }
    }

    // Save the needed variable state,
    // when phone rotate to the landscape mode or portrait mode.
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        Toolbar toolbar = findViewById(R.id.toolbar);
        savedInstanceState.putString("toolbarTitle", toolbar.getTitle().toString());
        savedInstanceState.putInt("listItemCount", mAdapter.getItemCount());

        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * This method will be called when it needs to create a new {@link Loader}.
     *
     * @param i      is the ID whose loader is to be created.
     * @param bundle is any arguments supplied by the caller. Here is null.
     * @return a new custom AsyncTaskLoader.
     */
    @Override
    public Loader<List<News>> onCreateLoader(int i, Bundle bundle) {

        // Use {@link Uri.Builder} to build a request url.
        Uri baseUri = Uri.parse(BASE_REQUEST_URL);
        Uri.Builder uriBuilder = baseUri.buildUpon();

        // Only section has value, append query parameter to the URL.
        if (section != null) {
            uriBuilder.appendQueryParameter("section", section);
        }

        uriBuilder.appendQueryParameter("page", Integer.toString(requestPage));
        uriBuilder.appendQueryParameter("format", "json");
        uriBuilder.appendQueryParameter("api-key", "test");

        return new NewsLoader(getApplicationContext(), uriBuilder.toString());
    }

    /**
     * This method will be called when the {@link Loader} finish loading in the working thread.
     *
     * @param loader   is an instance of the {@link Loader}.
     * @param newsList is the result of the loading in the working thread.
     */
    @Override
    public void onLoadFinished(Loader<List<News>> loader, List<News> newsList) {
        // Set the loading indicator to false, because the data loading is finished.
        isLoading = false;

        // Call setRefreshing(false) to signal refresh has finished.
        SwipeRefreshLayout swipeContainer = findViewById(R.id.swipe_container);
        swipeContainer.setRefreshing(false);

        // If there is a valid list of {@link Book}s, then add them to the adapter's data set.
        if (newsList != null && !newsList.isEmpty()) {
            if (requestPage == 1) {
                // Clear the adapter of previous book data.
                mAdapter.clear();
                // Make RecyclerView scroll to the top.
                layoutManager.scrollToPosition(0);
            }

            // Add the list of book through adapter.
            mAdapter.addAll(newsList);
            // Hide empty state view.
            setEmptyView(false, null, null);
        } else if (!isConnected()) {
            // Clear the adapter of previous book data.
            mAdapter.clear();
            // Set no internet connection empty state.
            setEmptyView(true, R.string.no_connection, R.drawable.no_connection);
        } else {
            // Clear the adapter of previous book data.
            mAdapter.clear();
            // Set no result found empty state.
            setEmptyView(true, R.string.something_wrong, R.drawable.something_wrong);
        }
    }

    /**
     * This method will be called when the {@link Loader} reset.
     *
     * @param loader is an instance of the {@link Loader}.
     */
    @Override
    public void onLoaderReset(Loader<List<News>> loader) {
        // Loader reset, clear out our existing data.
        mAdapter.clear();
    }

    /**
     * This method will be called when the Back button was pressed.
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        // If navigation drawer is open, close it.
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * This inner class method is for {@link NavigationView},
     * which implements its {@link NavigationView.OnNavigationItemSelectedListener}.
     *
     * @param item that users have clicked.
     * @return true means there is a navigation item click event.
     */
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        // Set the ToolBal title and the data set URL section.
        Toolbar toolbar = findViewById(R.id.toolbar);
        switch (item.getItemId()) {
            case R.id.nav_overview:
                toolbar.setTitle(R.string.app_name);
                section = null;
                break;
            case R.id.nav_news:
                toolbar.setTitle(R.string.menu_news);
                section = "news";
                break;
            case R.id.nav_opinion:
                toolbar.setTitle(R.string.menu_opinion);
                section = "commentisfree";
                break;
            case R.id.nav_sport:
                toolbar.setTitle(R.string.menu_sport);
                section = "sport";
                break;
            case R.id.nav_culture:
                toolbar.setTitle(R.string.menu_culture);
                section = "culture";
                break;
            case R.id.nav_lifestyle:
                toolbar.setTitle(R.string.menu_lifestyle);
                section = "lifeandstyle";
                break;
            default:
                Log.e(LOG_TAG, "Something wrong with navigation drawer items.");
        }

        if (isConnected()) {
            // Set the swipe refreshing indicator to true.
            SwipeRefreshLayout swipeContainer = findViewById(R.id.swipe_container);
            swipeContainer.setRefreshing(true);
            // Set news page back to one.
            requestPage = 1;
            // Get a reference to the LoaderManager and restart the loader.
            getLoaderManager().restartLoader(NEWS_LOADER_ID, null, this);
        } else {
            // Clear the list of news first.
            mAdapter.clear();
            // Set no internet connection empty state.
            setEmptyView(true, R.string.no_connection, R.drawable.no_connection);
        }
        // Close navigation drawer after handling item click event.
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Helper method that tells whether the device is connected to Internet or not.
     *
     * @return true when the device is connected, false when it is not.
     */
    private boolean isConnected() {
        // Get a reference to the ConnectivityManager to check state of network connectivity.
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        // Get details on the currently active default data network.
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        // Return true if the device is connected, vice versa.
        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * Helper method that set the empty state to the TextView,
     * which tells users the current status of the app.
     *
     * @param visibility      means whether empty view is visible or not.
     *                        true means visible, false means gone.
     * @param textStringId    is the text string id of the TextView.
     * @param imageDrawableId is the compound image id of the TextView.
     */
    private void setEmptyView(boolean visibility, @Nullable Integer textStringId,
                              @Nullable Integer imageDrawableId) {
        TextView emptyView = findViewById(R.id.empty_view);
        if (visibility && textStringId != null && imageDrawableId != null) {
            emptyView.setText(textStringId);
            emptyView.setCompoundDrawablesWithIntrinsicBounds(null,
                    ContextCompat.getDrawable(getApplicationContext(), imageDrawableId),
                    null, null);
            emptyView.setCompoundDrawablePadding(getResources().
                    getDimensionPixelOffset(R.dimen.compound_image_spacing));
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }
}
