package com.plusgaurav.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;

public class SearchArtistActivityFragment extends Fragment {

    private EditText searchArtistEditText;
    private static ArtistAdapter artistAdapter;
    private ArrayList<ArtistListData> artistList;
    ListView artistView;
    private ProgressBar spinner;
    private FetchArtistTask task;

    public SearchArtistActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // get saved datasource if present
        if (savedInstanceState != null) {
            artistList = savedInstanceState.getParcelableArrayList("savedArtistList");
            bindView();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save data source
        if (artistList != null) {
            outState.putParcelableArrayList("savedArtistList", artistList);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search_artist, container, false);

        // Progress Bar -> initially no progress
        spinner = (ProgressBar) rootView.findViewById(R.id.progressBar1);
        spinner.setVisibility(View.GONE);

        // Listener for when the user is done typing the artist name in the edittext feild
        searchArtistEditText = (EditText) rootView.findViewById(R.id.searchArtistEditText);
        searchArtistEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (searchArtistEditText.length() != 0) {

                    // show progress bar
                    spinner.setVisibility(View.VISIBLE);

                    if (task != null) {
                        task.cancel(false);
                    }

                    // search for artists
                    task = new FetchArtistTask();
                    task.execute("*" + searchArtistEditText.getText().toString() + "*");
                } else {
                    // remove old results on no text
                    if (task != null) {
                        task.cancel(false);
                    }
                    spinner.setVisibility(View.GONE);
                    artistList.clear();
                    artistAdapter.notifyDataSetChanged();
                }
            }
        });

        // bind view with adapter
        artistList = new ArrayList<>();
        artistView = (ListView) rootView.findViewById(R.id.artistListView);
        bindView();

        // open top 10 track view
        artistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String artistId = artistList.get(position).artistId;
                String artistName = artistList.get(position).artistName;
                Intent intent = new Intent(getActivity(), TopTenTracksActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, new String[]{artistId, artistName});
                startActivity(intent);
            }
        });

        return rootView;
    }

    private void bindView() {

        // initialize adapter
        artistAdapter = new ArtistAdapter(getActivity(), artistList);

        // bind listview
        artistView.setAdapter(artistAdapter);
    }

    public class FetchArtistTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... artistName) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // for catching network extra exceptions
            try {
                // do spotify transaction
                SpotifyApi api = new SpotifyApi();
                api.setAccessToken(SearchArtistActivity.getAccessToken());
                SpotifyService spotify = api.getService();

                // set options
                Map<String, Object> options = new HashMap<>();
                options.put("limit", 20);

                // check for empty string
                if (artistName[0].equals("")) {
                    return false;
                }

                // search artist
                ArtistsPager artistsPager = spotify.searchArtists(artistName[0], options);

                // update data source
                artistList.clear();
                for (Artist artist : artistsPager.artists.items) {
                    ArtistListData currentArtist = new ArtistListData(artist);
                    artistList.add(currentArtist);
                }

                // return true if data source refreshed
                return !artistList.isEmpty();
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
                artistAdapter.notifyDataSetChanged();
            } else {
                spinner.setVisibility(View.GONE);
                Toast.makeText(getActivity(), "No results found for \"" + searchArtistEditText.getText() + "\". Please refine your search.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // custom adapter
    // got help from "http://stackoverflow.com/questions/8166497/custom-adapter-for-list-view"
    public class ArtistAdapter extends BaseAdapter {
        ArrayList artistList = new ArrayList();
        Context context;


        public ArtistAdapter(Context context, ArrayList artistList) {
            this.artistList = artistList;
            this.context = context;
        }

        @Override
        public int getCount() {
            return artistList.size();
        }

        @Override
        public ArtistListData getItem(int position) {
            return (ArtistListData) artistList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if(convertView==null) {
                convertView = inflater.inflate(R.layout.artistlistview_layout, viewGroup, false);
            }

            // put artist image
            CircleImageView artistImageView =
                    (de.hdodenhof.circleimageview.CircleImageView) convertView.findViewById(R.id.artistImage);
            artistImageView.setImageBitmap(null);
            String url = getItem(position).artistImage;
            Picasso
                .with(convertView.getContext())
                .load(url)
                .placeholder(R.drawable.ic_play)
                .error(R.drawable.ic_play)
                .into(artistImageView);

            // put artist name
            TextView artistName = (TextView) convertView.findViewById(R.id.artistName);
            artistName.setText(getItem(position).artistName);

            return convertView;
        }
    }
}
