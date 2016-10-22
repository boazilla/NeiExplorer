package javaghost.neiexplorer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

/**
 * Created by JavaGhost on 7/12/2016.
 */
public class MiningCave extends SQLiteOpenHelper {

   SQLiteDatabase _db;
   StatePillar    reflection;

   static final String
      TBL_PLACES     = "neixpl_places",
      cl_pID         = "idp",
      cl_GGLID       = "place_id",
      cl_LOC         = "location",
      cl_NAME        = "nameof_place",
      cl_VICI        = "vicinity",
      cl_TYPICON     = "icon_uri",
      cl_GGLURI      = "maps_uri",
      cl_FLG_PH      = "has_photo",
      crt_PLACES
  = "CREATE TABLE %1$s (%2$s INTEGER PRIMARY KEY AUTOINCREMENT, %3$s TEXT NOT NULL unique, %4$s TEXT, %5$s TEXT, %6$s TEXT, %7$s TEXT, %8$s TEXT, %9$s INTEGER)",
      TBL_CONTEXTS   = "neixpl_contexts",
      cl_cID         = "idc",
      cl_CENTER      = "focal_loc",
      cl_TYPE        = "type_used",
      cl_WORD        = "word_used",
      cl_RADIUS      = "radius",
      cl_CNT_HASH    = "hash",
      crt_CONTEXTS
  = "CREATE TABLE %1$s (%2$s INTEGER PRIMARY KEY AUTOINCREMENT, %3$s TEXT, %4$s TEXT, %5$s TEXT, %6$s INTEGER, %7$s INTEGER)", 
      TBL_SYNTH      = "neixpl_synthviews",
      cl_vID         = "idv",
      cl_IDcs        = "idc",
      cl_IDps        = "idp",
      cl_FAVORED     = "favored",
      cl_DISTANCE    = "focal_dist",
      crt_SYNTH_FK
  = "CREATE TABLE %1$s (%2$s INTEGER NOT NULL, %3$s INTEGER NOT NULL, %4$s INTEGER, %5$s INTEGER, " +
     "FOREIGN KEY (%2$s) REFERENCES %6$s(%7$s), FOREIGN KEY (%3$s) REFERENCES %8$s(%9$s), PRIMARY KEY (%2$s, %3$s))",
      crt_SYNTH
  = "CREATE TABLE %1$s (%6$s INTEGER PRIMARY KEY AUTOINCREMENT, %2$s INTEGER NOT NULL, %3$s INTEGER NOT NULL, %4$s INTEGER, %5$s INTEGER)"
   ;
/*   CREATE TABLE table1 (
           table2_id INTEGER NOT NULL,
           table3_id INTEGER NOT NULL,
           FOREIGN KEY (table2_id) REFERENCES table2(id),
           FOREIGN KEY (table3_id) REFERENCES table3(id),
           PRIMARY KEY (table2_id, table3_id)
   );*/

   public MiningCave (Context context) {
      super(context, "neixpl.db", null, 1);
      _db = getWritableDatabase();
      reflection = StatePillar.getInstance();
   }

