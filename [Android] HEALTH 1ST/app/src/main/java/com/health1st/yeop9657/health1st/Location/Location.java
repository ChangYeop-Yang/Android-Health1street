package com.health1st.yeop9657.health1st.Location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.health1st.yeop9657.health1st.ResourceData.BasicData;

import java.io.IOException;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

/**
 * Created by yeop on 2017. 9. 18..
 */

public class Location implements LocationListener
{
    /* POINT - : Context */
    private Context mContext = null;

    /* POINT - : Double */
    private double dLatitude = 0.0;
    private double dLongitude = 0.0;
    private double rangeLatitude = 0.0;
    private double rangeLongitude = 0.0;

    /* POINT - : Boolean */
    private Boolean bGPSEnabled = false;
    private Boolean bNetworkEnabled = false;
    private Boolean isLocationStated = true;

    /* POINT - : String */
    private String mTelNumber = null;

    /* POINT - : LocationManager */
    private LocationManager mLocationManager = null;

    /* POINT - : Long */
    private final static long MIN_DISTANCE_UPDATE = 10;
    private final static long MIN_TIME_UPDATE = 1000 * 60 * 1;

    /* POINT - : GoogleMap */
    private GoogleMap mGoogle = null;

    /* POINT - : Location Creator */
    public Location(final Context mContext, final GoogleMap mGoogle, final SharedPreferences mSharedPreferences) {
        this.mContext = mContext;
        this.mGoogle = mGoogle;
        setLocationManager();

        /* POINT - : Shared Preference */
        final String rangeLatLng = mSharedPreferences.getString(BasicData.LOCATION_PATIENT_KEY, null);
        if (rangeLatLng != null) { rangeLatitude = Double.valueOf(rangeLatLng.split(",")[0]); rangeLongitude = Double.valueOf(rangeLatLng.split(",")[1]); isLocationStated = false; }
        mTelNumber = mSharedPreferences.getString(BasicData.SHARED_HELPER_TEL, null);
    }

    /* MARK - : User Custom Method */
    @SuppressLint("MissingPermission")
    private void setLocationManager()
    {
        try
        {
            mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
            /* POINT - : Import GPS Information */
            bGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            /* POINT - : Import Network Information */
            bNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            /* POINT - : Check GPS and Network Provider */
            if (!bGPSEnabled && !bNetworkEnabled) { return; }

            /* POINT - : Import Network Provider */
            if (bNetworkEnabled) {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_UPDATE, MIN_DISTANCE_UPDATE, this);

                if (mLocationManager != null) {
                    dLatitude = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLatitude();
                    dLongitude = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLongitude();
                }
            }

            /* POINT - : Import GPS Provider */
            if (bGPSEnabled) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_UPDATE, MIN_DISTANCE_UPDATE, this);

                if (mLocationManager != null) {
                    dLatitude = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLatitude();
                    dLongitude = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getLongitude();
                }
            }
        }
        catch (Exception error)
        {
            error.printStackTrace();
            Log.e("Location GPS Error!", error.getMessage());
        }
    }

    public final double getLatitude() { return this.dLatitude; }

    public final double getLongitude() { return this.dLongitude; }

    /* 위도와 경도를 통해서 지오코딩을 하여서 구글에서 주소를 반환하는 함수 */
    public final void getGEOAddress(final double Latitude, final double Longitude, final TextView mTextView) /* Latitude(위도) 와 Longitude(경도)의 인자를 받아서 사용하는 함수 */
    {
        /* Google Geocoder 을 위한 객체 생성 */
        final Geocoder geocoder = new Geocoder(mContext);

        /* 주소 관련 변수 */
        List<Address> list = null;

        try { list = geocoder.getFromLocation(Latitude, Longitude, 1);}
        catch (NumberFormatException e) { e.printStackTrace(); }
        catch (IOException e) { e.printStackTrace(); }

          /* 해당 지역의 정보를 받은 뒤 작동이 되는 구문 */
        if( (list != null) && (list.size()>0) ) {
            final Address address = list.get(0);

            String asAddress[] = new String[5];
            asAddress[0] = address.getPostalCode() == null ? null : address.getPostalCode();
            asAddress[1] = address.getLocality() == null ? null : address.getLocality();
            asAddress[2] = address.getSubLocality() == null ? null : address.getSubLocality();
            asAddress[3] = address.getThoroughfare() == null ? null : address.getThoroughfare();
            asAddress[4] = address.getFeatureName() == null ? null : address.getFeatureName();

            final StringBuffer mStringBuffer = new StringBuffer();
            for (final String sString : asAddress)
            { if (sString != null) { mStringBuffer.append(sString).append(" "); }  }

            /* 해당 텍스트 뷰에 출력 */
            mTextView.setText(mStringBuffer.toString());
        }
        /* 해당 주소가 없을 경우 주소를 찾을 수 없는 경고문을 텍스트 뷰에 출력 */
        else { mTextView.setText("Not Found Address."); }
    }

    /* TODO - : Location Listener */
    @Override
    public void onLocationChanged(android.location.Location location) {
        if (mLocationManager != null) {
            dLatitude = location.getLatitude();
            dLongitude = location.getLongitude();
            mGoogle.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(getLatitude(), getLongitude()), 15));

            /* POINT - : Check Lat/Long Range */
            final int mRange = (int) distanceFrom(dLatitude, dLongitude, rangeLatitude, rangeLongitude);
            if (mRange > BasicData.PATIENT_RANGE_VALUE && !isLocationStated) {

                /* POINT - : Vibrate */
                final Vibrator mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                mVibrator.vibrate(BasicData.VIBRATE_VALUE);

                /* POINT - : SMS Manager */
                SmsManager.getDefault().sendTextMessage(mTelNumber, null, String.format("[PATIENT] 위치 이탈 Lat: %f, Long: %f.", dLatitude, dLongitude), null, null);

                /* POINT - : SweetAlertDialog */
                final SweetAlertDialog mSweetAlertDialog = new SweetAlertDialog(mContext, SweetAlertDialog.WARNING_TYPE);
                mSweetAlertDialog.setTitleText("Excess Location Range").setContentText("지정 된 위치에서 벗어났습니다.\n보호자에게 문자를 전송하였습니다.");
                mSweetAlertDialog.setConfirmText("통화").setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onClick(SweetAlertDialog sweetAlertDialog) {
                        mContext.startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(String.format("tel:%s", mTelNumber))));
                        sweetAlertDialog.cancel();
                    }
                }).show();

                isLocationStated = true;
            }
        }
    }

    @Override public void onStatusChanged(String s, int i, Bundle bundle) {}
    @Override public void onProviderEnabled(String s) {}
    @Override public void onProviderDisabled(String s) {}

    /* TODO - : Distance Calculation Method */
    private final double distanceFrom(double lat1, double lng1, double lat2, double lng2)
    {
        final double EARTH_RADIOUS = 3958.75; // Earth radius;
        final int METER_CONVERSION = 1609;

        // Return distance between 2 points, stored as 2 pair location;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = EARTH_RADIOUS * c;
        return new Double(dist * METER_CONVERSION).floatValue();
    }
}
