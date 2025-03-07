package de.fischerq.rtm2;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        mToolbar.setLogo(R.drawable.logo);
        mToolbar.setLogoDescription("RunToMusic");
        setSupportActionBar(mToolbar);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.fragment_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setup(R.id.fragment_drawer, (DrawerLayout) findViewById(R.id.drawer), mToolbar);


    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        Fragment fragment;
        switch (position)
        {
            case 0:
                fragment = getFragmentManager().findFragmentByTag(ModeFragment.TAG);
                if (fragment == null) {
                    fragment = new ModeFragment();
                }
                getFragmentManager().beginTransaction().replace(R.id.container, fragment, ModeFragment.TAG).commit();
                break;
            case 1:
                fragment = getFragmentManager().findFragmentByTag(PlaylistFragment.TAG);
                if (fragment == null) {
                    fragment = new PlaylistFragment();
                }
                getFragmentManager().beginTransaction().replace(R.id.container, fragment, PlaylistFragment.TAG).commit();
                break;
            case 2:
                Toast.makeText(this, "goto playing", Toast.LENGTH_SHORT).show();
                fragment = getFragmentManager().findFragmentByTag(PlayingFragment.TAG);
                if (fragment == null) {
                    fragment = new PlayingFragment();
                }
                getFragmentManager().beginTransaction().replace(R.id.container, fragment, PlayingFragment.TAG).commit();
                break;
            default:
                Toast.makeText(this, "Unknown Menu item selected -> " + position, Toast.LENGTH_SHORT).show();
                break;
        }
    }


    @Override
    public void onBackPressed() {
        if (mNavigationDrawerFragment.isDrawerOpen())
            mNavigationDrawerFragment.closeDrawer();
        else
            super.onBackPressed();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
