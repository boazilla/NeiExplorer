package javaghost.neiexplorer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class Explorer extends AppCompatActivity {

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_explorer);
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
      switch (item.getItemId()){
         case R.id.menu_set:
            getFragmentManager().beginTransaction()
                    .replace(R.id.pref_container, new SettingsFragment())
                    .addToBackStack("settings") //add the action to the back stack. pressing back will undo the replace and will NOT close the activity!
                    .commit();
            break;

         case R.id.menu_clear:
            finish();
            break;
      }
      return super.onOptionsItemSelected(item);
   }
}
