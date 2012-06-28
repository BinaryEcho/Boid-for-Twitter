package com.teamboid.twitter;

import java.util.ArrayList;

import com.handlerexploit.prime.widgets.RemoteImageView;

import twitter4j.GeoLocation;
import twitter4j.Place;
import twitter4j.Tweet;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * The list adapter used in activites that search for tweets.
 * @author Aidan Follestad
 */
public class SearchFeedListAdapter extends BaseAdapter {

	public SearchFeedListAdapter(Context context) {
		mContext = context;
		tweets = new ArrayList<Tweet>();
	}
	public SearchFeedListAdapter(Context context, String _id) {
		mContext = context;
		tweets = new ArrayList<Tweet>();
		ID = _id;
	}

	private Context mContext;
	private ArrayList<Tweet> tweets;
	public String ID;
	public ListView list;
	public int savedIndex;
	public int savedIndexTop;
	
	private boolean add(Tweet tweet) {
		boolean added = false;
		if(!update(tweet)) {
			tweets.add(findAppropIndex(tweet), tweet);
			added = true;
		}
		notifyDataSetChanged();
		return added;
	}
	public int add(Tweet[] toAdd) {
		int before = tweets.size();
		int added = 0;
		for(Tweet tweet : toAdd) {
			if(add(tweet)) added++;
		}
		if(before == 0) return 0;
		else if(added == before) return 0;
		else return (tweets.size() - before);
	}
	public void remove(int index) {
		tweets.remove(index);
		notifyDataSetChanged();
	}
	public void clear() {
		tweets.clear();
		notifyDataSetChanged();
	}

	public Tweet[] toArray() { return tweets.toArray(new Tweet[0]); }
	
	public Boolean update(Tweet toFind) {
		Boolean found = false;
		for(int i = 0; i < tweets.size(); i++) {
			if(tweets.get(i).getId() == toFind.getId()) {
				found = true;
				tweets.set(i, toFind);
				break;
			}
		}
		return found;
	}

	private int findAppropIndex(Tweet tweet) {
		int toReturn = 0;
		ArrayList<Tweet> itemCache = tweets;
		for(Tweet t : itemCache) {
			if(t.getCreatedAt().before(tweet.getCreatedAt())) break;
			toReturn++;
		}
		return toReturn;
	}

	@Override
	public int getCount() { return tweets.size(); }
	@Override
	public Object getItem(int position) {
		return tweets.get(position);
	}
	@Override
	public long getItemId(int position) {
		if((position == 0 && tweets.size() == 0) || position > tweets.size()) return 0;
		else if(position == -1 && tweets.size() == 1) return tweets.get(0).getId();
		return tweets.get(position).getId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RelativeLayout toReturn = null;
		if(convertView != null) toReturn = (RelativeLayout)convertView;
		else toReturn = (RelativeLayout)LayoutInflater.from(mContext).inflate(R.layout.feed_item, null);
		toReturn.findViewById(R.id.feedItemMediaFrame).setVisibility(View.GONE);
		final Tweet tweet = tweets.get(position);
		RemoteImageView profilePic = (RemoteImageView)toReturn.findViewById(R.id.feedItemProfilePic);
		profilePic.setImageURL(tweet.getProfileImageUrl());
		profilePic.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { 
				mContext.startActivity(new Intent(mContext, ProfileScreen.class).putExtra("screen_name", tweet.getFromUser()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			}
		}); 
		if(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("show_real_names", false)) {
			((TextView)toReturn.findViewById(R.id.feedItemUserName)).setText(tweet.getFromUserName());
		} else ((TextView)toReturn.findViewById(R.id.feedItemUserName)).setText(tweet.getFromUser());
		TextView itemTxt = (TextView)toReturn.findViewById(R.id.feedItemText); 
		itemTxt.setText(Utilities.twitterifyText(mContext, tweet.getText(), tweet.getURLEntities(), tweet.getMediaEntities(), false));
		itemTxt.setLinksClickable(false);
		((TextView)toReturn.findViewById(R.id.feedItemTimerTxt)).setText(Utilities.friendlyTimeShort(tweet.getCreatedAt()));
		if(tweet.getPlace() != null || tweet.getGeoLocation() != null) {
			toReturn.findViewById(R.id.locationFrame).setVisibility(View.VISIBLE);
			if(tweet.getPlace() != null) {
				Place p = tweet.getPlace();
				((TextView)toReturn.findViewById(R.id.locationIndicTxt)).setText(p.getFullName());
			} else {
				GeoLocation g = tweet.getGeoLocation();
				((TextView)toReturn.findViewById(R.id.locationIndicTxt)).setText(Double.toString(g.getLatitude()) + ", " + Double.toString(g.getLongitude()));
			}
		} else toReturn.findViewById(R.id.locationFrame).setVisibility(View.GONE);
		final String media = Utilities.getTweetYFrogTwitpicMedia(tweet);
		if(media != null && !media.isEmpty()) {
			((ImageView)toReturn.findViewById(R.id.feedItemMediaIndicator)).setVisibility(View.VISIBLE);
		} else ((ImageView)toReturn.findViewById(R.id.feedItemMediaIndicator)).setVisibility(View.GONE);
		return toReturn;
	}
}