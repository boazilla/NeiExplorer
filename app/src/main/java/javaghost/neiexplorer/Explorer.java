package javaghost.neiexplorer;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

public class Explorer extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener {

   final int PERMISSION_CODE = 33;

   StatePillar statePillar;
   GoogleApiClient geo_Clt;

   //
   TextView gps_status; Button check_status;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_explorer);

   }

   protected void onResume(){
      super.onResume();
   }

   protected void onStart() {
      super.onStart();
      if (geo_Clt == null) {
         geo_Clt = new GoogleApiClient.Builder(this)
                 .addConnectionCallbacks(this)
                 .addOnConnectionFailedListener(this)
                 .addApi(LocationServices.API)
                 .build();
      }
      statePillar = StatePillar.linkToInstance(this, geo_Clt);
      statePillar.updateFields();
      statePillar.shP.registerOnSharedPreferenceChangeListener(this);
      checkGPSpermission();
      {
         gps_status = (TextView) findViewById(R.id.tvwGpsStatus);
         gps_status.setText(statePillar._man_loc.toString());
         check_status = (Button) findViewById(R.id.btnCheck);
         check_status.setOnClickListener(this);
//         onClick(findViewById(R.id.btnCheck));
      }
      if (statePillar.isHwDisabledByPermissionTest() || statePillar.isFirstRun()) {
         alertPermissionNeeded();
      }
   }

   protected void onStop() {
      statePillar.shP.unregisterOnSharedPreferenceChangeListener(this);
      geo_Clt.disconnect();
      super.onStop();
   }

   public void alertPermissionNeeded(){
      AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
      alertDialogBuilder.setMessage(getString(R.string.hw_permission_alert));

      alertDialogBuilder
              .setCancelable(false)
              .setTitle(getString(R.string.hw_permission_dlg_title))
              .setPositiveButton("Got it", new DialogInterface.OnClickListener() {
         @Override
         public void onClick(DialogInterface arg0, int arg1) {
            return;
         }
      });

      AlertDialog alertDialog = alertDialogBuilder.create();
      alertDialog.show();
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
            // hide main activity layout behind fragment container
            ((View)findViewById(R.id.pref_container)).setVisibility(View.VISIBLE);
            called_overlapping_screen = 1;
            getFragmentManager().beginTransaction()
                    .replace(R.id.pref_container, new SettingsFragment())
                    .addToBackStack("settings")
                    .commit();
            break;

         case R.id.menu_clear:
            finish();
            break;
      }
      return super.onOptionsItemSelected(item);
   }
   static int called_overlapping_screen = 0;

   @Override // avoid double back exit && handle overlapping view visibility
   public void onBackPressed() {

      called_overlapping_screen--;
      if (called_overlapping_screen < 0) {
      new AlertDialog.Builder(this)
              .setTitle(getString(R.string.exit_title))
              .setMessage(getString(R.string.exit_msg))
              .setNegativeButton(android.R.string.no, null)
              .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface arg0, int arg1) { // Explorer.super.onBackPressed();
                    finish(); }
              }).create().show(); }
      else {
         ((View)findViewById(R.id.pref_container)).setVisibility(View.INVISIBLE);
      }
   }

   @Override
   public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

      if (key.equals(getString(R.string.key_autogps))) {
         // if (statePillar.shP.getBoolean(key, true))
            checkGPSpermission(); // to update title
      }
      if (key.equals(getString(R.string.key_srvc_flag))) {
         gps_status.setText(statePillar.shP.getString(key,".!.,"));
      }
   }

   void checkGPSpermission() {
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
              != PackageManager.PERMISSION_GRANTED &&
          ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
         statePillar
                 .disableHWbyPermissionTest(true);
         Log.i("Explorer","GPS is either denied or disabled");
         popTitle("PRM-");
         ActivityCompat.requestPermissions( this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
         // implicitly calls .connect() when called from async thread in onRequestPermissionsResult()
      } else {
         // we HAVE permissions needed and can go
         if (!geo_Clt.isConnected() || !geo_Clt.isConnecting()) geo_Clt.connect();
         statePillar
                 .disableHWbyPermissionTest(false);
         popTitle("PRM+");
      }
   }

   void popTitle(String hw_permission_red){
      String app_title = getResources().getString(R.string.app_name)+ " ";
      if (statePillar._auto)
         this.setTitle(app_title + "GPS+" + hw_permission_red);
      else
         this.setTitle(app_title + "GPS-" + hw_permission_red);
   }

   @Override
   public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
      switch (requestCode) {
         case PERMISSION_CODE: {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               // permission was granted
               geo_Clt.connect();
               //-statePillar.setGPSon(true);
               popTitle("PRM++");
               statePillar
                       .disableHWbyPermissionTest(false);
            } else {
               // permission denied
               // some kind of termination action must be implemented
               // i.e. disable property from selection
               statePillar.useGeoTargetting(StatePillar.MODE_MANUAL);
               popTitle("PRM#");
               statePillar
                       .disableHWbyPermissionTest(true);
            }
            return;
         }
         default:
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
      }
   }

   @Override
   public void onConnected(Bundle connectionHint) {
      //-statePillar.setGPSon(true);

      Location got_geoloc = LocationServices.FusedLocationApi.getLastLocation(geo_Clt);
      if (got_geoloc != null) {
         statePillar.setHWLocation(got_geoloc);
         statePillar.markGotGPS(true);
         if (statePillar._auto)
            statePillar.useGeoTargetting(StatePillar.MODE_HW);
         else
            statePillar.useGeoTargetting(StatePillar.MODE_MANUAL);
      }
   }

   @Override
   public void onConnectionSuspended(int i) {
      //-statePillar.setGPSon(false);
   }

   @Override
   public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
      //-statePillar.setGPSon(false);
   }

   @Override
   public boolean onPreferenceChange(Preference preference, Object newValue) {
      return false;
   }

   @Override
   public void onClick(View v) {
      if(v.getId() == R.id.btnCheck) {
         String micro_log = "man " + statePillar._man_loc
                 + "\nhw " + statePillar._hw_loc
//                 + "\ndad " + statePillar._dad_loc
//                 + "\nnow " + statePillar._now
                 + "\nnow " + statePillar.getCenterNow();
         gps_status.setText(micro_log);
      }
      Intent intent = new Intent(this, FetchPlacesIntentService.class);
      intent.setAction(FetchPlacesIntentService.ACTION_20);
      /*Bundle params = new Bundle();
      params.putString(FetchPlacesIntentService.yell_LOCATION, statePillar.getCenterNow());
      params.putString(FetchPlacesIntentService.yell_RADIUS, statePillar.getLookupRadius());
      params.putString(FetchPlacesIntentService.yell_RADIUS, statePillar.getLookupRadius());
      params.*/
      startService(intent);
      return;
   }

}
