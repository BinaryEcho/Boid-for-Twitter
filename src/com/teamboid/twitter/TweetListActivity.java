package com.teamboid.twitter;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * @author Aidan Follestad
 */
public class TweetListActivity extends ListActivity {

	private int lastTheme;
	private boolean showProgress;
	private ResponseList<Status> tweets = null;
	private boolean allowPagination = true;
	private FeedListAdapter binder;

	public void showProgress(boolean visible) {
		if(showProgress == visible) return;
		if(binder.isEmpty() && visible) {
			findViewById(android.R.id.progress).setVisibility(View.VISIBLE);
			findViewById(android.R.id.empty).setVisibility(View.GONE);
		} else findViewById(android.R.id.progress).setVisibility(View.GONE);
		showProgress = visible;
		setProgressBarIndeterminateVisibility(visible);
		findViewById(R.id.horizontalProgress).setVisibility((visible == true) ? View.VISIBLE : View.GONE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if(savedInstanceState != null) {
			if(savedInstanceState.containsKey("lastTheme")) {
				lastTheme = savedInstanceState.getInt("lastTheme");
				setTheme(lastTheme);
			} else setTheme(Utilities.getTheme(getApplicationContext()));
			if(savedInstanceState.containsKey("showProgress")) showProgress(true);
		}  else setTheme(Utilities.getTheme(getApplicationContext()));
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.list_fragment);
		binder = new FeedListAdapter(this, null);
		setListAdapter(binder);
		refresh();
		getListView().setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index, long id) {
				Status tweet = (Status)binder.getItem(index);
				if(tweet.isRetweet()) tweet = tweet.getRetweetedStatus();
				startActivity(new Intent(getApplicationContext(), TweetViewer.class).putExtra("tweet_id", id).putExtra("user_name", tweet.getUser().getName()).putExtra("user_id", tweet.getUser().getId())
						.putExtra("screen_name", tweet.getUser().getScreenName()).putExtra("content", tweet.getText()).putExtra("timer", tweet.getCreatedAt().getTime())
						.putExtra("via", tweet.getSource()).putExtra("isFavorited", tweet.isFavorited()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			}
		});
		getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) { }
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				if(totalItemCount > 0 && (firstVisibleItem + visibleItemCount) >= (totalItemCount - 2) && totalItemCount > visibleItemCount) refresh();
			}
		});
	}
	
	public void refresh() {
		if(!allowPagination) return;
		showProgress(true);
		new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					switch(getIntent().getIntExtra("mode", -1)) {
					case USER_FAVORITES:
						runOnUiThread(new Runnable(){
							@Override
							public void run() { setTitle(getString(R.string.user_favorites).replace("{user}", getIntent().getStringExtra("username"))); }
						});
						Paging paging = new Paging(1, 20);
						if(binder.getCount() > 0) paging.setMaxId(binder.getItemId(binder.getCount() - 1));
						tweets = AccountService.getCurrentAccount().getClient().getFavorites(getIntent().getStringExtra("username"), paging);
						break;
					}
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							if(tweets.size() == 0) allowPagination = false;
							else binder.add(tweets.toArray(new Status[0]));
							showProgress(false);
						}
					});
				} catch(final Exception e) { 
					e.printStackTrace();
					runOnUiThread(new Runnable(){
						@Override
						public void run() { Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show(); }
					});
				}
			}
		}).start();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt("lastTheme", lastTheme);
		if(showProgress) {
    		showProgress(false);
    		outState.putBoolean("showProgress", true);
    	}
		super.onSaveInstanceState(outState);
	}

	public static final int USER_FAVORITES = 1;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			startActivity(new Intent(this, TimelineScreen.class));
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
