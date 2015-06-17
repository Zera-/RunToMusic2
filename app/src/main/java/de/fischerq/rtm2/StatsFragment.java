package de.fischerq.rtm2;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by poliveira on 11/03/2015.
 */
public class StatsFragment extends Fragment {
    public static final String TAG = "stats";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.statsfragment, container, false);
        ImageView btn = (ImageView) v.findViewById(R.id.float_button);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnButtonClick(v);
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        List<String> content = new ArrayList<>();
        content.add("a");
        content.add("b");
        content.add("c");
        content.add("d");
        content.add("e");
        ListView listView = (ListView) getActivity().findViewById(R.id.listView);
        listView.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, content));
    }

    public void OnButtonClick(View v){
        Toast.makeText(this.getActivity(), "Selected Playlist ", Toast.LENGTH_SHORT).show();
    }
}