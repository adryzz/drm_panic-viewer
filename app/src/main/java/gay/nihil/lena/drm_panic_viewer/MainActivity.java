package gay.nihil.lena.drm_panic_viewer;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.MediaStore;

import kotlin.jvm.internal.DefaultConstructorMarker;
import zxingcpp.BarcodeReader;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;
import androidx.room.Room;

import gay.nihil.lena.drm_panic_viewer.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    public AppDatabase database;


    private static final int CAMERA_REQUEST = 1312;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = Room.databaseBuilder(getBaseContext(),
                AppDatabase.class, "panic-database").allowMainThreadQueries().build();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // hide the fucking keyboard from everywhere god fuck
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM, WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        binding.fab.setOnClickListener(view -> {
            Intent takePictureIntent = new Intent(getBaseContext(), CameraViewActivity.class);
            try {
                // TODO: use a non-deprecated method
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            } catch (ActivityNotFoundException e) {
                // display error state to the user
                Toast.makeText(getBaseContext(), R.string.no_report_uri, Toast.LENGTH_SHORT).show();
            }
        });

        navController.addOnDestinationChangedListener((navController1, navDestination, bundle) -> {
            if (navDestination.getId() == R.id.FirstFragment) {
                binding.fab.show();
            } else {
                binding.fab.hide();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();

            assert uri != null;

            try {
                PanicMessage panic = PanicMessage.parse(uri);
                if (!PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getString("save_old_panics", "0").equals("0")) {
                    database.panicDao().insert(panic);
                }

                Fragment first = getSupportFragmentManager().getFragments().get(0);
                assert first != null;
                Bundle bundle = new Bundle();
                bundle.putParcelable("panic", panic);
                NavHostFragment.findNavController(first)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment, bundle);
            } catch (NullPointerException e) {
                Toast.makeText(getBaseContext(), R.string.not_a_crash_qr, Toast.LENGTH_LONG).show();
                return;
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(getBaseContext(), SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}