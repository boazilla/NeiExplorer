package javaghost.neiexplorer;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by JavaGhost on 7/11/2016.
 */
public class JustPlace {
   // internal implementation of Google Place API JSon object
   LatLng _location;
   String _icon_uri;
   String _name;
   String _vicinity;
   boolean _has_photo;
   String _place_id;

   public JustPlace(JSONObject obj_) {
      try {
         JSONObject try_o = obj_.getJSONObject("geometry");
         try_o = try_o.getJSONObject("location");
         _location = new LatLng(
                 Double.parseDouble(try_o.getString("lat")),
                 Double.parseDouble(try_o.getString("lng")));
         _icon_uri = obj_.getString("icon");
         _name = obj_.getString("name");
         _place_id = obj_.getString("place_id");
         _vicinity = obj_.getString("vicinity");
         JSONArray try_a = obj_.getJSONArray("photos");
         _has_photo = true;
         if (try_a.isNull(0)) _has_photo = false;
      } catch (Exception e) {
         _location = new LatLng(0, 0);
         _icon_uri = "https://greenaddress.it/static/img/pages/index/icon-no.png";
         _name = ".empty";
         _place_id = "ChIJLfFKZjCuEmsRmrYDhBV-XCc";
         _vicinity = ".empty";
         _has_photo = false;
      }

   }
}
