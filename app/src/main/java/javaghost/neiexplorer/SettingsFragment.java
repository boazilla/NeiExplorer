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

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
   // every change in preferences will be reflected in StatePillar object
   StatePillar reflection = StatePillar.getInstance();

   CheckBoxPreference use_imperial, use_auto;
   ListPreference lookup_distance, lookup_mode;
   MultiSelectListPreference keyset;
   EditTextPreference keyword, geoloc;
   Preference dadloc;
   int PLACE_PICKER_REQUEST = 1333;

   public SettingsFragment() {
      // Required empty public constructor
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      // load the preferences from an xml file and build the preferences ui
      addPreferencesFromResource(R.xml.app_preferences);
   }

   @Override
   public void onResume() {
      super.onResume();
      {
         use_imperial = (CheckBoxPreference) getPreferenceManager().findPreference("use_imperial");
         use_imperial.setOnPreferenceChangeListener(this);
         //onPreferenceChange(use_imperial, use_imperial.isChecked());
         lookup_distance = (ListPreference) getPreferenceManager().findPreference("lst_distance");
         lookup_distance.setOnPreferenceChangeListener(this);
         onPreferenceChange(lookup_distance, lookup_distance.getValue());
         //implicitly called for _imperial
      }

      {
         lookup_mode = (ListPreference) getPreferenceManager().findPreference("lst_mode");
         lookup_mode.setOnPreferenceChangeListener(this);
         keyset = (MultiSelectListPreference) getPreferenceManager().findPreference("lst_keytypes");
         keyset.setOnPreferenceChangeListener(this);
         keyword = (EditTextPreference) getPreferenceManager().findPreference("a_keyword");
         keyword.setOnPreferenceChangeListener(this);
         //onPreferenceChange(keyword, keyword.getText());
      }
      {
         dadloc = (Preference) getPreferenceManager().findPreference("dad_geoloc");
         dadloc.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
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
         geoloc = (EditTextPreference) getPreferenceManager().findPreference("manual_geoloc");
         geoloc.setOnPreferenceChangeListener(this);

         use_auto = (CheckBoxPreference) getPreferenceManager()
                 .findPreference(getString(R.string.key_autogps));
         use_auto.setOnPreferenceChangeListener(this);
         reflection._auto = use_auto.isChecked();
         onPreferenceChange(use_auto, use_auto.isChecked());
         if(!reflection.isHwDisabledByPermissionTest()){
            dadloc.setEnabled(true);
            use_auto.setEnabled(true);
         }
      }
   }

   public void onActivityResult(int requestCode, int resultCode, Intent data) {
      Log.d("FragmentCall", "catched, something at least...");
      if (requestCode == PLACE_PICKER_REQUEST) {
         if (resultCode == Activity.RESULT_OK) {
            Place place = PlacePicker.getPlace(getContext(), data);
            String gotName = place.getName().toString();
            String toastMsg = String.format("Place: %s", gotName);
            Log.i("Act.Result", toastMsg);
            dadloc.setSummary(gotName);
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

         case "use_auto":
            if (newValue.equals(Boolean.TRUE)) {
               preference.setSummary(R.string.sw_gps_on);
               reflection._auto = true;
            } else {
               preference.setSummary(R.string.sw_gps_off);
               reflection._auto = false;
            }
            break;

         case "lst_distance":
            double si_unt, imp_unt;
            if (newValue != null) {
               si_unt = Double.parseDouble(newValue.toString());
               imp_unt = Math.rint(si_unt * 6.21371d) / 10;
               si_unt = Math.rint(si_unt * 10) / 10;
               reflection._si_unt = si_unt;
               reflection._imp_unt = imp_unt;

               if (reflection._imperial) {
                  lookup_distance.setSummary("< " + imp_unt + " mi");
               } else {
                  lookup_distance.setSummary("< " + si_unt + " km");
               }
            }
            break;


         default:
            preference.setSummary(R.string.hello_blank_fragment);
      }
      return true;
   }

}
