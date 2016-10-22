package javaghost.neiexplorer;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

public class SettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {
   // every change in preferences will be reflected in StatePillar object
   StatePillar reflection = StatePillar.getInstance();

   CheckBoxPreference use_imperial, use_auto;
   ListPreference lookup_distance, lookup_mode;
   MultiSelectListPreference keyset;
   EditTextPreference keyword, manual_loc;
   Preference dad_loc;
   int PLACE_PICKER_REQUEST = 1333;

   public SettingsFragment() {
      // Required empty public constructor
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      // load the preferences from an xml file and build the preferences ui
      addPreferencesFromResource(R.xml.app_preferences);
      //*REM* setHasOptionsMenu(true);
   }

  /* @Override
   public void onDestroy(){
      View my_container = getActivity().findViewById(R.id.pref_container);
//      my_container.setBackgroundColor(transparent);
      my_container.setVisibility(View.INVISIBLE);
      super.onDestroy();
   }
   */
   @Override
   public void onResume() {
      super.onResume();
      {
         use_imperial = (CheckBoxPreference) getPreferenceManager().findPreference("use_imperial");
         use_imperial.setOnPreferenceChangeListener(this);
         lookup_distance = (ListPreference) getPreferenceManager().findPreference("lst_distance");
         lookup_distance.setOnPreferenceChangeListener(this);
         onPreferenceChange(lookup_distance, lookup_distance.getValue());
         //implicitly called for _imperial
      }

      {
         lookup_mode = (ListPreference) getPreferenceManager().findPreference("lst_mode");
         lookup_mode.setOnPreferenceChangeListener(this);
//         lookup_mode.setSummary("" +lookup_mode.getValue());
         keyset = (MultiSelectListPreference) getPreferenceManager().findPreference("lst_keytypes");
         keyset.setOnPreferenceChangeListener(this);
         keyword = (EditTextPreference) getPreferenceManager().findPreference("a_keyword");
         keyword.setOnPreferenceChangeListener(this);
      }
      {
         dad_loc = (Preference) getPreferenceManager().findPreference("dad_geoloc");
         dad_loc.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

               PlacePicker.IntentBuilder builder;
               builder = new PlacePicker.IntentBuilder();
               try {

                  startActivityForResult(builder.build(getActivity()), PLACE_PICKER_REQUEST);
                  Log.d("FragmentCall", "launched");
               } catch (GooglePlayServicesRepairableException e) {
                  Log.d("FragmentCall", "launch failed " + e.getConnectionStatusCode());
                  e.printStackTrace();
               } catch (GooglePlayServicesNotAvailableException e) {
                  e.printStackTrace();
               }
               return true;
            /*Uri gmmIntentUri = Uri.parse("geo:32.064,34.790?z=10&q=restaurants");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
//          if (mapIntent.resolveActivity(getPackageManager()) != null) {
               startActivity(mapIntent);
//            }
         return true;*/
            }
         });
         if (reflection._hw_loc != null)
            dad_loc.setSummary("last " + reflection._hw_loc);
         manual_loc = (EditTextPreference) getPreferenceManager().findPreference("manual_geoloc");
         manual_loc.setOnPreferenceChangeListener(this);
         manual_loc.setSummary( manual_loc.getText());
         
         use_auto = (CheckBoxPreference) getPreferenceManager()
                 .findPreference(getString(R.string.key_autogps));
         use_auto.setOnPreferenceChangeListener(this);
         onPreferenceChange(use_auto, use_auto.isChecked());
         if(!reflection.isHwDisabledByPermissionTest()){
            dad_loc.setEnabled(true);
            use_auto.setEnabled(true);
         }
      }
   }

   public void onActivityResult(int requestCode, int resultCode, Intent data) {
      Log.d("FragmentCall", "catched, something at least...");
      if (requestCode == PLACE_PICKER_REQUEST) {
         if (resultCode == Activity.RESULT_OK) {
            Place place = PlacePicker.getPlace(getActivity(), data);
            if (place != null) {
               reflection.setDadLocation(place.getLatLng());
               reflection.useGeoTargetting(StatePillar.MODE_HW);
               dad_loc.setSummary(place.getLatLng().toString() + " in use");
            }
         } else {
            dad_loc.setSummary("back pressed");
            if (!reflection.isGotGPS()) // no GPS location red at least once this session
               reflection.useGeoTargetting(StatePillar.MODE_MANUAL);
         }
      }
   }

   @Override
   public boolean onPreferenceChange(Preference preference, Object newValue) {
      // assume we use Android Studio 0.3.2 or later ;)
      // but still cases use constants... 8(
      switch (preference.getKey()) {

         case "use_imperial":
            if (newValue.equals(Boolean.TRUE)) {
               preference.setSummary(R.string.sw_imp_on);
               reflection._imperial = true;
            } else {
               preference.setSummary(R.string.sw_imp_off);
               reflection._imperial = false;
            }
            onPreferenceChange(lookup_distance, lookup_distance.getValue());
            break;

         case "lst_distance":
            double si_unt, imp_unt;
            if (newValue != null) {
               si_unt  = Double.parseDouble(newValue.toString());
               imp_unt = Math.rint(si_unt * 6.21371d) / 10;
               si_unt  = Math.rint(si_unt * 10) / 10;
               reflection._si_unt  = si_unt;
               reflection._imp_unt = imp_unt;

               if (reflection._imperial) {
                  lookup_distance.setSummary("< " + imp_unt + " mi");
               } else {
                  lookup_distance.setSummary("< " + si_unt + " km");
               }
            }
            break;

         case "use_auto":
            if (newValue.equals(Boolean.TRUE)) {
               preference.setSummary(R.string.sw_gps_on);
               reflection._auto = true;
            } else {
               preference.setSummary(R.string.sw_gps_off);
               reflection._auto = false;
            }
            // no break!!
         case "manual_geoloc":
            reflection.setManualLocation(newValue.toString());
            String my_smmy = "("+reflection._man_loc.latitude+","
                    + reflection._man_loc.longitude + ")";
            if (!reflection._auto) {
               my_smmy += " in use";
               reflection.useGeoTargetting(StatePillar.MODE_MANUAL);
            }
            else {
               my_smmy += " not in use";
            }
            manual_loc.setSummary(my_smmy);
            if (preference.getKey().equals("manual_geoloc")) preference.getEditor().commit();
            break;
         case "lst_mode":
            String keyval = ((ListPreference)preference).getValue();
            switch (keyval) {
               case "word":
                  reflection._by_word = true;
                  reflection._by_keys = false;
                  break;
               case "list":
                  reflection._by_word = false;
                  reflection._by_keys = true;
                  break;
               case "combo":
                  reflection._by_word = true;
                  reflection._by_keys = true;
                  break;
               default: // 'around' or whatever
                  reflection._by_word = false;
                  reflection._by_keys = false;
                  break;
            }
            preference.setSummary(keyval);
            break;

         default:
            preference.setSummary(R.string.hello_blank_fragment);
      }
      return true;
   }

}
