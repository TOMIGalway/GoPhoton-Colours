package eu.gophoton.colours;
import java.util.Locale;

import eu.gophoton.colours.R;
import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends FragmentActivity {
	
ViewPager Tab;
    TabPagerAdapter TabAdapter;
  ActionBar actionBar;
    
  
    @Override
    protected void onCreate(Bundle savedInstanceState) {

		
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TabAdapter = new TabPagerAdapter(getSupportFragmentManager());
        Tab = (ViewPager)findViewById(R.id.pager);
        Tab.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                      actionBar = getActionBar();
                      actionBar.setSelectedNavigationItem(position);                    }
                });
        Tab.setAdapter(TabAdapter);
        actionBar = getActionBar();
        //Enable Tabs on Action Bar
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ActionBar.TabListener tabListener = new ActionBar.TabListener(){
      
      @Override
      public void onTabReselected(android.app.ActionBar.Tab tab,
          FragmentTransaction ft) {
        // TODO Auto-generated method stub
      }
      @Override
       public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
              Tab.setCurrentItem(tab.getPosition());
          }
      @Override
      public void onTabUnselected(android.app.ActionBar.Tab tab,
          FragmentTransaction ft) {
        // TODO Auto-generated method stub
      }};
      //Add New Tab
      actionBar.addTab(actionBar.newTab().setIcon(R.drawable.gp_menu1_50).setTabListener(tabListener));
      actionBar.addTab(actionBar.newTab().setIcon(R.drawable.gp_menu2_50).setTabListener(tabListener));
      actionBar.addTab(actionBar.newTab().setIcon(R.drawable.gp_menu3_50).setTabListener(tabListener));
      actionBar.addTab(actionBar.newTab().setIcon(R.drawable.gp_menu4_50).setTabListener(tabListener));    
    }
    
    
    
    //Added 25/8/14 - Add a menu bar for the language settings. The menu is created from language_menu.xml
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.language_menu, menu);
		return true;
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch(item.getItemId()){
		case R.id.english:
			changeLocale("en");
			break;
//		case R.id.irish:
	//		changeLocale("ga");
		//	break;
		case R.id.dutch:
			changeLocale("nl");
			break;
		case R.id.french:
			changeLocale("fr");
			break;
		case R.id.spanish:
			changeLocale("es");
			break;
		case R.id.catalan:
			changeLocale("ca");
			break;
		case R.id.german:
			changeLocale("de");
			break;
//		case R.id.italian:
//			changeLocale("it");
	//		break;
		case R.id.slovak:
			changeLocale("sk");
			break;
		case R.id.portuguese:
			changeLocale("pt");
			break;
		}
		return false;
	}
       
	
	private void changeLocale (String localeCode){
		
		Locale locale = null;		
	    
	    if (localeCode.equalsIgnoreCase(""))
	    	return;
	    
    	locale = new Locale(localeCode);	
    	saveLocale(localeCode);
    	Locale.setDefault(locale);
    	android.content.res.Configuration config = new android.content.res.Configuration();
    	config.locale = locale;
    	getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    	
        Resources res = getResources(); 
        DisplayMetrics dm = res.getDisplayMetrics(); 
        Configuration conf = res.getConfiguration(); 
        conf.locale = locale; 
        res.updateConfiguration(conf, dm); 
        Intent refresh = new Intent(this, MainActivity.class); 
        startActivity(refresh);
    	
    	

	}
	
    public void saveLocale(String lang)
    {
    	String langPref = "Language";
    	SharedPreferences prefs = getSharedPreferences("CommonPrefs", Activity.MODE_PRIVATE);
    	SharedPreferences.Editor editor = prefs.edit();
		editor.putString(langPref, lang);
		editor.commit();
    }
    


	
}