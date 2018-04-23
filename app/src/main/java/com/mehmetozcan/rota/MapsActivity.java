package com.mehmetozcan.rota;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.support.v7.widget.Toolbar;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.mehmetozcan.rota.Utils.DataParser;
import com.mehmetozcan.rota.Utils.LocationService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Location mLocation = null;
    private boolean onCreateRoute = false;
    private Toolbar toolbar;
    private Button createRoute;
    private ArrayList<LatLng> markerPoints= new ArrayList();
    private ArrayList<String> markerTitles = new ArrayList();
    private EditText temp ;
    private Spinner routeSelect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        toolbar = (Toolbar) findViewById(R.id.tool_bar);
        toolbar.setTitle(R.string.app_name);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);

        temp = (EditText)findViewById(R.id.tempText);

        routeSelect = (Spinner) findViewById(R.id.rota_select);

        final SharedPreferences prefs = getSharedPreferences("routes",MODE_PRIVATE);

        updateRoutes(routeSelect);

        routeSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mMap.clear();
                markerPoints.clear();
                markerTitles.clear();

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
                            LatLng point = new LatLng(Double.valueOf(latlong.getString("Lat")),Double.valueOf(latlong.getString("Lng")));
                            points.add(point);
                            mMap.addMarker(new MarkerOptions().position(point).title(latlong.getString("title")));
                        }
                        openRoute(points);

                    }
                }catch (Exception e){
                    Log.d("json", "onCreate: "+e.getMessage());
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mMap.clear();
            }
        });

        CheckBox enableMyLoc = (CheckBox)findViewById(R.id.showMyLocation);
        enableMyLoc.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)  MapsActivityPermissionsDispatcher.EnableMyLocationWithPermissionCheck(MapsActivity.this);
                else MapsActivityPermissionsDispatcher.DisableMyLocationWithPermissionCheck(MapsActivity.this);
            }
        });


        createRoute = (Button)findViewById(R.id.create_route);
        createRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if(!onCreateRoute){
                   onCreateRoute = true;
                   createRoute.setText("Durdur");
                   findViewById(R.id.info_text).setVisibility(View.VISIBLE);
                    mMap.clear();
                    markerPoints.clear();
                    markerTitles.clear();



               }else{
                   onCreateRoute = false;
                   createRoute.setText("Yeni Rota Oluştur");
                   findViewById(R.id.info_text).setVisibility(View.GONE);

                   if(markerPoints.size() > 1) {

                       LayoutInflater li = LayoutInflater.from(MapsActivity.this);
                       View promptsView = li.inflate(R.layout.marker_title_popup, null);

                       AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                               MapsActivity.this);

                       alertDialogBuilder.setView(promptsView);

                       final EditText userInput = (EditText) promptsView
                               .findViewById(R.id.userInput);

                       final TextView textView = (TextView)promptsView.findViewById(R.id.textView1);

                       textView.setText("Rota için başlık girin");

                       alertDialogBuilder
                               .setCancelable(false)
                               .setPositiveButton("Tamam",
                                       new DialogInterface.OnClickListener() {
                                           public void onClick(DialogInterface dialog,int id) {
                                               // get user input and set it to result
                                               // edit text
                                               SaveAsJSON(markerPoints,markerTitles,userInput.getText().toString());
                                               openRoute(markerPoints);
                                               updateRoutes(routeSelect);
                                           }
                                       });

                       alertDialogBuilder.setCancelable(false);

                       AlertDialog alertDialog = alertDialogBuilder.create();

                       alertDialog.show();


                   }
               }
            }
        });

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.moveCamera(CameraUpdateFactory
                .newLatLngZoom(new LatLng(37.781986, 29.096297), 12));

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.showInfoWindow();
                return true;
            }
        });

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if(onCreateRoute){
                    final LatLng mLatLng = latLng;

                    LayoutInflater li = LayoutInflater.from(MapsActivity.this);
                    View promptsView = li.inflate(R.layout.marker_title_popup, null);

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                            MapsActivity.this);

                    alertDialogBuilder.setView(promptsView);

                    final EditText userInput = (EditText) promptsView
                            .findViewById(R.id.userInput);

                    final TextView textView = (TextView)promptsView.findViewById(R.id.textView1);

                    textView.setText("Konum için başlık girin");

                    alertDialogBuilder
                            .setCancelable(false)
                            .setPositiveButton("Tamam",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,int id) {
                                            // get user input and set it to result
                                            // edit text
                                            markerPoints.add(mLatLng);
                                            markerTitles.add(userInput.getText().toString());
                                            mMap.addMarker(new MarkerOptions().position(mLatLng).title(userInput.getText().toString()));

                                        }
                                    });

                    alertDialogBuilder.setCancelable(false);

                    AlertDialog alertDialog = alertDialogBuilder.create();

                    alertDialog.show();


                }
            }
        });

    }

    private void SaveAsJSON(ArrayList<LatLng> list,ArrayList<String> listTitles,String routeName){
        JSONObject jsonObject = new JSONObject();

        try{

            jsonObject.put("RouteName",routeName);
            JSONArray latlng = new JSONArray();
            for (int i = 0; i<list.size();i++){
                LatLng each = list.get(i);
                latlng.put(new JSONObject().put("Lat",each.latitude).put("Lng",each.longitude).put("title",listTitles.get(i)));
            }

            jsonObject.put("locations",latlng);
            Log.d("json", "SaveAsJSON: JSON: "+jsonObject.toString());

            SharedPreferences prefs = getSharedPreferences("routes",MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            JSONArray allRoutes = new JSONArray(prefs.getString("AllRoutes","[]"));
            allRoutes.put(jsonObject);

            editor.putString("AllRoutes",allRoutes.toString());
            editor.apply();

        }catch (Exception e){
            Log.d("json", "SaveAsJSON: "+e.getMessage());
        }

    }

    private void openRoute(ArrayList<LatLng> markerPoints){
        String url = getUrl(markerPoints);
        Log.d("onMapClick", url.toString());
        FetchUrl FetchUrl = new FetchUrl();

        // Start downloading json data from Google Directions API
        FetchUrl.execute(url);
        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(markerPoints.get(0)));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));
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
                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(MapsActivity.this, android.R.layout.simple_spinner_item,routeNames);
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                routeSelect.setAdapter(dataAdapter);
                routeSelect.setSelection(routeNames.size()-1);

            }
        }catch (Exception e){
            Log.d("json", "onCreate: "+e.getMessage());
        }
    }

    private String getUrl(ArrayList<LatLng> markerPoints) {

        if(markerPoints.size()>1){
            String address = "origin="+markerPoints.get(0).latitude + "," + markerPoints.get(0).longitude;
            address += "&destination="+markerPoints.get(markerPoints.size()-1).latitude + "," + markerPoints.get(markerPoints.size()-1).longitude;
            address += "&waypoints=";
            for (int i= 1 ;i <  markerPoints.size()-1 ; i++)
            {
                if(i == 1)
                    address += "via:" + markerPoints.get(i).latitude + "," + markerPoints.get(i).longitude;
                else   address += "|via:" + markerPoints.get(i).latitude + "," + markerPoints.get(i).longitude;
            }

            // Building the parameters to the web service
            String parameters = address + "&key=" + getResources().getString(R.string.apikey);

            // Output format
            String output = "json";

            // Building the url to the web service
            String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

            Log.d("route", "getUrl: "+url);
            return url;
        }else return null;

    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask",jsonData[0].toString());
                DataParser parser = new DataParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask","Executing routes");
                Log.d("ParserTask",routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask",e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.RED);

                Log.d("onPostExecute","onPostExecute lineoptions decoded");

            }

            // Drawing polyline in the Google Map for the i-th route
            if(lineOptions != null) {
                mMap.addPolyline(lineOptions);
            }
            else {
                Log.d("onPostExecute","without Polylines drawn");
            }
        }
    }


    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void EnableMyLocation(){
        mMap.setMyLocationEnabled(true);
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void DisableMyLocation(){
        mMap.setMyLocationEnabled(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MapsActivityPermissionsDispatcher.onRequestPermissionsResult(this,requestCode,grantResults);
    }
}
