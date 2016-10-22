package javaghost.neiexplorer;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;


public class FetchPlacesIntentService extends IntentService {
   public static final String
           ACTION_20          = "javaghost.neiexplorer.action.20",
//           yell_LOCATION      = "yell_location",
//           yell_RADIUS        = "yell_radius",
//           yell_WORD          = "yell_word",
//           yell_TYPE          = "yell_type",
           debug_title        = "FetchPlaces ** ";
   static final String
           st_ZERO_RESULTS    = "ZERO_RESULTS",
           st_REQUEST_DENIED  = "REQUEST_DENIED",
           st_OVER_QUERY_LIMIT= "OVER_QUERY_LIMIT",
           st_INVALID_REQUEST = "INVALID_REQUEST",
           st_OK              = "OK";
   ArrayList<JSONObject> all_places;
   StatePillar reflection = StatePillar.getInstance();

   public FetchPlacesIntentService() {
      super("FetchPlacesIntentService");
   }

   @Override
   protected void onHandleIntent(Intent intent) {
      if (intent != null) {
         final String action = intent.getAction();
         final Bundle params = intent.getExtras();
         if (ACTION_20.equals(action) && doFull20Search(reflection.getCenterNow(),
                 reflection.getLookupRadius(), reflection.getWordOfBlake(), reflection.getTypeWriter()))
         {
            pushPreference( String.valueOf(all_places.size()));
         }
      }
   }

   void pushPreference(String message_me){
      reflection.shE.putString(getString(R.string.key_srvc_flag), message_me);
      reflection.shE.commit();
   }

   boolean doFull20Search(String location_, String radius_, String word_, String type_) {
      // https://maps.googleapis.com/maps/api/place/details/json?placeid=ChIJN1t_tDeuEmsRUsoyG83frY4&key=
      // https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=-33.8670522,151.1957362&type=store&radius=5000&key=
      // https://maps.googleapis.com/maps/api/place/nearbysearch/json?pagetoken= &key=
      String
              DEFAULT_SEARCH_WHAT = "place_of_interest",
              API_K = getString(R.string.api_K_web),
              URL_ROOT = "https://maps.googleapis.com/maps/api/place/",
              _FULL_SEARCH_WT     = "nearbysearch/json?location=%2$s&radius=%3$s&keyword=%4$s&type=%5$s&key=%1$s",
              _FULL_SEARCH_W      = "nearbysearch/json?location=%2$s&radius=%3$s&keyword=%4$s&key=%1$s",
              _FULL_SEARCH_T      = "nearbysearch/json?location=%2$s&radius=%3$s&type=%5$s&key=%1$s",
              _FULL_SEARCH_D      = "nearbysearch/json?rankby=distance&location=%2$s&radius=%3$s&keyword=%4$s&type=%5$s&key=%1$s",
              _FULL_SEARCH_DN     = "nearbysearch/json?rankby=distance&location=%2$s&radius=%3$s&type=place_of_interest&key=%1$s",
              _DETAILED_SEARCH    = "details/json?placeid=%2$s&key=%1$s",
              GET_PAGE            = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?pagetoken=%2$s&key=%1$s";

      String try_url = "";

      if (reflection._by_keys && reflection._by_word)
         try_url = URL_ROOT + String.format(_FULL_SEARCH_WT, API_K, location_, radius_, word_, type_);
      else if (reflection._by_word)
         try_url = URL_ROOT + String.format(_FULL_SEARCH_W, API_K, location_, radius_, word_);
      else if (reflection._by_keys)
         try_url = URL_ROOT + String.format(_FULL_SEARCH_T, API_K, location_, radius_, type_);
      else if(!(reflection._by_keys && reflection._by_word))
         try_url = URL_ROOT + String.format(_FULL_SEARCH_DN, API_K, location_, radius_);
      pushPreference(try_url);
      String next_token = "next_page_token";
      all_places = new ArrayList<>();
      int loop, passes_left =3;
      do {
         String assembled_str = readJStringFromFarFarAway(try_url);
         if (assembled_str.length() < 10) // i.e. 'false'
         {
            Log.d(debug_title, "reply on POST: " +assembled_str);
            return false; // Aborted
         }

         JSONObject bootObj = null;
         JSONArray place_cluster = null;
         try {
            bootObj = new JSONObject(assembled_str);
            String status = bootObj.getString("status");
            switch (status) {
               case st_REQUEST_DENIED:
               case st_OVER_QUERY_LIMIT:
               case st_INVALID_REQUEST:
                  pushPreference(status);
                  return false;

               case st_ZERO_RESULTS:
                  break;
               case st_OK:
                  break;
            }
            if (bootObj.has("next_page_token"))
               next_token = bootObj.getString("next_page_token");

            place_cluster = bootObj.getJSONArray("results");
         } catch (JSONException e) {
            e.printStackTrace();
            pushPreference(e.getLocalizedMessage());
            return false;
         }
         pushPreference(next_token);
         loop = 0;
         assert place_cluster != null;
         while (loop < 20 && !place_cluster.isNull(loop)) {
            try {
               all_places.add(place_cluster.getJSONObject(loop));
            } catch (JSONException e) {
               e.printStackTrace();
               passes_left = 0;
            }
            loop++;
         }
         pushPreference(String.valueOf(all_places.size()));
         passes_left--;
         if (next_token.length() > 15 && passes_left > 0)
            try_url = String.format(GET_PAGE, API_K, next_token);
         else
            passes_left = 0;
      }
      while (passes_left > 0 && next_token.length() > 15 && loop > 19);
      // do not more than 3 queries
      // until 20 results received
      // probability on next token exists
      // all_places -> dB;
      return true;
   }

   String readJStringFromFarFarAway(String distant_star){
      String nextln, assembled_str = "";
      try {
         URL url = new URL(distant_star);
         HttpURLConnection open_pipe = (HttpsURLConnection) url.openConnection();
         if (open_pipe.getResponseCode() != HttpURLConnection.HTTP_OK) {
            Log.d("Get JSON","Https connection failed");
            return "" + false;
         }
         InputStream in_str = open_pipe.getInputStream();
         InputStreamReader in_reader = new InputStreamReader(in_str);
         BufferedReader buf_reader = new BufferedReader(in_reader);

         while ((nextln = buf_reader.readLine()) != null) {
            if (!nextln.endsWith("\n")) nextln += "\n";
            assembled_str += nextln;
         }

      } catch (IOException e) {
         Log.d("Full20","Failed to combine URL");
         return "" + false; // Aborted
      }
      return assembled_str;
   }
}
