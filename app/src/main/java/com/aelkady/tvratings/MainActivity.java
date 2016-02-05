package com.aelkady.tvratings;

import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.db.chart.Tools;
import com.db.chart.listener.OnEntryClickListener;
import com.db.chart.model.ChartSet;
import com.db.chart.model.LineSet;
import com.db.chart.model.Point;
import com.db.chart.view.AxisController;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog pDialog;
    private float minimumRating = 11f;
    private LineSet imdbRatingSet;
    private ArrayList<ChartSet> graphData;
    private String title;
    private Boolean home = true;
    private ArrayList<android.text.Spanned> episodeData;
    private int lastDataIndex = -1;
    private String showInfo;
    private LineChartView lineChartView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            boolean test = !((boolean)savedInstanceState.getSerializable("home"));
            home = test;
            if(test) {
                ArrayList<ChartSet> graphDataInput = (ArrayList<ChartSet>)
                        savedInstanceState.getSerializable("graphData");
                String titleInput = (String) savedInstanceState.getSerializable("title");
                float minimumRatingInput = (float)
                        savedInstanceState.getSerializable("minimumRating");
                ArrayList<android.text.Spanned> episodeDataInput = (ArrayList<android.text.Spanned>)
                        savedInstanceState.getSerializable("episodeData");
                int lastDataIndexInput = savedInstanceState.getInt("lastDataIndex");
                String showInfoInput = (String) savedInstanceState.getSerializable("showInfo");

                graphData = graphDataInput;
                title = titleInput;
                minimumRating = minimumRatingInput;
                episodeData = episodeDataInput;
                lastDataIndex = lastDataIndexInput;
                showInfo = showInfoInput;

                openChart(false);
            }
            else {
                setContentView(R.layout.activity_main);
                home = true;

                final EditText search = (EditText) findViewById(R.id.search);
                search.setHintTextColor(Color.WHITE);
                // Getting reference to the button btn_chart
                Button btnChart = (Button) findViewById(R.id.btn_chart);

                // Setting event click listener for the button btn_chart of the MainActivity layout
                btnChart.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        String text = search.getText().toString();
                        TextView notFound = (TextView) findViewById(R.id.notFound);
                        if (Pattern.matches("\\s*",text)) {
                            notFound.setVisibility(View.VISIBLE);
                            notFound.setText("Please type something");
                        }
                        else {
                            notFound.setVisibility(View.INVISIBLE);
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                            new GetEpisodes().execute(text);
                        }
                    }
                });
            }
        }
        else {
            setContentView(R.layout.activity_main);
            home = true;

            final EditText search = (EditText) findViewById(R.id.search);
            search.setHintTextColor(Color.WHITE);
            // Getting reference to the button btn_chart
            Button btnChart = (Button) findViewById(R.id.btn_chart);

            // Setting event click listener for the button btn_chart of the MainActivity layout
            btnChart.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    String text = search.getText().toString();
                    TextView notFound = (TextView) findViewById(R.id.notFound);
                    if (Pattern.matches("\\s*",text)) {
                        notFound.setVisibility(View.VISIBLE);
                        notFound.setText("Please type something");
                    }
                    else {
                        notFound.setVisibility(View.INVISIBLE);
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                        new GetEpisodes().execute(text);
                    }

                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Make sure to call the super method so that the states of our views are saved
        super.onSaveInstanceState(outState);
        // Save our own state now
        outState.putSerializable("graphData", graphData);
        outState.putSerializable("title", title);
        outState.putSerializable("minimumRating", minimumRating);
        outState.putSerializable("home", home);
        outState.putSerializable("episodeData", episodeData);
        outState.putInt("lastDataIndex", lastDataIndex);
        outState.putSerializable("showInfo", showInfo);
    }

    private void openChart(boolean fromOriginalScreen) {
        if(graphData == null) {
            ScrollView main = (ScrollView) findViewById(R.id.ScrollView01);
            main.setVisibility(View.GONE);
        }
        setContentView(R.layout.activity_graph);
        TextView episodeInfo = (TextView) findViewById(R.id.episodeInfo);

        home = false;

        lineChartView = (LineChartView) findViewById(R.id.linechart);
        if(fromOriginalScreen) {
            lineChartView.addData(imdbRatingSet);
        }
        else {
            lineChartView.addData(graphData);
        }
        TextView graphTitle = (TextView) findViewById(R.id.graphTitle);
        graphTitle.setText(title);

        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#7F97B867"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setAntiAlias(true);
        gridPaint.setStrokeWidth(Tools.fromDpToPx(.75f));

        lineChartView.setTopSpacing(Tools.fromDpToPx(15))
                .setBorderSpacing(Tools.fromDpToPx(0))
                .setXLabels(AxisController.LabelPosition.OUTSIDE)
                .setYLabels(AxisController.LabelPosition.OUTSIDE)
                .setLabelsColor(Color.parseColor("#e08b36"))
                .setAxisBorderValues((int) Math.floor((double) minimumRating), 10, 1)
                .setXAxis(true)
                .setYAxis(true)
                .setGrid(ChartView.GridType.HORIZONTAL, gridPaint);



        lineChartView.setOnEntryClickListener(new OnEntryClickListener() {
            @Override
            public void onClick(int setIndex, int entryIndex, Rect rect) {
                lastDataIndex = entryIndex;
                TextView episodeInfo = (TextView) findViewById(R.id.episodeInfo);
                episodeInfo.setVisibility(View.GONE);
                episodeInfo.setText(episodeData.get(entryIndex));
                episodeInfo.setVisibility(View.VISIBLE);
//                ScrollView scroll = (ScrollView) findViewById(R.id.ScrollView01);
//                scroll.fullScroll(View.FOCUS_DOWN);
            }
        });

        lineChartView.show();
        graphData = lineChartView.getData();
        if(lastDataIndex == -1) {
            episodeInfo.setVisibility(View.GONE);
        }
        else {
            episodeInfo.setVisibility(View.GONE);
            episodeInfo.setText(episodeData.get(lastDataIndex));
            episodeInfo.setVisibility(View.VISIBLE);
//            ScrollView scroll = (ScrollView) findViewById(R.id.ScrollView01);
//            scroll.fullScroll(View.FOCUS_DOWN);

        }
        TextView showInfoView = (TextView) findViewById(R.id.showInfo);
        if(showInfo == null) {
            showInfoView.setVisibility(View.GONE);
        }
        else {
            showInfoView.setVisibility(View.GONE);
            showInfoView.setText(Html.fromHtml(showInfo));
            showInfoView.setVisibility(View.VISIBLE);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            ScrollView main = (ScrollView) findViewById(R.id.ScrollView01);
            main.setVisibility(View.GONE);
            setContentView(R.layout.activity_main);
            home = true;
            imdbRatingSet = null;
            title = null;
            episodeData = null;
            graphData = null;
            minimumRating = 100f;
            final EditText search = (EditText) findViewById(R.id.search);
            search.setHintTextColor(Color.WHITE);
            Button button = (Button) findViewById(R.id.btn_chart);
            button.invalidate();
            button.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // Draw the Income vs Expense Chart
                    String text = search.getText().toString();
                    TextView notFound = (TextView) findViewById(R.id.notFound);
                    if(Pattern.matches("\\s*",text)) {
                        notFound.setVisibility(View.VISIBLE);
                        notFound.setText("Please type something");
                    }
                    else {
                        notFound.setVisibility(View.INVISIBLE);
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                        new GetEpisodes().execute(text);
                    }
                }
            });
            return true;
        }

        return super.onKeyDown(keyCode, event);


    }

    private class GetEpisodes extends AsyncTask<String, String, String> {

        private String urlGeneral = "http://www.myapifilms.com/imdb/idIMDB";
        private String urlSpecific = "http://www.omdbapi.com/";
        private String[] colors = {"red","blue","green","black","gray",
                "cyan","magenta","yellow","lightgray","darkgray","aqua",
                "fuchsia","lime","maroon","navy","olive","purple","silver","teal"};

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Fetching...");
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected String doInBackground(String... args) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();

//            EditText editText = (EditText) findViewById(R.id.editText);
            String text = args[0];
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("title",text));
            params.add(new BasicNameValuePair("format","json"));
            params.add(new BasicNameValuePair("seasons","1"));
            params.add(new BasicNameValuePair("limit","1"));
            params.add(new BasicNameValuePair("language","en-us"));
            params.add(new BasicNameValuePair("token", "0c2b58d0-a25d-46ec-b060-79912e817a60"));
            // Making a request to url and getting response
            long startTime = System.currentTimeMillis();
            String jsonStr = sh.makeServiceCall(urlGeneral, ServiceHandler.GET, params);

        if (jsonStr != null && jsonStr != "") {
            try {

                //JSONArray jsonArray = new JSONArray(jsonStr);
                JSONObject bridgeObj = new JSONObject(jsonStr);
                JSONObject jsonObj = bridgeObj.getJSONObject("data").getJSONArray("movies").getJSONObject(0);
                JSONArray seasons = jsonObj.getJSONArray("seasons");
                String showRating = jsonObj.getString("rating");
                if(showRating=="N/A") {
                    showRating = "0";
                }
                int masterLength = 0;

                imdbRatingSet = new LineSet();
                episodeData = new ArrayList<android.text.Spanned>();
                title = jsonObj.getString("title");
                final int arbitraryFloor = (int)Math.floor(seasons.length() * 1.5);
                for (int i = 0; i < seasons.length(); i++) {
                    JSONObject season = seasons.getJSONObject(i);
                    JSONArray episodes = season.getJSONArray("episodes");
                    for(int j = 0; j < episodes.length(); j++) {
                        JSONObject episode = episodes.getJSONObject(j);
                        String episodeId = episode.getString("idIMDB");
                        List<NameValuePair> paramsNew = new ArrayList<NameValuePair>();
                        paramsNew.add(new BasicNameValuePair("i", episodeId));
                        String episodeStr = sh.makeServiceCall(urlSpecific, ServiceHandler.GET,
                                paramsNew);
                        JSONObject episodeObj = new JSONObject(episodeStr);
                        String response = episodeObj.getString("Response");
                        if(response.equals("False")) {
                            continue;
                        }
                        String episodeReleased = episodeObj.getString("Released");

                        if(episodeReleased.equals("N/A")) {
                            continue;
                        }
                        int episodeYear = Integer.parseInt(episodeObj.getString("Year"));
                        int year = Calendar.getInstance().get(Calendar.YEAR);
                        if(episodeYear > year) {
                            continue;
                        }
                        SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy");
                        Date convertedDate = format.parse(episodeReleased);
                        Date currentDate = new Date();
                        int difference = convertedDate.compareTo(currentDate);
                        if(difference >= 0) {
                            continue;
                        }
                        masterLength++;
                        String rating = episodeObj.getString("imdbRating");
                        if(!rating.equals("N/A")) {
                            if(masterLength%arbitraryFloor==0) {
                                Point point = new Point(Integer.toString(masterLength),
                                        Float.parseFloat(rating));
                                point.setColor(Color.parseColor(colors[i % 19]));
                                point.setStrokeColor(Color.parseColor(colors[i % 19]));
                                imdbRatingSet.addPoint(point);
                            } else {
                                Point point = new Point("",Float.parseFloat(rating));
                                point.setColor(Color.parseColor(colors[i % 19]));
                                point.setStrokeColor(Color.parseColor(colors[i % 19]));
                                imdbRatingSet.addPoint(point);
                            }
                            if(Float.parseFloat(rating) < minimumRating) {
                                minimumRating = Float.parseFloat(rating);
                            }
                        }
                        else {
                            if(masterLength%arbitraryFloor==0) {
                                Point point = new Point(Integer.toString(masterLength),
                                        Float.parseFloat(rating));
                                point.setColor(Color.parseColor(colors[i % 19]));
                                point.setStrokeColor(Color.parseColor(colors[i % 19]));
                                imdbRatingSet.addPoint(point);
                            }
                            else {
                                Point point = new Point("",Float.parseFloat(rating));
                                point.setColor(Color.parseColor(colors[i % 19]));
                                point.setStrokeColor(Color.parseColor(colors[i % 19]));
                                imdbRatingSet.addPoint(point);
                            }
                            if(Float.parseFloat(rating) < minimumRating) {
                                minimumRating = Float.parseFloat(rating);
                            }
                        }
                        String seasonNumber = episodeObj.getString("Season");
                        String episodeNumber = episodeObj.getString("Episode");
                        String episodeTitle = episodeObj.getString("Title");
                        String votes = episodeObj.getString("imdbVotes");

                        String toBeDisplayed = "<b>Season:</b> " + seasonNumber + "<br />" +
                                "<b>Episode:</b> " + episodeNumber + "<br />" +
                                "<b>Title:</b> " + episodeTitle + "<br />" +
                                "<b>Rating:</b> " + rating + "<br />" +
                                "<b>Votes:</b> " + votes;
                        episodeData.add(Html.fromHtml(toBeDisplayed));
                    }

                }
                showInfo = "<b>" + title + "</b><br />" +
                        "<b>"+showRating+"/10</b> from <b>" + jsonObj.getString("votes") +
                        "</b> votes.";

                imdbRatingSet.setSmooth(false)
                .setThickness(0.001f)
                .setDotsRadius(7f);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        openChart(true);
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);

                    }
                });

            } catch (JSONException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        TextView notFound = (TextView) findViewById(R.id.notFound);
                        notFound.setText("Not Found");
                        notFound.setVisibility(View.VISIBLE);

                    }
                });
            }
            catch (ParseException e)
            {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        TextView notFound = (TextView) findViewById(R.id.notFound);
                        notFound.setText("Error");
                        notFound.setVisibility(View.VISIBLE);

                    }
                });
            }
            catch(Exception e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        TextView notFound = (TextView) findViewById(R.id.notFound);
                        notFound.setText("Error");
                        notFound.setVisibility(View.VISIBLE);

                    }
                });
            }
        } else {
            Log.e("ServiceHandler", "Couldn't get any data from the url");
        }
            long difference = System.currentTimeMillis() - startTime;
            Log.d("Time Taken", ">" + difference);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();

        }

    }

}




