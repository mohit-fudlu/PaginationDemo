package com.test.paginationdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.JsonObject;
import com.test.paginationdemo.Utils.Api;
import com.test.paginationdemo.Utils.PaginationAdapterCallback;
import com.test.paginationdemo.Utils.PaginationScrollListener;
import com.test.paginationdemo.models.Paginatedemo;
import com.test.paginationdemo.volley.MySingleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;



public class MainActivity extends AppCompatActivity implements PaginationAdapterCallback {

    private static final String TAG = "MainActivity";

    PaginationAdapter adapter;
    LinearLayoutManager linearLayoutManager;

    RecyclerView rv;
    ProgressBar progressBar;
    LinearLayout errorLayout;
    Button btnRetry;
    TextView txtError;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private  int TOTAL_PAGES = 0;
    private int currentPage = 1;
    TextView txt_page_count;
    TextView txt_record_count;
    LinearLayout linear_bottom;
TextView txt_no_record;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txt_no_record=findViewById(R.id.txt_no_record);
        rv = findViewById(R.id.main_recycler);
        progressBar = findViewById(R.id.main_progress);
        errorLayout = findViewById(R.id.error_layout);
        btnRetry = findViewById(R.id.error_btn_retry);
        txtError = findViewById(R.id.error_txt_cause);
        txt_page_count=findViewById(R.id.txt_page_count);
        txt_record_count=findViewById(R.id.txt_records_count);
        linear_bottom=findViewById(R.id.linear_bottom);


        adapter = new PaginationAdapter(this);

        linearLayoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(linearLayoutManager);
        rv.setItemAnimator(new DefaultItemAnimator());

        rv.setAdapter(adapter);

