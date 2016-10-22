package javaghost.neiexplorer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;

public class Explorer extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

   final int PERMISSION_CODE = 33;

   StatePillar statePillar;
   GoogleApiClient geo_Clt;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_explorer);

      if (geo_Clt == null) {
         geo_Clt = new GoogleApiClient.Builder(this)
                 .addConnectionCallbacks(this)
                 .addOnConnectionFailedListener(this)
                 .addApi(LocationServices.API)
                 .build();
      }
      statePillar = StatePillar.linkToInstance(this, geo_Clt);
   }

   protected void onStart() {
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
              != PackageManager.PERMISSION_GRANTED &&
              ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {

         Toast.makeText(this, "GPS is either denied or disabled", Toast.LENGTH_SHORT).show();
         Log.i("Explorer","GPS is either denied or disabled");
         ActivityCompat.requestPermissions( this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
         // implicitly calls .connect() when called from async thread in onRequestPermissionsResult()
      } else {
         // we HAD permissions needed and can go
         geo_Clt.connect();
      }

      super.onStart();
   }

   @Override
   public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
      switch (requestCode) {
         case PERMISSION_CODE: {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               // permission was granted
               geo_Clt.connect();
               statePillar.setGPSon(true);
            } else {
               // permission denied
               statePillar.setGPSon(false);
            }
            return;
         }
         default:
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      }
   }

   protected void onStop() {
      geo_Clt.disconnect();
      super.onStop();
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      // create the menu from the menu xml file
      getMenuInflater().inflate(R.menu.actb_menu, menu);
      return super.onCreateOptionsMenu(menu);
   }

   // what to do when an item in the menu is clicked
   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         case R.id.menu_set:
            getFragmentManager().beginTransaction()
                    .replace(R.id.pref_container, new SettingsFragment())
                    .addToBackStack("settings") //add the action to the back stack. pressing back will undo the replace and will NOT close the activity!
                    .commit();
            break;

         case R.id.menu_clear:
            finish();
            break;
      }
      return super.onOptionsItemSelected(item);
   }

   @Override
   public void onConnected(Bundle connectionHint) {

      Location got_geoloc = LocationServices.FusedLocationApi.getLastLocation(geo_Clt);
      if (got_geoloc != null) {
         statePillar.setHWLocation(got_geoloc);
         statePillar.setGPSon(true);
// TODO        WHERE should I define IF ._now = ._hw_loc ?
      }
   }

   @Override
   public void onConnectionSuspended(int i) {
      // statePillar.setGPSon(false);
   }

   @Override
   public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
      statePillar.setGPSon(false);
   }
}
