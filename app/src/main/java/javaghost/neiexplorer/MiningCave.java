package javaghost.neiexplorer;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by JavaGhost on 7/12/2016.
 */
public class MiningCave extends SQLiteOpenHelper {

   static final String
      TBL_PLACES     = "neixpl_places",
      cl_pID         = "id",
      cl_GGLID       = "place_id",
      cl_LOC         = "location",
      cl_NAME        = "nameof_place",
      cl_VICI        = "vicinity",
      cl_TYPICON     = "icon_uri",
      cl_GGLURI      = "maps_uri",
      cl_FLG_PH      = "has_photo",
      crt_PLACES
  = "CREATE TABLE %1$s (%2$s INTEGER PRIMARY KEY AUTOINCREMENT, %3$s TEXT, %4$s TEXT, %5$s TEXT, %6$s TEXT, %7$s TEXT, %8$s TEXT, %9$s INTEGER)",
      TBL_CONTEXTS   = "neixpl_contexts",
      cl_cID         = "id",
      cl_CENTER      = "focal_loc",
      cl_TYPE        = "type_used",
      cl_WORD        = "word_used",
      cl_RADIUS      = "radius",
      crt_CONTEXTS
  = "CREATE TABLE %1$s (%2$s INTEGER PRIMARY KEY AUTOINCREMENT, %3$s TEXT, %4$s TEXT, %5$s TEXT, %6$s INTEGER)", // Integer ?!
      TBL_SYNTH      = "neixpl_synthviews",
      cl_vID         = "id",
      cl_IDcs        = "idc",
      cl_IDps        = "idp",
      cl_FAVORED     = "favored",
      cl_DISTANCE    = "focal_dist",
      crt_SYNTH
  = "CREATE TABLE %1$s (%2$s INTEGER PRIMARY KEY AUTOINCREMENT, %3$s INTEGER, %4$s INTEGER, %5$s INTEGER, %6$s INTEGER)" // Integer ?!
   ;

   @Override
   public void onCreate(SQLiteDatabase db) {
   String spell1 = String.format(crt_PLACES, TBL_PLACES, cl_pID, cl_GGLID, cl_LOC, cl_NAME, cl_VICI, cl_TYPICON, cl_GGLURI, cl_FLG_PH);
   String spell2 = String.format(crt_CONTEXTS, TBL_CONTEXTS, cl_cID, cl_CENTER, cl_TYPE, cl_WORD, cl_RADIUS);
   String spell3 = String.format(crt_SYNTH, TBL_SYNTH, cl_vID, cl_IDcs, cl_IDps, cl_FAVORED, cl_DISTANCE);
      db.execSQL(spell1 + ";" + spell2 + ";" + spell3);
   }
   @Override
   public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
   }

   public void upstore(JustPlace place_){

   }
   public void nextContxt(String focal_, String type_, String word_, String radius_ ){}
   public void nextContxt(LatLng focal_, String type_, String word_, double radius_ ){}
   public void nextContxt(/*StatePillar current_state*/){}
   public void storeSynth(JustPlace[] setPlaces, boolean[] favores){}
   public void updatePlaceURI(JustPlace for_place){   }
   public void updatePlaceFav(JustPlace for_place){ }

}