        rv.addOnScrollListener(new PaginationScrollListener(linearLayoutManager) {
            @Override
            protected void loadMoreItems() {
                isLoading = true;
                currentPage += 1;
if(isNetworkConnected()) {
    loadNextPage(currentPage);
}
            }

            @Override
            public int getTotalPageCount() {
                Toast.makeText(MainActivity.this, TOTAL_PAGES+"", Toast.LENGTH_SHORT).show();
                return TOTAL_PAGES;
            }

            @Override
            public boolean isLastPage() {
                return isLastPage;
            }

            @Override
            public boolean isLoading() {
                return isLoading;
            }
        });


if(isNetworkConnected()) {
    loadFirstPage();
}





    }







    private void loadFirstPage() {
        String url = Api.BASE_Url;
        Log.i("url",url);
        progressBar.setVisibility(View.VISIBLE);
        linear_bottom.setVisibility(View.GONE);
        StringRequest stringRequest=new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    txt_no_record.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    linear_bottom.setVisibility(View.VISIBLE);
                    JSONObject jsonObject=new JSONObject(response);
                    JSONArray items=jsonObject.getJSONArray("items");
                    List<Paginatedemo> paginatedemos=new ArrayList<>();
                        if(items!=null && items.length()>0){
                            for(int i=0; i<items.length();i++){
                                JSONObject result=items.getJSONObject(i);
                                Paginatedemo paginatedemo=new Paginatedemo();
                                paginatedemo.id=result.optString("id");
                                paginatedemo.pincode=result.optString("pincode");
                                paginatedemo.district=result.optString("district");
                                paginatedemo.state=result.optString("state");
                                paginatedemo.created=result.optString("created");
                                paginatedemo._updated =result.optString("_updated");
                                paginatedemos.add(paginatedemo);

                            }

                        }
                        if(paginatedemos.size()>0){

                            adapter.addAll(paginatedemos);
                        }
                        JSONObject links=jsonObject.getJSONObject("links");
                    JSONObject meta=jsonObject.getJSONObject("meta");
                    currentPage=meta.getInt("page");
                    txt_page_count.setText("Current page : "+currentPage);
                    txt_record_count.setText("Current records : "+paginatedemos.size());
                    int max_results=meta.getInt("max_results");
                    int total=meta.getInt("total");
                        JSONObject last=links.getJSONObject("last");
                        JSONObject next=links.getJSONObject("next");
                        JSONObject self=links.getJSONObject("self");
                        String nexthref=next.getString("href");
                        String lasthref=last.getString("href");
                        String next_url;
//                        if(nexthref.contains("?")){
//                            next_url=nexthref.substring(nexthref.lastIndexOf("?"),nexthref.length()-1);
//                            adapter.setNext_page_url(next_url);
//                        }

                        if(lasthref.contains("=")){
                           String last_page_url=lasthref.substring(lasthref.lastIndexOf("=")+1);
                           TOTAL_PAGES= Integer.parseInt(last_page_url);
                            TOTAL_PAGES=TOTAL_PAGES+1;
                        }
                    if (currentPage <= TOTAL_PAGES) adapter.addLoadingFooter();
                else isLastPage = true;

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.GONE);
                linear_bottom.setVisibility(View.GONE);
                txt_no_record.setVisibility(View.VISIBLE);
                try {

                    if(error.networkResponse!=null) {
                        if (error.networkResponse.data != null) {

                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            JSONObject exception = new JSONObject(responseBody);
                            Toast.makeText(MainActivity.this, exception.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    }

                } catch ( JSONException e ) {
                    e.printStackTrace();
//Handle a malformed json response
                } catch (UnsupportedEncodingException e){
                    e.printStackTrace();
                }
            }
        });
        MySingleton.getInstance(this).addToRequestQueue(stringRequest);
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
//        Log.d(TAG, "loadFirstPage: ");
//
//        // To ensure list is visible when retry button in error view is clicked
//        hideErrorView();
//        currentPage = PAGE_START;
//
//        callTopRatedMoviesApi().enqueue(new Callback<TopRatedMovies>() {
//            @Override
//            public void onResponse(Call<TopRatedMovies> call, Response<TopRatedMovies> response) {
//                hideErrorView();
//
////                Log.i(TAG, "onResponse: " + (response.raw().cacheResponse() != null ? "Cache" : "Network"));
//
//                // Got data. Send it to adapter
//                List<Result> results = fetchResults(response);
//                progressBar.setVisibility(View.GONE);
//                adapter.addAll(results);
//
//                if (currentPage <= TOTAL_PAGES) adapter.addLoadingFooter();
//                else isLastPage = true;
//            }
//
//            @Override
//            public void onFailure(Call<TopRatedMovies> call, Throwable t) {
//                t.printStackTrace();
//                showErrorView(t);
//            }
//        });
    }




    private void loadNextPage(int page_number) {
        String url = Api.BASE_Url+"?page="+page_number;
        Log.i("url",url);
        StringRequest stringRequest=new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject=new JSONObject(response);
                    JSONArray items=jsonObject.getJSONArray("items");
                    List<Paginatedemo> paginatedemos=new ArrayList<>();
                    if(items!=null && items.length()>0){
                        for(int i=0; i<40;i++){
                            JSONObject result=items.getJSONObject(i);
                            Paginatedemo paginatedemo=new Paginatedemo();
                            paginatedemo.id=result.optString("id");
                            paginatedemo.pincode=result.optString("pincode");
                            paginatedemo.district=result.optString("district");
                            paginatedemo.state=result.optString("state");
                            paginatedemo.created=result.optString("created");
                            paginatedemo._updated =result.optString("_updated");
                            paginatedemos.add(paginatedemo);

                        }

                    }
                    if(paginatedemos.size()>0){
                        adapter.addAll(paginatedemos);
                        rv.scrollToPosition(paginatedemos.size() - 1);
                    }
                    adapter.removeLoadingFooter();
                isLoading = false;
                    JSONObject links=jsonObject.getJSONObject("links");

                    JSONObject meta=jsonObject.getJSONObject("meta");
                    currentPage=meta.getInt("page");
                    int max_results=meta.getInt("max_results");
                    int total=meta.getInt("total");
                    txt_page_count.setText("Current page : "+currentPage);
                    txt_record_count.setText("Current records : "+paginatedemos.size());
                    JSONObject last=links.getJSONObject("last");
                    JSONObject next=links.getJSONObject("next");
                    JSONObject self=links.getJSONObject("self");
                    String nexthref=next.getString("href");
                    String lasthref=last.getString("href");
                    String next_url;
//                    if(nexthref.contains("?")){
//                        next_url=nexthref.substring(nexthref.lastIndexOf("?"),nexthref.length()-1);
//                        adapter.setNext_page_url(next_url);
//                    }

                    if(lasthref.contains("=")){
                        String last_page_url=lasthref.substring(lasthref.lastIndexOf("=")+1);
                        TOTAL_PAGES= Integer.parseInt(last_page_url);
                        TOTAL_PAGES=TOTAL_PAGES+1;
                    }

                    if (currentPage <= TOTAL_PAGES) adapter.addLoadingFooter();
                    else isLastPage = true;

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                try {

                    if(error.networkResponse!=null) {
                        if (error.networkResponse.data != null) {

                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            JSONObject exception = new JSONObject(responseBody);
                            Toast.makeText(MainActivity.this, exception.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    }

                } catch ( JSONException e ) {
                    e.printStackTrace();
//Handle a malformed json response
                } catch (UnsupportedEncodingException e){
                    e.printStackTrace();
                }
            }
        });
        MySingleton.getInstance(this).addToRequestQueue(stringRequest);
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
//
    }






    @Override
    public void retryPageLoad() {
        if(isNetworkConnected()) {
            loadNextPage(currentPage + 1);
        }
    }





    /**
     * Remember to add android.permission.ACCESS_NETWORK_STATE permission.
     *
     * @return
     */
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }
}
