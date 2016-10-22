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

   double
           _lookup,    // distenace in meters
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
           MODE_PLACE =1, MODE_MANUAL =2, MODE_HW =3;
   /*static final boolean
           GPS_ON =true, GPS_OFF =false;*/

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

   protected void readPreferences() {
      Resources res = host_context.getResources();
      String key;
      key = res.getString(R.string.key_imperial);
      if (shP.contains(key)) {
         _imperial = shP.getBoolean(key, false);
      } else _imperial = false;

      key = res.getString(R.string.key_autogps);
      if (shP.contains(key)) {
         _auto = shP.getBoolean(key, false);
      } else _auto = false;

      key = res.getString(R.string.key_distance);
      if (shP.contains(key)) {
         _lookup = shP.getFloat(key, 1.61f);
      } else _lookup = 1.61f;
      _imp_unt = Math.rint(_lookup * 6.21371f) / 10;
      _si_unt = Math.rint(_lookup * 10) / 10;

      key = res.getString(R.string.key_keytypes);
      if (shP.contains(key)) {
         Set<String> set_ = shP.getStringSet(key, null);
         _keyset = set_.toArray(new String[set_.size()]);
      } else {
         _keyset = new String[]{""};
         _by_keys = false;
      }

      key = res.getString(R.string.key_keyword);
      if (shP.contains(key)) {
         _keyword = shP.getString(key, "point_of_interest");
      } else {
         _keyword = "point_of_interest";
         _by_word = false;
      }

      key = res.getString(R.string.key_mode);
      if (shP.contains(key)) {
         String keyval = shP.getString(key, "around");
         switch (keyval) {
            case "word":
               _by_word = true;
               _by_keys = false;
               break;
            case "list":
               _by_word = false;
               _by_keys = true;
               break;
            case "combo":
               _by_word = true;
               _by_keys = true;
               break;
            default: // 'around' or whatever
               _by_word = false;
               _by_keys = false;
               break;
         }
      } else {
         _by_word = false;
         _by_keys = false;
      }

      _null = fromDoubleGeoString(res.getString(R.string.null_geoloc), "");
      key = res.getString(R.string.key_man_geoloc);
      String keyval_ = res.getString(R.string.null_geoloc);
      if (shP.contains(key)) {
         keyval_ = shP.getString(key, res.getString(R.string.null_geoloc));
         if (keyval_.isEmpty() || !keyval_.contains(".") || !keyval_.contains(",") || keyval_.length() < 5)
            keyval_ = res.getString(R.string.null_geoloc);
      }
      _man_loc = fromDoubleGeoString(keyval_, res.getString(R.string.null_geoloc));

      if (!_auto) {
         _hw_loc = _null;
      } else {
         // TODO
//         https://developer.android.com/training/location/retrieve-current.html
//         and LocationSettings management
//         https://developer.android.com/training/location/change-location-settings.html
//         _now, _dad_loc are both affected by getLastLocation();
      }
   }

   public void setManualLocation(String double_geo_string) {
      _man_loc = fromDoubleGeoString(double_geo_string, host_context.getResources()
              .getString(R.string.null_geoloc));
   }
   public void setHWLocation(Location api_last) {
      _hw_loc = new LatLng(api_last.getLatitude(), api_last.getLongitude());
   }
   public void setDadLocation(LatLng place_pick) {
      _dad_loc = place_pick;
   }

   public void useLocationMode(int of_kind){
      switch (of_kind) {
         case MODE_HW:
            _now = (_hw_loc == null) ? _null : _hw_loc;
            break;
         case MODE_PLACE:
            _now = (_dad_loc ==null) ? _null : _dad_loc;
            break;
         default:
         case MODE_MANUAL:
            _now = (_man_loc ==null) ? _null : _man_loc;
            break;
      }
   }

   static LatLng fromDoubleGeoString(String double_geo_string, String default_sample) {
      double latitude, longitude;
      String[] LL;

         LL =  double_geo_string.split(",");
         if(LL[0].isEmpty() || LL[1].isEmpty())
            LL = default_sample.split(",");
         try {
            latitude = Double.parseDouble(LL[0]);
            longitude = Double.parseDouble(LL[1]);
         } catch (NumberFormatException e){
            LL = default_sample.split(",");
            latitude = Double.parseDouble(LL[0]);
            longitude = Double.parseDouble(LL[1]);
         }

      return new LatLng (latitude, longitude);
   }

   public void setGPSon(boolean is_enabled){
      shE.putBoolean(res.getString(R.string.key_autogps), is_enabled);
      shE.commit();
      _auto = is_enabled;
      if (!is_enabled) {
      }
   }
}
