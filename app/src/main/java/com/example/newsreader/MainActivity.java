package com.example.newsreader;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();

    ArrayList<String> content = new ArrayList<>();

    ArrayAdapter<String> arrayAdapter;

    SQLiteDatabase articlesDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView =  findViewById(R.id.listView);

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles);

        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(getApplicationContext() , ArticleActivity.class);
                intent.putExtra("content",content.get(position));

                startActivity(intent);

            }
        });

        articlesDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);

        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY,articleId INTEGER,title VARCHAR,content VARCHAR)");

        updateListView();

        DownloadTask task = new DownloadTask();
        try{


           task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch(Exception e){

            e.printStackTrace();
        }
    }

    public void updateListView(){                                                   //get data from DB and display title to user
        Cursor c = articlesDB.rawQuery("SELECT * FROM articles", null);


            int contentIndex = c.getColumnIndex("content");
            int titleIndex = c.getColumnIndex("title");

            if (c.moveToFirst()) {

                //if database has any value

                titles.clear();
                content.clear();

                do {

                    titles.add(c.getString(titleIndex));
                    content.add(c.getString(contentIndex));
                } while (c.moveToNext());

                arrayAdapter.notifyDataSetChanged();

            }
            c.close();
    }

    @SuppressLint("StaticFieldLeak")
    public class DownloadTask extends AsyncTask<String, Void, String> {

        String result = "";
        HttpURLConnection urlConnection = null;
        URL url;

        @Override
        protected String doInBackground(String... urls) {

            try {
                url = new URL(urls[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = urlConnection.getInputStream();

                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();

                while (data != -1) {

                    char current = (char) data;

                    result += current;

                    data = reader.read();
                }


                JSONArray jsonArray = new JSONArray(result);

                int numberOfItems = 20;

                if(jsonArray.length() <20){

                    numberOfItems = jsonArray.length();
                }


                articlesDB.execSQL("DELETE FROM articles");

                for(int i =0; i<numberOfItems;i++){
                   String articleId = jsonArray.getString(i);

                   //getting details of current article
                   url = new URL("https://hacker-news.firebaseio.com/v0/item/" +articleId + ".json?print=pretty");

                  HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                   in =  urlConnection.getInputStream();

                   reader = new InputStreamReader(in);

                   data = reader.read();

                   StringBuilder articleInfo = new StringBuilder();

                   while(data !=-1){

                       char current = (char)data;

                       articleInfo.append(current);

                       data = reader.read();
                   }


                    JSONObject jsonObject = new JSONObject(articleInfo.toString());

                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")){

                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        url = new URL(articleUrl);

                        urlConnection = (HttpURLConnection) url.openConnection();

                        in =  urlConnection.getInputStream();

                        reader = new InputStreamReader(in);

                        data = reader.read();

                        String articleContent = "";

                        while(data !=-1){

                            char current = (char)data;

                            articleContent = articleContent + current;

                            data = reader.read();
                        }

                       Log.i("articleContent",articleContent);

                        String sql = "INSERT INTO  articles (articleId,title,content)VALUES( ? , ? , ? )";

                        SQLiteStatement statement = articlesDB.compileStatement(sql);

                        statement.bindString(1,articleId);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3,articleContent);

                        statement.execute();

                    }

                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }
}
