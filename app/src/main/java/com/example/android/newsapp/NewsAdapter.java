package com.example.android.newsapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * {@link NewsAdapter} is an {@link RecyclerView.Adapter} that can provide the layout
 * for each list item based on a data source, which is a list of {@link News} objects.
 */
public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.MyViewHolder> {

    /**
     * Tag for the log messages.
     */
    private static final String LOG_TAG = NewsAdapter.class.getSimpleName();

    /**
     * Create a new list of {@link News} object.
     */
    private List<News> mNewsList;

    /**
     * Context passed in through the constructor.
     */
    private Context mContext;

    /**
     * Create a new {@link NewsAdapter} object.
     *
     * @param context  is the context of the Activity.
     * @param newsList is a list of {@link News} objects。
     */
    public NewsAdapter(Context context, List<News> newsList) {
        mContext = context;
        mNewsList = newsList;
    }

    /**
     * Create a RecyclerView OnItemClickListener object.
     */
    private OnItemClickListener mOnItemClickListener;

    /**
     * Setup the RecyclerView item click listener.
     *
     * @param OnItemClickListener is the interface of RecyclerVIew OnItemClickListener.
     */
    public void setOnItemClickListener(OnItemClickListener OnItemClickListener) {
        mOnItemClickListener = OnItemClickListener;
    }

    /**
     * The interface of RecyclerVIew OnItemClickListener.
     */
    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    /**
     * Return the size of the list of {@link News} object.
     * Must override this method.
     *
     * @return the size of the list of {@link News} object.
     */
    @Override
    public int getItemCount() {
        return mNewsList.size();
    }

    /**
     * Called when {@link RecyclerView} needs a new {@link RecyclerView.ViewHolder}
     * of the given type to represent an item.
     * Usually involves inflating a layout from XML and returning the holder.
     *
     * @param parent   is the ViewGroup into which the new View will be added
     *                 after it is bound to an adapter position.
     * @param viewType is the view type of the new View.
     * @return a new {@link MyViewHolder} that holds a View of the given view type.
     */
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the custom layout.
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);

        // Return a new holder instance.
        return new MyViewHolder(itemView);
    }

    /**
     * Involves populating data into the item through holder.
     *
     * @param holder   is the custom ViewHolder.
     * @param position is the current position in RecyclerView.
     */
    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        // Get the data model based on position.
        final News news = mNewsList.get(position);

        // Set the initial letter of the news title to the TextView.
        holder.initialLetterView.setText(String.valueOf(news.getTitle().charAt(0)));
        // Set the title of the news to the TextView.
        holder.newsTitleView.setText(news.getTitle().substring(1));

        try {
            // The API returns the time in ISO-8601 format, it needs to be parsed to Date object.
            SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                    Locale.getDefault());
            Date dateIn = inFormat.parse(news.getTime());
            // Define the time format that the app wants and set it to the TextView.
            SimpleDateFormat outFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault());
            holder.newsTimeView.setText(outFormat.format(dateIn));
        } catch (ParseException e) {
            Log.e(LOG_TAG, "Problem parsing the news time.", e);
        }

        // Create an instance of RecyclerVIew OnItemClickListener.
        if (mOnItemClickListener != null) {
            holder.listItemContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Override the onItemClick method in MainActivity.
                    mOnItemClickListener.onItemClick(view, holder.getAdapterPosition());

                    // Intent to browser according to the news URL.
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(news.getUrl()));
                    mContext.startActivity(intent);
                }
            });
        }
    }

    /**
     * Provide a direct reference to each of the views within a data item.
     * Used to cache the views within the item layout for fast access.
     */
    public class MyViewHolder extends RecyclerView.ViewHolder {
        // The holder should contain a member variable
        // for any view that will be set as you render a row.
        public RelativeLayout listItemContainer;
        public TextView initialLetterView, newsTitleView, newsTimeView;

        // Create a constructor that accepts the entire item row
        // and does the view lookups to find each subview.
        public MyViewHolder(View view) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(view);

            listItemContainer = view.findViewById(R.id.list_item_container);
            initialLetterView = view.findViewById(R.id.initial_letter);
            newsTitleView = view.findViewById(R.id.title);
            newsTimeView = view.findViewById(R.id.time);
        }
    }

    /**
     * Helper method that clear the list of {@link RecyclerView}
     * and notify the adapter of the removal.
     */
    public void clear() {
        mNewsList.clear();
        notifyDataSetChanged();
    }

    /**
     * Helper method that pass in the list of {@link RecyclerView}
     * and notify the adapter of the data change.
     *
     * @param newsList is a reference of the {@link List<News>}.
     */
    public void addAll(List<News> newsList) {
        mNewsList.addAll(newsList);
        notifyDataSetChanged();
    }
}
