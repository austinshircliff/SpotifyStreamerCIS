package com.plusgaurav.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;

public class TopTenTracksActivityFragment extends Fragment {

    private static TopTenTrackAdapter topTenTrackAdapter;
    public static ArrayList<TrackListData> topTenTrackList;
    ListView topTenTrackView;
    private ProgressBar spinner;
    String[] artistInfo = TopTenTracksActivity.artistInfo;

    public TopTenTracksActivityFragment() {
    }

    /* ------------------life-cycle------------------------------ */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save data source
        if (topTenTrackList != null) {
            outState.putParcelableArrayList("savedTopTenTrackList", topTenTrackList);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_top_ten_tracks, container, false);

        // Progress Bar
        spinner = (ProgressBar) rootView.findViewById(R.id.progressBar2);
        spinner.setVisibility(View.VISIBLE);

        // bind view with adapter
        topTenTrackList = new ArrayList<>();
        topTenTrackView = (ListView) rootView.findViewById(R.id.topTenTrackListView);
        bindView();

        // if savedInstanceState is null -> false
        // else datasource has been parcelled and can be reused ->true
        Boolean isRestoringState = savedInstanceState != null;

        // no data to restore -> run Async
        if (!isRestoringState) {

            // get top ten tracks of the artist (async task)
            FetchTopTenTrack task = new FetchTopTenTrack();
            assert artistInfo != null;
            task.execute(artistInfo[0]);

        } else {
            // get saved datasource
            topTenTrackList = savedInstanceState.getParcelableArrayList("savedTopTenTrackList");
            bindView();
            spinner.setVisibility(View.GONE);
        }

        // TODO implement listener to start PlayMusicActivity
        topTenTrackView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // start Player Activity
                String trackPosition = String.valueOf(position);
                Intent intent = new Intent(getActivity(), PlayerActivity.class).putExtra(Intent.EXTRA_TEXT, trackPosition);
                startActivity(intent);
            }
        });

        return rootView;
    }

    private void bindView() {

        // initialize adapter
        topTenTrackAdapter = new TopTenTrackAdapter(getActivity(), topTenTrackList);

        // bind listview
        topTenTrackView.setAdapter(topTenTrackAdapter);
    }


    public class FetchTopTenTrack extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... artistId) {

            // for catching network extra exceptions
            try {
                // do spotify transaction
                SpotifyApi api = new SpotifyApi();
                api.setAccessToken(SearchArtistActivity.getAccessToken());
                SpotifyService spotify = api.getService();

                // set options
                Map<String, Object> options = new HashMap<>();
                options.put("country", "US");

                // search top 10 tracks of the artist
                Tracks topTracks = spotify.getArtistTopTrack(artistId[0], options);
                topTenTrackList.clear();
                for (Track track : topTracks.tracks) {
                    TrackListData currentTrack = new TrackListData(track);
                    currentTrack.trackArtist = artistInfo[1];
                    topTenTrackList.add(currentTrack);
                }

                // return true if data source refreshed
                return !topTenTrackList.isEmpty();
            } catch (Exception e) {
                return false;
            }

        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Boolean isDataSourceRefreshed) {
            if (isDataSourceRefreshed) {
                spinner.setVisibility(View.GONE);
                topTenTrackAdapter.notifyDataSetChanged();
            } else {
                spinner.setVisibility(View.GONE);
                String[] artistInfo = getActivity().getIntent().getExtras().getStringArray(Intent.EXTRA_TEXT);
                assert artistInfo != null;
                Toast.makeText(getActivity(), "No tracks found for \"" + artistInfo[1] + "\"", Toast.LENGTH_LONG).show();
            }
        }
    }


    // custom adapter
    // got help from "http://stackoverflow.com/questions/8166497/custom-adapter-for-list-view"
    public class TopTenTrackAdapter extends BaseAdapter {
        ArrayList topTenTrackList = new ArrayList();
        Context context;


        public TopTenTrackAdapter(Context context, ArrayList topTenTrackList) {
            this.topTenTrackList = topTenTrackList;
            this.context = context;
        }

        @Override
        public int getCount() {
            return topTenTrackList.size();
        }

        @Override
        public TrackListData getItem(int position) {
            return (TrackListData) topTenTrackList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row = inflater.inflate(R.layout.toptentracklistview_layout, viewGroup, false);

            // put track image
            de.hdodenhof.circleimageview.CircleImageView trackImageView = (de.hdodenhof.circleimageview.CircleImageView) row.findViewById(R.id.trackImage);
            trackImageView.setImageBitmap(null);
            String url = getItem(position).trackImageSmall;
            Picasso.with(row.getContext()).load(url).placeholder(R.drawable.ic_play).error(R.drawable.ic_play).into(trackImageView);

            // put track name
            TextView trackName = (TextView) row.findViewById(R.id.trackName);
            trackName.setText(getItem(position).trackName);

            // put track album
            TextView trackAlbum = (TextView) row.findViewById(R.id.trackAlbum);
            trackAlbum.setText(getItem(position).trackAlbum);

            return row;
        }
    }
}
