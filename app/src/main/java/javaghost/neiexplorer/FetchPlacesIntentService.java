package javaghost.neiexplorer;

import android.app.IntentService;
import android.content.Intent;
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
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;


public class FetchPlacesIntentService extends IntentService {
   private static final String
           ACTION_20          = "javaghost.neiexplorer.action.20",
           EXTRA_CENTAL_POINT = "javaghost.neiexplorer.extra.CPOINT";

   public FetchPlacesIntentService() {
      super("FetchPlacesIntentService");
   }

/*   public static void startActionBaz(Context context, String param1, String param2) {
      Intent intent = new Intent(context, FetchPlacesIntentService.class);
      intent.setAction(ACTION_BAZ);
      intent.putExtra(EXTRA_PARAM1, param1);
      intent.putExtra(EXTRA_PARAM2, param2);
      context.startService(intent);
   }*/

   @Override
   protected void onHandleIntent(Intent intent) {
      if (intent != null) {
         final String action = intent.getAction();
         if (ACTION_20.equals(action)) {
            final String param1 = intent.getStringExtra(EXTRA_CENTAL_POINT);
            handleActionFoo(param1, param2);
         } else if (ACTION_BAZ.equals(action)) {
            final String param1 = intent.getStringExtra(EXTRA_PARAM1);
            final String param2 = intent.getStringExtra(EXTRA_PARAM2);
            handleActionBaz(param1, param2);
         }
      }
   }

   boolean doFull20Search(String location_, String radius_, String word_, String type_) {
      // https://maps.googleapis.com/maps/api/place/details/json?placeid=ChIJN1t_tDeuEmsRUsoyG83frY4&key=
      // https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=-33.8670522,151.1957362&type=store&radius=5000&key=
      // https://maps.googleapis.com/maps/api/place/nearbysearch/json?pagetoken= &key=
      String
              DEFAULT_SEARCH_WHAT = "atm",
              API_K = getResources().getString(R.string.geo_API_K),
              URL_ROOT = "https://maps.googleapis.com/maps/api/place/",
              _FULL_SEARCH       = "nearbysearch/json?location=%2$s&radius=%3$s&keyword=%4$s&type=%5$s&key=%1$s",
              _DETAILED_SEARCH   = "details/json?placeid=%2$s&key=%1$s",
              GET_PAGE           = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?pagetoken=%2Ss&key=%1$s";

      String try_url = "";
      try_url = URL_ROOT + String.format(_FULL_SEARCH, API_K, location_, radius_, word_, type_);

      String next_token = "next_page_token";
      ArrayList<JSONObject> all_places = new ArrayList<>();
      int loop, passes_left =3;
      do {
         String assembled_str = readJStringFromFarFarAway(try_url);
         if (assembled_str.length() == 5) // i.e. 'false'
            return false; // Aborted

         JSONObject bootObj = null;
         JSONArray place_cluster = null;
         try {
            bootObj = new JSONObject(assembled_str);
            if (!bootObj.getString("status").equals("OK"))
               return false; // Query was not success by any reason
            if (bootObj.has(next_token))
               next_token = bootObj.getString(next_token);

            place_cluster = bootObj.getJSONArray("results");
         } catch (JSONException e) {
            e.printStackTrace();
         }
         loop = 0;
         assert place_cluster != null;
         while (loop < 20 && !place_cluster.isNull(loop)) {
            try {
               all_places.add(place_cluster.getJSONObject(loop));
            } catch (JSONException e) {
               e.printStackTrace();
            }
            loop++;
         }
         passes_left--;
      }
      while (passes_left > 0 && next_token.length() > 15 && loop > 19);
      // do not more than 3 queries
      // until 20 results received
      // probability on next token exists
//    all_places -> dB;
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
