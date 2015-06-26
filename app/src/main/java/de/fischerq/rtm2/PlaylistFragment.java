package de.fischerq.rtm2;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class PlaylistFragment extends Fragment {
    public static final String TAG = "playlist";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.playlist_fragment, container, false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        List<String> content = new ArrayList<>();
        content.add("AUSTRIA TOP 40 (120 min)");
        content.add("Summer in Munich (54 min)");
        content.add("Isar luxury chillout lounge (287 min)");
        content.add("Sailing across Bavaria (67 min)");
        content.add("Alpenboogie (23 min)");
        ListView listView = (ListView) getActivity().findViewById(R.id.listView);
        listView.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, content));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {

                Toast.makeText(getActivity(), "Selected Playlist", Toast.LENGTH_SHORT).show();
                Fragment fragment = getFragmentManager().findFragmentByTag(PlayingFragment.TAG);
                if (fragment == null) {
                    fragment = new PlayingFragment();
                }
                getFragmentManager().beginTransaction().replace(R.id.container, fragment, PlayingFragment.TAG).commit();
            }
        });
    }

}