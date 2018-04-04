package pulkit.demo_video_chat.com.demovidechatwebrtc;

import android.Manifest;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;

import android.content.pm.PackageInstaller;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements
        Session.SessionListener
        , PublisherKit.PublisherListener
        , SubscriberKit.SubscriberListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static String API_KEY = null;
    private static String SESSION_ID = null;
    private static String TOKEN = null;

    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;

    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    private FrameLayout publisher_container;
    private FrameLayout subscriber_container;
    private Switch sw_audio_on_off, sw_video_on_off;

    public RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        queue = Volley.newRequestQueue(getApplicationContext());

        findIds();
        init();
    }

    private void findIds() {

        publisher_container = (FrameLayout) findViewById(R.id.publisher_container);
        subscriber_container = (FrameLayout) findViewById(R.id.subscriber_container);
        sw_audio_on_off = (Switch) findViewById(R.id.sw_audio_on_off);
        sw_video_on_off = (Switch) findViewById(R.id.sw_video_on_off);
    }

    private void init() {

        /*initialize audio and video off*/
        sw_audio_on_off.setChecked(true);
        sw_video_on_off.setChecked(true);

        requestPermissions();

        setListners();
    }

    private void setListners() {

        publisher_container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                /*Swap the camera*/
                mPublisher.cycleCamera();

            }
        });

        sw_video_on_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                /*You can toggle video on or off, by calling the setPublishAudio()*/
                if (sw_video_on_off.isChecked()) {
                    mPublisher.setPublishVideo(true);

                } else {
                    mPublisher.setPublishVideo(false);
                }
            }
        });

        sw_audio_on_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                /*You can toggle audio on or off, by calling the setPublishAudio()*/
                if (sw_audio_on_off.isChecked()) {
                    mPublisher.setPublishAudio(true);

                } else {
                    mPublisher.setPublishAudio(false);
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {

            // initialize view objects from your layout
            /*This is for hardcoded*/
            /*mSession = new Session.Builder(this, API_KEY, SESSION_ID).build();
            mSession.setSessionListener(this);*/

            // initialize and connect to the session
            /*mSession.connect(TOKEN);*/

            /*Through server automatically*/
            fetchSessionConnectionData();

        } else {
            EasyPermissions.requestPermissions(this, "This app needs access to your camera and mic to make video calls", RC_VIDEO_APP_PERM, perms);
        }
    }

    /*when we setSessionListener(this) implement these five methods, 1) onConnected, 2) onDisconnected, 3) onStreamReceived,
    4) onStreamDropped, 5) onError*/
    /*When the client connects to the OpenTok session,
    the implementation of the SessionListener.onConnected(session) method is called.*/

    /*Modify the implementation of the SessionListener.onConnected() method to include code to publish a stream to the session:*/
    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG, "Session Connected");
        //Toast.makeText(this, "onConnected", //Toast.LENGTH_SHORT).show();

        mPublisher = new Publisher.Builder(this)
                .name("Pulkit's video")                                     // To set the name
                .videoTrack(true)                                           // To set up a voice-only session, call the videoTrack()
                .audioTrack(true)                                           // To set the audio track
                .resolution(Publisher.CameraCaptureResolution.HIGH)         // Setting the resolution for a video
                .frameRate(Publisher.CameraCaptureFrameRate.FPS_30)         // frame rate for a video
                .build();

        /*If you call the startPreview() method,
        you must call the destroy() method of the Publisher to remove the Publisher's view (and the video),
        when the publisher stops streaming (when the onStreamDestroyed(PublisherKit publisher,
        Stream stream) method of the PublisherListener is called).*/
        mPublisher.startPreview();

        /*The code above uses Publisher.Builder() to instantiate a Publisher object.*/
        mPublisher.setPublisherListener(this);
        /*The constructor takes one parameter: the Android application context associated with this process.*/

        publisher_container.addView(mPublisher.getView());
        /*In this method, Add view to the container*/
        mSession.publish(mPublisher);
        /*This method publishes an audio-video stream to the OpenTok session, using the camera and microphone of the Android device.*/

    }

    /*When the client disconnects from the OpenTok session,
    the implementation of the SessionListener.onDisconnected(session) method is called.*/
    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG, "Session Disconnected");
        //Toast.makeText(this, "onDisconnected", Toast.LENGTH_SHORT).show();

    }

    /*When another client publishes a stream to the OpenTok session,
    the implementation of the SessionListener.onStreamReceived(session, stream) method is called.*/
    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Received");
        //Toast.makeText(this, "onStreamReceived", Toast.LENGTH_SHORT).show();

        if (mSubscriber == null) {
            mSubscriber = new Subscriber.Builder(this, stream).build();
            /*The Subscriber.Builder() constructor takes two parameters:
                1) The Android application context associated with this process.
                2) The Stream object (for the stream you want to view)*/
            mSession.subscribe(mSubscriber);
            /*The Session.subscribe(subscriber) method subscribes to the stream that was just received.*/
            subscriber_container.addView(mSubscriber.getView());
            /*subscriber_container.addView(mSubscriber.getView()) places the new subscribed stream's view on the screen.*/
        }
    }

    /*When another client stops publishing a stream to the OpenTok session,
    the implementation of the SessionListener.onStreamDropped(session, stream) method is called.*/
    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Dropped");
        //Toast.makeText(this, "onStreamDropped", Toast.LENGTH_SHORT).show();

        if (mSubscriber != null) {
            mSubscriber = null;
            subscriber_container.removeAllViews();
        }
    }

    /*If the client fails to connect to the OpenTok session,
    the implementation of the SessionListener.onError(session, error) method is called.*/
    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.e(LOG_TAG, "Session error: " + opentokError.getMessage());
        //Toast.makeText(this, "Session onError", Toast.LENGTH_SHORT).show();

    }

    /*TODO: when we setPublisherListener(this) implement these three methods, 1) onStreamCreated, 2) onStreamDestroyed, 3) onError*/

    /*Called when the publisher starts streaming to the OpenTok session.*/
    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.i(LOG_TAG, "Publisher onStreamCreated");
        //Toast.makeText(this, "onStreamCreated", Toast.LENGTH_SHORT).show();

    }

    /*Called when the publisher stops streaming to the OpenTok session.*/
    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.i(LOG_TAG, "Publisher onStreamDestroyed");
        //Toast.makeText(this, "onStreamDestroyed", Toast.LENGTH_SHORT).show();

        mPublisher.destroy();

    }

    /*Called when the client fails in publishing to the OpenTok session. An OpentokError object is passed into the method.*/
    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        Log.e(LOG_TAG, "Publisher error: " + opentokError.getMessage());
        //Toast.makeText(this, "Publisher onError", Toast.LENGTH_SHORT).show();

    }

    /*TODO: when we setPublisherListener(this) implement these three methods, 1) onStreamCreated, 2) onStreamDestroyed, 3) onError*/
    @Override
    public void onConnected(SubscriberKit subscriberKit) {
        Log.i(LOG_TAG, "Subscriber onConnected");

        mSubscriber = new Subscriber.Builder(this, subscriberKit.getStream()).build();
        /*The code above uses Publisher.Builder() to instantiate a Publisher object.*/
        mSubscriber.setSubscriberListener(this);
        /*The constructor takes one parameter: the Android application context associated with this process.*/

        subscriber_container.addView(mSubscriber.getView());
        /*In this method, Add view to the container*/
        /*This method publishes an audio-video stream to the OpenTok session, using the camera and microphone of the Android device.*/

    }

    /*When the stream is dropped and the client tries to reconnect,
    the SubscriberKit.StreamListener.onDisconnected(SubscriberKit subscriber) method is called.*/
    @Override
    public void onDisconnected(SubscriberKit subscriberKit) {
        Log.i(LOG_TAG, "Subscriber onDisconnected");
        /*To stop playing a stream you are subscribed to, call the Session.unsubscribe(Subscriber subscriber) method:*/
        mSession.unsubscribe(mSubscriber);
    }

    @Override
    public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {
        Log.e(LOG_TAG, "Publisher error: " + opentokError.getMessage());
    }

    public void fetchSessionConnectionData() {
        RequestQueue reqQueue = Volley.newRequestQueue(this);
        reqQueue.add(new JsonObjectRequest(Request.Method.GET, "https://videottestchat.herokuapp.com" + "/session",
                null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                try {
                    API_KEY = response.getString("apiKey");
                    SESSION_ID = response.getString("sessionId");
                    TOKEN = response.getString("token");

                    Log.i(LOG_TAG, "API_KEY: " + API_KEY);
                    Log.i(LOG_TAG, "SESSION_ID: " + SESSION_ID);
                    Log.i(LOG_TAG, "TOKEN: " + TOKEN);

                    mSession = new Session.Builder(MainActivity.this, API_KEY, SESSION_ID).build();
                    mSession.setSessionListener(MainActivity.this);
                    mSession.connect(TOKEN);

                } catch (JSONException error) {
                    Log.e(LOG_TAG, "Web Service error: " + error.getMessage());
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(LOG_TAG, "Web Service error: " + error.getMessage());
            }
        }));

    }

}
