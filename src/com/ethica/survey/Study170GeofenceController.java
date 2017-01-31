
package com.ethica.survey;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.database.Cursor;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.ethica.logger.api.data.DataContract;

/**
 * @author Mohammad Hashemian (mohammad@ethicadata.com)
 */
public class Study170GeofenceController {

    public static final SimpleDateFormat sSimpleDateFormat =
            new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    private static final int NO_SUVERY = -1;
    private static final int ENTER_CAMPUS_SURVEY = 1;
    private static final int MIN_ACCEPTABLE_ACCURACY = 50;
    private static final long MIN_STAY_MS = 5 * 60 * 1000;
    private static final String SP_KEY_CURRENT_POI = "study_170_current_poi";
    private static final String SP_KEY_TIME_OF_ENTER_CURRENT_POI = "study_170_time_of_enter";

    public static final String TAG = "ETHICA:S170Geofence";
    private static final boolean DEBUG = true;

    private int mVersionCode;
    private Context mContext = null;
    private PointsOfInterest mCurrentPOI = null;
    private long mTimeOfEnteringCurLoc = 0;
    private boolean mCurPoiSurveyIssued = false;
    private long mPreviousCycleInMs = -1;

    public void init(final Context c) {
        mContext = c;
        try {
            mVersionCode = mContext.getPackageManager()
                    .getPackageInfo(mContext.getPackageName(), 0).versionCode;
        } catch (final NameNotFoundException e) {
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        mCurrentPOI = PointsOfInterest.fromString(sharedPref.getString(SP_KEY_CURRENT_POI, ""));
        mTimeOfEnteringCurLoc = sharedPref.getLong(SP_KEY_TIME_OF_ENTER_CURRENT_POI, 0);
        mCurPoiSurveyIssued = true;

        sendReport("Initializing.");
        // Initialize previous cycle to 5 minutes ago.
        mPreviousCycleInMs = System.currentTimeMillis() - (5 * 60 * 1000);
    }

    public Object[] shouldShow() {
        StringBuilder sbDebug = new StringBuilder();
        sbDebug.append("Starting from ").append(mPreviousCycleInMs);
        Cursor c = mContext.getContentResolver().query(
                    DataContract.Gps.CONTENT_URI,
                    DataContract.Gps.ALL_COLUMNS,
                    DataContract.Gps.COLUMN_NAME_KEY_TIMESTAMP + " >= ? AND " +
                            DataContract.Gps.COLUMN_NAME_KEY_ACCU + " < ?",
                    new String[]{
                            String.valueOf(mPreviousCycleInMs),
                            String.valueOf(MIN_ACCEPTABLE_ACCURACY)},
                    DataContract.Gps.COLUMN_NAME_KEY_TIMESTAMP);
        mPreviousCycleInMs = System.currentTimeMillis();
        if (c == null) {
            sbDebug.append(" null cursor");
            sendReport(sbDebug.toString());
            return new Object[] {NO_SUVERY};
        }
        if (!c.moveToFirst()) {
            sbDebug.append(" Empty cursor");
            sendReport(sbDebug.toString());
            c.close();
            return new Object[] {NO_SUVERY};
        }

        PointsOfInterest newPOI;
        PointsOfInterest surveyPOI = null;
        do {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(
                    c.getLong(c.getColumnIndexOrThrow(DataContract.Gps.COLUMN_NAME_KEY_TIMESTAMP)));
            final LatLng newLocFix = new LatLng(1,
                    c.getDouble(c.getColumnIndexOrThrow(DataContract.Gps.COLUMN_NAME_KEY_LAT)),
                    c.getDouble(c.getColumnIndexOrThrow(DataContract.Gps.COLUMN_NAME_KEY_LON)),
                    0.0, 0.0, cal);

            newPOI = PointsOfInterest.whereIs(newLocFix);
            if (newPOI == null) {
                continue;
            }
            if (mCurrentPOI == null) {
                mCurrentPOI = newPOI;
                sbDebug.append(" Initial location is " + mCurrentPOI.toString());
                mCurPoiSurveyIssued = true;
            } else if (newPOI != mCurrentPOI) {
                sbDebug.append(" Moved from " + mCurrentPOI.toString() +
                               " to " + newPOI.toString());
                mCurrentPOI = newPOI;
                mTimeOfEnteringCurLoc = newLocFix.timestamp.getTimeInMillis();
                mCurPoiSurveyIssued = false;
                surveyPOI = null;
            } else if (!mCurPoiSurveyIssued &&
                (newLocFix.timestamp.getTimeInMillis() - mTimeOfEnteringCurLoc) > MIN_STAY_MS) {
                sbDebug.append(" stayed in ").append(newPOI).append(" long enough. Issuing survey.");
                mCurPoiSurveyIssued = true;
                surveyPOI = newPOI;
            }
        } while (c.moveToNext());

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        sharedPref.edit()
                    .putString(SP_KEY_CURRENT_POI, mCurrentPOI == null ? "" : mCurrentPOI.getName())
                    .putLong(SP_KEY_TIME_OF_ENTER_CURRENT_POI, mTimeOfEnteringCurLoc)
                    .apply();

        if (surveyPOI != null) {
            if (surveyPOI == PointsOfInterest.OFF_CAMPUS) {
                sbDebug.append("Was off campus. Not issuing.");
            } else {
                sbDebug.append("Was on campus. Issuing.");
            }
            sendReport(sbDebug.toString());
            int qSetId = surveyPOI == PointsOfInterest.OFF_CAMPUS ? NO_SUVERY : ENTER_CAMPUS_SURVEY;
            return new Object[] {qSetId};
        } else {
            sbDebug.append(" Not Issuing");
        }
        sendReport(sbDebug.toString());
        return new Object[] {NO_SUVERY};
    }

    /**
     * Records the message and uploads it to server.
     */
    public void sendReport(final String message) {
        final ContentValues cv = new ContentValues();
        cv.put(DataContract.LogMessage.COLUMN_NAME_KEY_TIMESTAMP, System.currentTimeMillis());
        cv.put(DataContract.LogMessage.COLUMN_NAME_KEY_VERSION_CODE, mVersionCode);
        cv.put(DataContract.LogMessage.COLUMN_NAME_KEY_MESSAGE, message);
        cv.put(DataContract.LogMessage.COLUMN_NAME_KEY_TAG, TAG);
        mContext.getContentResolver().insert(DataContract.LogMessage.CONTENT_URI, cv);
        Log.d(TAG, message);
    }
}
