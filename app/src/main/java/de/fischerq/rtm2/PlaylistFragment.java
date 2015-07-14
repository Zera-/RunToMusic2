package de.fischerq.rtm2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class PlaylistFragment extends Fragment {
    public static final String TAG = "playlist";

    private static final int PICKFILE_RESULT_CODE = 1;

    private LinkedList<PlaylistEntry> queued_songs = new LinkedList<PlaylistEntry>();
    private Uri mFileURI = null;

    private View myview;
    List<String> content = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.playlist_fragment, container, false);
        myview = v;

        Button add = (Button) v.findViewById(R.id.addSong);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("audio/*");
                    startActivityForResult(intent,PICKFILE_RESULT_CODE);
                }
                catch(ActivityNotFoundException exp){
                    Toast.makeText(getActivity().getBaseContext(), "No File (Manager / Explorer)etc Found In Your Device",Toast.LENGTH_LONG).show();
                }
            }
        });


        Button go = (Button) v.findViewById(R.id.go_on);
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment fragment = getFragmentManager().findFragmentByTag(PlayingFragment.TAG);
                if (fragment == null) {
                    fragment = new PlayingFragment();
                }
                PlayingFragment playing = (PlayingFragment)fragment;
                playing.setPlaylist(queued_songs);
                getFragmentManager().beginTransaction().replace(R.id.container, fragment, PlayingFragment.TAG).commit();

            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ListView listView = (ListView) getActivity().findViewById(R.id.listView);
        listView.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, content));

        /*listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
        });*/

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        switch(requestCode){
            case PICKFILE_RESULT_CODE:
                if(resultCode== Activity.RESULT_OK){

                    mFileURI = data.getData();

                    AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
                    builder.setTitle("Song's BPM");

// Set up the input
                    final EditText input = new EditText(this.getActivity());
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    builder.setView(input);

// Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                int number = Integer.parseInt(input.getText().toString());
                                PlaylistEntry p = new PlaylistEntry();
                                p.speed = number;
                                p.uri = mFileURI;

                                Cursor returnCursor =
                                        getActivity().getApplicationContext().getContentResolver().query(p.uri, null, null, null, null);
                                returnCursor.moveToFirst();
                                p.name = returnCursor.getString(returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                                returnCursor.close();
                                queued_songs.offer(p);
                                content.add(p.name);
                            } catch (NumberFormatException e) {

                            }


                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();
                }
                break;
        }
    }

}