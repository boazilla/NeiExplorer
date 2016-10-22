package javaghost.neiexplorer;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.preference.PreferenceManager;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.model.LatLng;

import java.util.Set;

/**
 * Created by JavaGhost on 7/2/2016.
 */
public class StatePillar {
   // shared resources
   private static StatePillar _instance = null;
   static Context host_context;
   static Resources res;
   static GoogleApiClient _api_Clt;
   static SharedPreferences shP;
   static SharedPreferences.Editor shE;

   /* preference fields */
   boolean
           _imperial,  // use imperial units
           _auto;      // use auto geolocation as source
   protected boolean
           _denied_hw, // if hw reading is denied per app.
           _first_run,
           _got_gps_once;

   double
           _lookup,    // distenace in Kilo-meters
           _si_unt,    // formatted representation of distance in SI
           _imp_unt;   // formatted as imperial
   // TODO rework 'formatted' as methods
   String[]
           _keyset;    // set of keys selected for criteral search
   String
           _keyword;   // keyword used in cretarial search if present
   boolean
           _by_keys,   // use keys in search criteria
           _by_word;   // use keyword in search criteria
   // false + false means 'everything for territory'
   LatLng
           _null,      // R.string.null_geoloc parsed;
           _now,       // mean geoposition, used as center despite it is or not real
           _dad_loc,   // retrived from location picker
           _man_loc,   // manually overriden position
           _hw_loc;    // retrieved from gps sensor
   static final int
           MODE_MANUAL =2, MODE_HW =3;

   protected StatePillar() {
      // Exists only to defeat instantiation but...
   }

   public static SharedPreferences.Editor linkEditor() {
      return shP.edit();
   }

   public static StatePillar linkToInstance(Context main_context, GoogleApiClient api_Clt) {
      if (_instance == null) {
         _instance = new StatePillar();
         host_context = main_context.getApplicationContext();
         res = host_context.getResources();
         shP = PreferenceManager.getDefaultSharedPreferences(host_context);
         shE = linkEditor();
         _api_Clt = api_Clt;

      }
      return _instance;
   }
   public static StatePillar getInstance() {
      return _instance;
   }

   public void updateFields() {
      Resources res = host_context.getResources();
      String key;
      key = res.getString(R.string.key_imperial);
      if (shP.contains(key)) {
         _imperial = shP.getBoolean(key, false);
      } else {_imperial = false;
         shE.putBoolean(key, _imperial);
      }

      key = res.getString(R.string.key_autogps);
      if (shP.contains(key)) {
         _auto = shP.getBoolean(key, true);
      } else {
         _auto = false;
         shE.putBoolean(key, _auto);
      }

      // use optimistic assumption - we have permissions per app. level
      disableHWbyPermissionTest(false);
      _first_run = false;
      key = res.getString(R.string.key_disabled);
      if (shP.contains(key)) {
         _denied_hw = shP.getBoolean(key, false); // using optimistic assumption
      } else {
         _first_run = true;
         _denied_hw = false;                  // no perm.test done yet bc. of first run
      }

      key = res.getString(R.string.key_distance);
      if (shP.contains(key)) {
         _lookup = Double.parseDouble(shP.getString(key, "1.61"));
      } else { _lookup = 1.61;
         shE.putString(key,  "1.61");
      }
      _imp_unt = Math.rint(_lookup * 6.21371d) / 10;
      _si_unt = Math.rint(_lookup * 10) / 10;

      key = res.getString(R.string.key_keytypes);
      if (shP.contains(key)) {
         Set<String> set_ = shP.getStringSet(key, null);
         _keyset = set_.toArray(new String[set_.size()]);
      } else {
         _keyset = new String[]{"atm"};
         _by_keys = false;
      }

      key = res.getString(R.string.key_keyword);
      if (shP.contains(key)) {
         _keyword = shP.getString(key, "point_of_interest");
      } else {
         _keyword = "point_of_interest";
         _by_word = false;
         shE.putString(key, _keyword);
      }

      key = res.getString(R.string.key_mode);
      if (!shP.contains(key)) {
         _by_word = false;
         _by_keys = false;
      }

      _null = fromDoubleGeoString(res.getString(R.string.null_geoloc), "");
      key = res.getString(R.string.key_man_geoloc);
      String keyval_ = res.getString(R.string.null_geoloc);
      _man_loc = _null;
      if (shP.contains(key)) {
         keyval_ = shP.getString(key, res.getString(R.string.null_geoloc));
         if (keyval_.isEmpty() || !keyval_.contains(".") || !keyval_.contains(",") || keyval_.length() < 5)
            keyval_ = res.getString(R.string.null_geoloc);
      } else {
         shE.putString(key, keyval_);
      }
      _man_loc = fromDoubleGeoString(keyval_, res.getString(R.string.null_geoloc));

      if (!_auto) {
         _hw_loc  = _null;
         _now     = _man_loc;
      } else {
//         https://developer.android.com/training/location/retrieve-current.html
//         https://developer.android.com/training/location/change-location-settings.html
         _now     = _null;
      }
      _got_gps_once = false;
      shE.commit();
   }

