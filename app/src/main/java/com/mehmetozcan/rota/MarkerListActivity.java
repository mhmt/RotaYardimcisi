package com.mehmetozcan.rota;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.media.ExifInterface;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.hudomju.swipe.SwipeToDismissTouchListener;
import com.hudomju.swipe.adapter.ListViewAdapter;
import com.mehmetozcan.rota.Utils.ListAdapter;


import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;

public class MarkerListActivity extends AppCompatActivity {
    Toolbar toolbar;
    ListView listView ;
    Spinner routeSelect;
    ArrayList<JSONObject> titles = new ArrayList<>();
    ListAdapter adapter;
    String selectedRoute="varsayılan";

    public static boolean mSortable = false;
    public static JSONObject mDragString;
    public static int mPosition = -1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_marker_list);

        toolbar = (Toolbar) findViewById(R.id.tool_bar);
        toolbar.setTitle("Rota Düzenle");
        toolbar.setTitleTextColor(Color.WHITE);

        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        listView = (ListView)findViewById(R.id.marker_lv);

        final SharedPreferences prefs = getSharedPreferences("routes",MODE_PRIVATE);

        routeSelect = (Spinner) findViewById(R.id.rota_select);
        updateRoutes(routeSelect);

        routeSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                titles.clear();
                try{
                    JSONArray routes = new JSONArray(prefs.getString("AllRoutes","[]"));
                    Log.d("json", "onCreate: Routes: "+ routes.toString());
                    if(routes.length() > 0){
                        ArrayList<LatLng> points = new ArrayList();

                        JSONObject route = routes.getJSONObject(position);
                        Log.d("select", "onItemSelected: Selected: "+route.getString("RouteName"));
                        JSONArray locations = route.getJSONArray("locations");
                        for (int i = 0; i < locations.length(); i++) {
                            JSONObject latlong = locations.getJSONObject(i);

                            titles.add(latlong);
                        }
                        adapter = new ListAdapter(MarkerListActivity.this,titles);
                        listView.setAdapter(adapter);
                        selectedRoute = route.getString("RouteName");
                    }
                }catch (Exception e){
                    Log.d("json", "onCreate: "+e.getMessage());
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                titles.clear();
                adapter.notifyDataSetChanged();
            }
        });

       listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
           @Override
           public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
               AlertDialog.Builder dialog ;
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                   dialog = new AlertDialog.Builder(MarkerListActivity.this, android.R.style.Theme_Material_Dialog_Alert);
               } else {
                   dialog = new AlertDialog.Builder(MarkerListActivity.this);
               }
               dialog.setTitle("Düzenle");
               dialog.setMessage("Bu durak noktasını ne yapmak istiyorsunuz?");


               dialog.setNeutralButton("İptal", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {

                       dialog.dismiss();
                   }
               });

               dialog.setNegativeButton("Noktayı Düzenle", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                      // Log.d("edit", "onClick: "+titles.get(position).toString());
                       editRoute(position);
                   }
               });

               dialog.setPositiveButton("Noktayı Sil!", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                        titles.remove(position);
                        adapter.notifyDataSetChanged();
                        SaveAsJSON(titles,selectedRoute);
                   }
               });

               dialog.show();
               return false;
           }
       });


        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {

                if (!mSortable) {

                    return false;
                }


                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {

                        int position = listView.pointToPosition((int) event.getX(), (int) event.getY());
                        if (position < 0) {
                            break;
                        }

                        if (position != mPosition) {
                            mPosition = position;
                            titles.remove(mDragString);
                            titles.add(mPosition,mDragString);
                            adapter.notifyDataSetChanged();
                        }
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_OUTSIDE: {
                        stopDrag();
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.delete:
                AlertDialog.Builder dialog ;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    dialog = new AlertDialog.Builder(MarkerListActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                } else {
                    dialog = new AlertDialog.Builder(MarkerListActivity.this);
                }
                dialog.setTitle("Sil");
                dialog.setMessage("Bu ROTA'yı silmek istediğinize emin misiniz?");


                dialog.setNeutralButton("İptal", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                    }
                });

                dialog.setPositiveButton("Evet, Sil !", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DeleteRouteAndSave(routeSelect.getSelectedItemPosition());
                    }
                });

                dialog.show();

                break;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit, menu);
        return true;
    }

    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    public void startDrag(JSONObject string) {
        mPosition = -1;
        mSortable = true;
        mDragString = string;
        adapter.notifyDataSetChanged(); // ハイライト反映・解除の為
    }

    public void stopDrag() {
        mPosition = -1;
        mSortable = false;
        mDragString = null;

        adapter.notifyDataSetChanged();
        SaveAsJSON(titles,selectedRoute);
    }

    private void updateRoutes(Spinner routeSelect){
        final SharedPreferences prefs = getSharedPreferences("routes",MODE_PRIVATE);
        try{
            JSONArray routes = new JSONArray(prefs.getString("AllRoutes","[]"));
            Log.d("json", "onCreate: Routes: "+ routes.toString());
            if(routes.length() > 0){
                List<String> routeNames = new ArrayList<String>();

                for (int i = 0; i < routes.length(); i++) {
                    JSONObject route = routes.getJSONObject(i);
                    routeNames.add(route.getString("RouteName"));
                }
                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(MarkerListActivity.this, android.R.layout.simple_spinner_item,routeNames);
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                routeSelect.setAdapter(dataAdapter);
                routeSelect.setSelection(routeNames.size()-1);
                selectedRoute = routeSelect.getSelectedItem().toString();

            }
        }catch (Exception e){
            Log.d("json", "onCreate: "+e.getMessage());
        }
    }

    private void SaveAsJSON(ArrayList<JSONObject> list, String routeName){
        JSONObject jsonObject = new JSONObject();

        try{

            jsonObject.put("RouteName",routeName);
            JSONArray locs = new JSONArray();
            for (int i = 0; i<list.size();i++){
                JSONObject each = list.get(i);
                locs.put(each);
            }
            jsonObject.put("locations",locs);
            Log.d("json", "SaveAsJSON: JSON: "+jsonObject.toString());

            SharedPreferences prefs = getSharedPreferences("routes",MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            JSONArray allRoutes = new JSONArray(prefs.getString("AllRoutes","[]"));
            allRoutes.put(routeSelect.getSelectedItemPosition(),jsonObject);


            editor.putString("AllRoutes",allRoutes.toString());
            editor.apply();

        }catch (Exception e){
            Log.d("json", "SaveAsJSON: "+e.getMessage());
        }

    }

    private void DeleteRouteAndSave(int position){


        try{

            SharedPreferences prefs = getSharedPreferences("routes",MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            JSONArray allRoutes = new JSONArray(prefs.getString("AllRoutes","[]"));
            allRoutes.remove(position);

            editor.putString("AllRoutes",allRoutes.toString());
            editor.apply();

            updateRoutes(routeSelect);

        }catch (Exception e){
            Log.d("json", "SaveAsJSON: "+e.getMessage());
        }

    }

    private void editRoute(final int position){
        LayoutInflater li = LayoutInflater.from(MarkerListActivity.this);
        View promptsView = li.inflate(R.layout.marker_title_popup, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                MarkerListActivity.this);

        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView
                .findViewById(R.id.userInput);

        final TextView textView = (TextView)promptsView.findViewById(R.id.textView1);

        textView.setText("Konum için YENİ başlık girin");

        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("Tamam",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                // get user input and set it to result
                                // edit text
                                try{
                                    titles.get(position).put("title",userInput.getText().toString());
                                    adapter.notifyDataSetChanged();
                                    SaveAsJSON(titles,selectedRoute);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        });

        alertDialogBuilder.setCancelable(false);

        AlertDialog alertDialog = alertDialogBuilder.create();

        alertDialog.show();
    }

}