   @Override
   public void onCreate(SQLiteDatabase db) {
   String spell1 = String.format(crt_PLACES, TBL_PLACES, cl_pID, cl_GGLID, cl_LOC, cl_NAME, cl_VICI, cl_TYPICON, cl_GGLURI, cl_FLG_PH);
   String spell2 = String.format(crt_CONTEXTS, TBL_CONTEXTS, cl_cID, cl_CENTER, cl_TYPE, cl_WORD, cl_RADIUS, cl_CNT_HASH);
   String spell3 = String.format(crt_SYNTH_FK, TBL_SYNTH, cl_IDcs, cl_IDps, cl_FAVORED, cl_DISTANCE, TBL_CONTEXTS, cl_cID, TBL_PLACES, cl_pID);
   String spell4 = String.format(crt_SYNTH, TBL_SYNTH, cl_IDcs, cl_IDps, cl_FAVORED, cl_DISTANCE, cl_vID);
      db.execSQL(spell1 + ";" + spell2 + ";" );
      db.execSQL(spell4);
   }
   @Override
   public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
   }

   public long lastContext(){
      long maxid = 0;
      Cursor cursor = _db.query(TBL_CONTEXTS, null, "maxid=(SELECT MAX(idc) FROM " + TBL_CONTEXTS + ")", null, null, null, null);
      if (cursor != null) {
         cursor.moveToFirst();
         maxid = cursor.getLong(0);
         cursor.close();
      }
      return maxid;
   }
   //   public void updatePlaceURI(JustPlace for_place){   } -- done by upstore(JustPlace)
   public long upstore(JustPlace that){
      ContentValues cv = new ContentValues();
        cv.put(cl_GGLID, that._place_id);
        cv.put(cl_LOC, ""+that._location.latitude+","+that._location.longitude);
        cv.put(cl_NAME, that._name);
        cv.put(cl_VICI, that._vicinity);
        cv.put(cl_TYPICON, that._icon_uri);
        cv.put(cl_GGLURI, that._google_uri);
        cv.put(cl_FLG_PH, (that._has_photo)? 1 : 0 );
      long rowID;
      rowID = included(that);
      if ( rowID > 0) {
         if (that._place_id.length() > 0)
            rowID = _db.update(TBL_PLACES, cv, cl_GGLID + " =?", new String[]{that._place_id});
      // else -- no update needed, no insert possible ->> just return record number
      // implicitly this avoids blind updates with loss of place_uri BUT outdated info could happen to stay in DB
      }
      else
         rowID = _db.insert(TBL_PLACES, null, cv);
      return rowID;
   }
   public long included(JustPlace that) {
      return included(that._place_id);
   }
   public long included(String place_ggl_id){
      SQLiteDatabase db = getReadableDatabase();
      long got_id = 0;
      Cursor c = db.query(TBL_PLACES, new String[] {cl_pID},
              cl_GGLID +" =?", new String[] {place_ggl_id},
              null, null, null);
      int found = c.getCount();
      if (found > 0) {
         c.moveToFirst();
         got_id = c.getLong(0);
         c.close();
      }
      return got_id;
   }
//   public void nextContxt(String focal_, String type_, String word_, String radius_ ){}
//   public void nextContxt(LatLng focal_, String type_, String word_, double radius_ ){}
   public long getContxt(/*StatePillar current_state*/){
      SQLiteDatabase db = getWritableDatabase();
      long got_id = lastContext();
      Cursor c = db.query(TBL_CONTEXTS, new String[] {cl_cID},
              cl_CNT_HASH +" =?", new String[] {reflection.hashStString()},
              null, null, null);
      if (c.getCount() >0) {
         c.moveToFirst();
         long goal = c.getLong(0);
         c.close();
         return goal;
      } else
      {
         ContentValues cv = new ContentValues();
         cv.put(cl_CENTER, reflection.getCenterNow());
         cv.put(cl_TYPE,   reflection.getTypeWriter());
         cv.put(cl_WORD,   reflection.getWordOfBlake());
         cv.put(cl_RADIUS, reflection.getLookupRadius());
         cv.put(cl_CNT_HASH, reflection.hashStString());

         return db.insert(TBL_CONTEXTS, null, cv);
      }
   }

   public long storeSynth(JustPlace[] setof_places){
      SQLiteDatabase db = getWritableDatabase();
      ContentValues cv = new ContentValues();
      long lastxt = lastContext();
      long contxt = getContxt();
      float[] distances = new float[3];
      if (lastxt == contxt) {
         return 0; // no data were written as lookup context was previously queried;
      }
      else {
         
//    TODO   LOOP, but 8( not unique checked for cntxt*place, IRL should omit update as lookup was done before
         cv.put(cl_vID, contxt);
         cv.put(cl_IDps, upstore(setof_places[idx]));
         cv.put(cl_FAVORED, false);
         Location.distanceBetween(reflection._now.latitude, reflection._now.longitude, setof_places[idx]._location.latitude, setof_places[idx]._location.longitude, distances);
         cv.put(cl_DISTANCE, distances[0]);

         return db.insert(TBL_SYNTH, null, cv);
         // BTW return count::
      }
   }
//   public void updateFav(JustPlace for_place){  }
   public long upstore(JustPlace that, boolean is_favored){
      ContentValues cv = new ContentValues();
      cv.put(cl_FAVORED, (is_favored)? 1 : 0);
      long rowID = lastContext();
      String cntxt_row = String.valueOf(rowID);
      String place_row = String.valueOf(included(that));
      if ( rowID > 0) { // paranoid am I? .true
         if (that._place_id.length() > 0)
            rowID = _db.update(TBL_SYNTH, cv, cl_IDcs + " =?" + " " + cl_IDps + " =?", new String[]{cntxt_row, place_row});
         // logic says update context should be adjusted
      }
      else
         rowID = _db.insert(TBL_PLACES, null, cv);
      return rowID;
   }


}
