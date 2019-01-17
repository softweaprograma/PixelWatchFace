package com.corvettecole.pixelwatchface;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link WatchPreview.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link WatchPreview#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WatchPreview extends androidx.fragment.app.Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_USE_24_HOUR_TIME = "param1";
    private static final String ARG_SHOW_TEMPERATURE = "param2";
    private static final String ARG_USE_CELSIUS = "param3";
    private static final String ARG_SHOW_WEATHER = "param4";
    private static final String ARG_DARK_SKY_API_KEY = "param5";
    private static final String ARG_USE_DARK_SKY = "param6";


    private boolean mUse24HourTime;
    private boolean mShowTemperature;
    private boolean mUseCelsius;
    private boolean mShowWeather;
    private String mDarkSkyAPIKey;
    private boolean mUseDarkSky;

    private OnFragmentInteractionListener mListener;


    private ImageView watchPreviewImageView;

    //declared properties from the actual watch face
    private Calendar mCalendar;
    private FusedLocationProviderClient mFusedLocationClient;
    private final Bitmap wearOSBitmap = drawableToBitmap(getContext().getDrawable(R.drawable.ic_wear_os_logo));
    private boolean mRegisteredTimeZoneReceiver = false;
    private Paint mBackgroundPaint;
    private Paint mTimePaint;
    private Paint mDatePaint;
    /**
     * Whether the display supports fewer bits for each color in ambient mode. When true, we
     * disable anti-aliasing in ambient mode.
     */
    private boolean mLowBitAmbient;
    private boolean mBurnInProtection;
    private boolean mAmbient;

    private float mIconTitleYOffset;

    private Typeface mProductSans;

    private long mLastWeatherUpdateTime = 0;
    private long mLastWeatherUpdateFailedTime = 0;
    private CurrentWeather mLastWeather;
    private final long ONE_MIN = 60000;
    private long mGetLastLocationCalled = 0;

    private boolean mSubscriptionActive;

    SharedPreferences mSharedPreferences;
    private final int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 1;
    private boolean forceWeatherUpdate = false;

    public WatchPreview() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *

     * @return A new instance of fragment WatchPreview.
     */
    // TODO: Rename and change types and number of parameters
    public static WatchPreview newInstance(boolean mUse24HourTime, boolean mShowTemperature,
                                           boolean mUseCelsius, boolean mShowWeather, String mDarkSkyAPIKey,
                                           boolean mUseDarkSky) {
        WatchPreview fragment = new WatchPreview();
        Bundle args = new Bundle();
        args.putBoolean(ARG_USE_24_HOUR_TIME, mUse24HourTime);
        args.putBoolean(ARG_SHOW_TEMPERATURE, mShowTemperature);
        args.putBoolean(ARG_USE_CELSIUS, mUseCelsius);
        args.putBoolean(ARG_SHOW_WEATHER, mShowWeather);
        args.putString(ARG_DARK_SKY_API_KEY, mDarkSkyAPIKey);
        args.putBoolean(ARG_USE_DARK_SKY, mUseDarkSky);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUse24HourTime = getArguments().getBoolean(ARG_USE_24_HOUR_TIME);
            mShowTemperature = getArguments().getBoolean(ARG_SHOW_TEMPERATURE);
            mUseCelsius = getArguments().getBoolean(ARG_USE_CELSIUS);
            mShowWeather = getArguments().getBoolean(ARG_SHOW_WEATHER);
            mDarkSkyAPIKey = getArguments().getString(ARG_DARK_SKY_API_KEY);
            mUseDarkSky = getArguments().getBoolean(ARG_USE_DARK_SKY);
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mCalendar = Calendar.getInstance();
        //Resources resources = PixelWatchFace.this.getResources();

        // Initializes background.
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(
                ContextCompat.getColor(getContext(), R.color.background));
        mProductSans = ResourcesCompat.getFont(getContext(), R.font.product_sans_regular);

        // Initializes Watch Face.
        mTimePaint = new Paint();
        mTimePaint.setTypeface(mProductSans);
        mTimePaint.setAntiAlias(true);
        mTimePaint.setColor(
                ContextCompat.getColor(getContext(), R.color.digital_text));
        mDatePaint = new Paint();
        mDatePaint.setTypeface(mProductSans);
        mDatePaint.setAntiAlias(true);
        mDatePaint.setColor(ContextCompat.getColor(getContext(), R.color.digital_text));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        watchPreviewImageView = container.findViewById(R.id.watchPreview);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_watch_preview, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