   public void disableHWbyPermissionTest(boolean found_lock){
      _denied_hw = (found_lock);
      shE.putBoolean(res.getString(R.string.key_disabled), found_lock);
      shE.apply();
   }

   public boolean isHwDisabledByPermissionTest() { return _denied_hw; }
   public boolean isFirstRun() { return _first_run; }
   public void markGotGPS(boolean mark) { _got_gps_once = mark;}
   public boolean isGotGPS() {return _got_gps_once; }
   public String getCenterNow() {
      return String.format("%1$2.5f,%2$2.5f",_now.latitude,_now.longitude);
   }
   public String getLookupRadius() {
      return "" + Math.round(1000 * (float)_lookup);
   }
   public String getWordOfBlake() {
      String try_ = _keyword.toLowerCase();
      try_.trim();
      try_.replace(" ", "%20");
      try_.replace("\t", "%20");
      try_.replace("\n", "%20");

      return try_;
   }
   public String getTypeWriter() {
      return "atm";
      // TODO something is about to change isnt it?
   }
   public void setManualLocation(String double_geo_string) {
      _man_loc = fromDoubleGeoString(double_geo_string, host_context.getResources()
              .getString(R.string.null_geoloc));
      //_now     = _man_loc;
   }
   public void setHWLocation(Location api_last) {
      _hw_loc  = new LatLng(api_last.getLatitude(), api_last.getLongitude());
      //_now     = _hw_loc;
   }
   public void setDadLocation(LatLng place_pick) {
      _dad_loc = place_pick;
      _hw_loc = _dad_loc;
   }

   public void useGeoTargetting(int of_kind){
      switch (of_kind) {
         case MODE_HW:
            _now = (_hw_loc == null) ? _null : _hw_loc;
            break;

         default:
         case MODE_MANUAL:
            _now = (_man_loc ==null) ? _null : _man_loc;
            shE.putString(res.getString(R.string.key_man_geoloc)
                    , "" + _now.latitude + "," + _now.longitude);
            shE.apply();
            break;
      }
   }

   static LatLng fromDoubleGeoString(String double_geo_string, String default_sample) {
      double latitude, longitude;
      String[] LL;
      try {
         LL =  double_geo_string.split(",");
         if(LL[0].isEmpty() || LL[1].isEmpty())
            LL = default_sample.split(",");

            latitude = Double.parseDouble(LL[0]);
            longitude = Double.parseDouble(LL[1]);
         } catch (Exception e){
            LL = default_sample.split(",");
            latitude = Double.parseDouble(LL[0]);
            longitude = Double.parseDouble(LL[1]);
         }

      return new LatLng (latitude, longitude);
   }

   /*public void setGPSon(boolean is_enabled){
      shE.putBoolean(res.getString(R.string.key_autogps), is_enabled);
      shE.commit();
      _auto = is_enabled;
   }*/
}
