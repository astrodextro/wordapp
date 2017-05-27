package com.felixunlimited.word.app.sync;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class JSONParser {
    static InputStream is = null;
    static OutputStream os = null;
    static JSONObject jObj = null;
    static String json = "";

    // constructor
    public JSONParser() {
    }

    // function get json from url
    // by making HTTP POST or GET method
    public JSONObject makeHttpRequest(String url, String method,
                                      String query, String table) {

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;
        URL murl = null;

        try {
            murl = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        // Making HTTP request
        try {
            OutputStream out;
            InputStream in;

            // check for request method
            if(method == "POST"){
                // request method is POST
                // defaultHttpClient
                if (murl != null) {
                    urlConnection = (HttpURLConnection) murl.openConnection();
                    try {
                        // Create data variable for sent values to server

                        String data = URLEncoder.encode("table", "UTF-8")
                                + "=" + URLEncoder.encode(table, "UTF-8")
                                + "&" + URLEncoder.encode("query", "UTF-8")
                                + "=" + URLEncoder.encode(query, "UTF-8");


                        urlConnection.setDoOutput(true);
                        OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
                        wr.write( data );
                        wr.flush();

                        // Get the server response

                        reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line = null;

                        // Read Server Response
                        while((line = reader.readLine()) != null)
                        {
                            // Append server response in string
                            sb.append(line + "\n");
                        }

                        json = sb.toString();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            else if(method == "GET"){

                try {
                    // Construct the URL for the OpenMessageMap query
                    // Possible parameters are avaiable at OWM's message API page, at
                    // http://openmessagemap.org/API#message

                    URL mUrl = new URL(url);

                    // Create the request to OpenMessageMap, and open the connection
                    urlConnection = (HttpURLConnection) mUrl.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // Read the input stream into a String
                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
                        return null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                        // But it does make debugging a *lot* easier if you print out the completed
                        // buffer for debugging.
                        buffer.append(line + "\n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No point in parsing.
                        return null;
                    }
                    json = buffer.toString();
                } catch (IOException e) {
                    Log.e("LOG_TAG", "Error ", e);
                    // If the code didn't successfully get the message data, there's no point in attempting
                    // to parse it.
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (final IOException e) {
                            Log.e("LOG_TAG", "Error closing stream", e);
                        }
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        // try parse the string to a JSON object
        try {
            jObj = new JSONObject(json);
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data " + e.toString());
        }

        // return JSON String
        return jObj;
    }
}