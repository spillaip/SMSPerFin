package com.example.spillaip.sms_app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    ListView listitem;
    String TAG = "MYSMS";
    double NAV;
    String _scheme;
    double _units;
    Date _msgDate;
    long _eventId = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Validate and get Permissions
        validatePermission();
        //Get Calendars
        int calStatus = get_calendars();
        //_eventId = insert_event("Hello");


        listitem = (ListView) findViewById(R.id.SMSList);

        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            OrganizeInBox();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
        }

    }

    public void OrganizeInBox() {
        String _hdfc = "_Blank";
        Uri mSmsQueryUri = Uri.parse("content://sms/inbox");
        List<String> messages = new ArrayList<String>();

        Cursor cursor = null;
        try {
            // Add SMS Read Permision At Runtime
            // Todo : If Permission Is Not GRANTED

            cursor = getContentResolver().query(mSmsQueryUri, null, null, null, null);
            if (cursor == null) {
                Log.i(TAG, "cursor is null. uri: " + mSmsQueryUri);

            }
            for (boolean hasData = cursor.moveToFirst(); hasData; hasData = cursor.moveToNext()) {

                final String _person = cursor.getString(cursor.getColumnIndexOrThrow("person"));

                final String _id = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                final String _thread_id = cursor.getString(cursor.getColumnIndexOrThrow("thread_id"));
                final String _address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                final String _date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                _msgDate = new Date(cursor.getLong(4));
                Log.d(TAG,"Date is "+_msgDate);
                String formattedDate = new SimpleDateFormat("dd/MM/yyyy").format(_msgDate);
                final String _protocol = cursor.getString(cursor.getColumnIndexOrThrow("protocol"));
                final String _read = cursor.getString(cursor.getColumnIndexOrThrow("read"));
                final String _status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
                final String _type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                final String _subject = cursor.getString(cursor.getColumnIndexOrThrow("subject"));
                final String _body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                final String _service_center = cursor.getString(cursor.getColumnIndexOrThrow("service_center"));
                final String _error_code = cursor.getString(cursor.getColumnIndexOrThrow("error_code"));
                final String _seen = cursor.getString(cursor.getColumnIndexOrThrow("seen"));
                final String _personName = getContactName(this.getApplicationContext(), _address);
                //Log.d("TAG","After contact Name");
                final String body = _person + ";" + _personName + ";" + _id + ";" + _thread_id + ";" + _address + ";" + formattedDate + ";" + _protocol + ";" + _read + ";" + _status + ";" + _type + ";" + _subject + ";" + _body + ";" + _service_center + ";" + _error_code + ";" + _seen;
                if (_address.contains("HDFC")) {
                    if (_body.contains("processed"))
                        _hdfc = getHDFC(body);
                        messages.add(formattedDate + "," + _hdfc);
                   // _eventId = insert_event(_msgDate, _hdfc);
                }
                if (_address.contains("ABCINV")) {
                    String _birla;
                    if (_body.contains("processed"))
                        _birla = getBIRLA(body);
                    messages.add(formattedDate + "," + _hdfc);
                    // _eventId = insert_event(_msgDate, _hdfc);
                }

            }
        } catch (Exception e) {
            Log.e(TAG, "Error is "+e.getMessage());
        } finally {
            if (cursor != null)
                cursor.close();
        }

        listitem.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, messages));
    }

    public String getContactName(Context context, String phoneNumber) {
        String contactName = "Unknown";
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_CONTACTS") == PackageManager.PERMISSION_GRANTED) {

            ContentResolver cr = context.getContentResolver();
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(phoneNumber));
            Cursor cursor = cr.query(uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (cursor == null) {
                return null;
            }

            if (cursor.moveToFirst()) {
                contactName = cursor.getString(cursor
                        .getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                if (contactName == null) {
                    contactName = "Unknown";
                }
            }
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return contactName;
    }

    public String getHDFC(String body) {
        String _output = "Unknown";
        String[] first = body.split(" under ");
        String[] second = first[1].split(" for ");

        String _schemeName = second[0];
        String _folio[] = first[0].split("Folio");
        String _rs[] = body.split("Rs. ");
        String _amount[] = _rs[1].split(" has ");
        String _getNav[] = body.split("NAV of ");
        String _NAV[] = _getNav[1].split(" for ");
        String _getUnits[] = _NAV[1].split(" units");
        String _units = _getUnits[0];

        _output = _folio[1] + "," + _schemeName + "," + _amount[0] + "," + _NAV[0] + "," + _units;


        return _output;
    }


    public String getBIRLA(String body) {
        String _output = "Unknown";
        String[] first = body.split(" under ");
        String[] second = first[1].split(" for ");

        String _schemeName = second[0];
        String _folio[] = first[0].split("Folio");
        String _rs[] = body.split("Rs. ");
        String _amount[] = _rs[1].split(" has ");
        String _getNav[] = body.split("NAV of ");
        String _NAV[] = _getNav[1].split(" for ");
        String _getUnits[] = _NAV[1].split(" units");
        String _units = _getUnits[0];

        _output = _folio[1] + "," + _schemeName + "," + _amount[0] + "," + _NAV[0] + "," + _units;


        return _output;
    }

    public int get_calendars() {
        //Todo : Read calendar Name as MF_TRX

        // Projection array. Creating indices for this array instead of doing
// dynamic lookups improves performance.
        final String[] EVENT_PROJECTION = new String[]{
                CalendarContract.Calendars._ID,                           // 0
                CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 2
                CalendarContract.Calendars.OWNER_ACCOUNT                  // 3
        };

// The indices for the projection array above.
        final int PROJECTION_ID_INDEX = 0;
        final int PROJECTION_ACCOUNT_NAME_INDEX = 1;
        final int PROJECTION_DISPLAY_NAME_INDEX = 2;
        final int PROJECTION_OWNER_ACCOUNT_INDEX = 3;

        // Run query
        Cursor cur = null;
        ContentResolver cr = getContentResolver();
        Uri uri = CalendarContract.Calendars.CONTENT_URI;
        String selection = "((" + CalendarContract.Calendars.ACCOUNT_NAME + " = ?))";
        //+ CalendarContract.Calendars.ACCOUNT_TYPE + " = ?) AND ("
        //+ CalendarContract.Calendars.OWNER_ACCOUNT + " = ?))";
        String[] selectionArgs = new String[]{"spillaip@gmail.com"};
        // Submit the query and get a Cursor object back.
        // ToDo : Query MF_TRX only
        cur = cr.query(uri, EVENT_PROJECTION, selection, selectionArgs, null);

        // Use the cursor to step through the returned records
        while (cur.moveToNext()) {
            long calID = 0;
            String displayName = null;
            String accountName = null;
            String ownerName = null;

            // Get the field values
            calID = cur.getLong(PROJECTION_ID_INDEX);
            displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX);
            accountName = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX);
            ownerName = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX);

            //ToDo: Do something with the values...
            Log.d(TAG, "Cal Info: " + calID + "-" + displayName + "-" + accountName + "-" + ownerName);

        }


        return -1;
    }

    public int new_calendar() {
        //Todo : Create a calendar named MF_TRX and return the calendar ID and store in STORAGE
        //https://developer.android.com/guide/topics/providers/calendar-provider
        return -1;
    }

    public long insert_event(Date calDate, String calData) {
        //Todo : Insert the event into the calendar
        Log.d(TAG,"calData is "+calData);
        long calID = 14;
        long startMillis = 0;
        long endMillis = 0;
        int dt = calDate.getDay();
        int mm = calDate.getMonth();
        int yr = calDate.getYear();
        int minute = calDate.getMinutes();
        int hr = calDate.getHours();
        Calendar beginTime = Calendar.getInstance();
        //beginTime.set(yr, mm, dt);
        beginTime.setTime(calDate);


        startMillis = beginTime.getTimeInMillis();
        Calendar endTime = Calendar.getInstance();
        //endTime.set(yr, mm, dt);
        endTime.setTime(calDate);
        endMillis = endTime.getTimeInMillis();

        Log.d(TAG,"After Date parsing");

        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, startMillis);
        values.put(CalendarContract.Events.DTEND, endMillis);
        values.put(CalendarContract.Events.TITLE, calData);
        values.put(CalendarContract.Events.DESCRIPTION, calData);
        values.put(CalendarContract.Events.CALENDAR_ID, calID);
        values.put(CalendarContract.Events.ALL_DAY,1);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Calcutta");
        Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);

        // get the event ID that is the last element in the Uri
        //long eventID = Long.parseLong(uri.getLastPathSegment());


        return -1;
    }

    public int validate_event(int eventID) {
        //Todo : Retrieve eventId and return the status
        return -1;

    }

    public void validatePermission() {
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_CALENDAR") == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ_CALENDAR present");
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_CALENDAR"}, REQUEST_CODE_ASK_PERMISSIONS);
        }
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.WRITE_CALENDAR") == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "WRITE_CALENDAR present");
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.WRITE_CALENDAR"}, REQUEST_CODE_ASK_PERMISSIONS);
        }
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ_SMS present");
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
        }
        if (ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_CONTACTS") == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "READ_CONTACTS present");
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.READ_CONTACTS"}, REQUEST_CODE_ASK_PERMISSIONS);
        }

    }
}
