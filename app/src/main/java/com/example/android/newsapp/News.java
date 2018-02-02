package com.example.android.newsapp;

/**
 * A {@link News} object contains information related to a news.
 * It includes the title, time and URL of the news.
 */
public class News {
    /**
     * Title of the news.
     */
    private String mTitle;

    /**
     * Time of the news.
     */
    private String mTime;

    /**
     * URL of the news.
     */
    private String mUrl;

    /**
     * Create a new News object.
     *
     * @param title is the title of the news.
     * @param time  is the time of the news.
     * @param url   is the URL of the news.
     */
    public News(String title, String time, String url) {
        mTitle = title;
        mTime = time;
        mUrl = url;
    }

    /**
     * Return the title of the news.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Return the time of the book.
     */
    public String getTime() {
        return mTime;
    }

    /**
     * Return the URL of the book.
     */
    public String getUrl() {
        return mUrl;
    }
}
