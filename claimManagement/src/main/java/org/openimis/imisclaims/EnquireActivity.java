package org.openimis.imisclaims;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.openimis.CallSoap.CallSoap;
import org.openimis.general.General;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.openimis.imisclaims.R;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class EnquireActivity extends AppCompatActivity {

    General _General = new General();
   // downloadFile df = new downloadFile();

    EditText etCHFID;
    TextView tvCHFID,tvName,tvGender,tvDOB;
    ImageButton btnGo,btnScan;
    ListView lv;
    ImageView iv;
    LinearLayout ll;
    ProgressDialog pd;
    NotificationManager mNotificationManager;
    Vibrator vibrator;

    ArrayList<HashMap<String, String>> PolicyList = new ArrayList<HashMap<String,String>>();

    static String Language;
    Bitmap theImage;

    final String ApkFileLocation = _General.getDomain() + "/Apps/Enquire.apk";
    final String VersionField = "AppVersionEnquire";
    final String Path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/IMIS/";
    //final CharSequence[] lang = {"English","Swahili"};
    final CharSequence[] lang = {"English","Francais"};
    final int SIMPLE_NOTIFICATION_ID = 1;


    String result;

    AlertDialog ad;

    SQLiteDatabase db;

    private boolean ZoomOut = false;
    private int orgHeight, orgWidth;

    public TextView getTvCHFID() {
        return tvCHFID;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enquire);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.app_name_enquire));
        actionBar.setDisplayHomeAsUpEnabled(true);


        isSDCardAvailable();

        //Check if network available
        if (_General.isNetworkAvailable(EnquireActivity.this)) {
//			        	tvMode.setText(Html.fromHtml("<font color='green'>Online mode.</font>"));

        } else {
//			        	tvMode.setText(Html.fromHtml("<font color='red'>Offline mode.</font>"));
            setTitle(getResources().getString(R.string.app_name) + "-" + getResources().getString(R.string.OfflineMode));
            setTitleColor(getResources().getColor(R.color.Red));
        }


/*        new Thread() {
            public void run() {
                CheckForUpdates();
            }

        }.start();*/
        etCHFID = (EditText) findViewById(R.id.etCHFID);
        tvCHFID = (TextView) findViewById(R.id.tvCHFID);
        tvName = (TextView) findViewById(R.id.tvName);
        tvDOB = (TextView) findViewById(R.id.tvDOB);
        tvGender = (TextView) findViewById(R.id.tvGender);
        iv = (ImageView) findViewById(R.id.imageView);
        btnGo = (ImageButton) findViewById(R.id.btnGo);
        btnScan = (ImageButton) findViewById(R.id.btnScan);
        lv = (ListView) findViewById(R.id.listView1);
        ll = (LinearLayout) findViewById(R.id.llListView);


        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ZoomOut) {
                    iv.setLayoutParams(new LinearLayout.LayoutParams(orgWidth, orgHeight));
                    iv.setAdjustViewBounds(true);
                    ZoomOut = false;
                } else {
                    orgWidth = iv.getWidth();
                    orgHeight = iv.getHeight();
                    iv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    ZoomOut = true;
                }
            }
        });

        btnGo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                ClearForm();
                Escape escape = new Escape();
                if (!escape.CheckCHFID(etCHFID.getText().toString())) {
                    ShowDialog(tvCHFID, getResources().getString(R.string.MissingCHFID));
                    return;
                }


                pd = ProgressDialog.show(EnquireActivity.this, "", getResources().getString(R.string.GetingInsuuree));
                new Thread() {
                    public void run() {
                        getInsureeInfo();

                        pd.dismiss();
                    }
                }.start();
            }
        });

        btnScan.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                startActivityForResult(intent, 1);

                ClearForm();
                //if (!CheckCHFID())return;


            }
        });

        etCHFID.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    ClearForm();
                    Escape escape = new Escape();
                    if (!escape.CheckCHFID(etCHFID.getText().toString())) return false;

                    pd = ProgressDialog.show(EnquireActivity.this, "", getResources().getString(R.string.GetingInsuuree));
                    new Thread() {
                        public void run() {
                            getInsureeInfo();

                            pd.dismiss();
                        }
                    }.start();
                }
                return false;
            }
        });

    }

    /*private void CheckForUpdates(){
        if(_General.isNetworkAvailable(EnquireActivity.this)){
            if(_General.isNewVersionAvailable(VersionField,EnquireActivity.this,getApplicationContext().getPackageName())){
                //Show notification bar
                mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

                final Notification NotificationDetails = new Notification(R.drawable.enquire, getResources().getString(R.string.NotificationAlertText), System.currentTimeMillis());

                NotificationDetails.flags = Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_AUTO_CANCEL | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE;

                Context context = getApplicationContext();
                CharSequence ContentTitle = getResources().getString(R.string.ContentTitle);
                CharSequence ContentText = getResources().getString(R.string.ContentText);

                Intent NotifyIntent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(ApkFileLocation));

                PendingIntent intent = PendingIntent.getActivity(EnquireActivity.this, 0, NotifyIntent,0);
                NotificationDetails.setLatestEventInfo(context, ContentTitle, ContentText, intent);

                mNotificationManager.notify(SIMPLE_NOTIFICATION_ID, NotificationDetails);

                vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                vibrator.vibrate(500);

            }
        }
    }*/
    /*private void CheckForUpdates(){
        if(_General.isNetworkAvailable(EnquireActivity.this)){
            if(_General.isNewVersionAvailable(VersionField,EnquireActivity.this,getApplicationContext().getPackageName())){
                //Show notification bar
                mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

                //final Notification NotificationDetails = new Notification(R.drawable.ic_launcher, getResources().getString(R.string.NotificationAlertText), System.currentTimeMillis());
                //NotificationDetails.flags = Notification.FLAG_SHOW_LIGHTS | Notification.FLAG_AUTO_CANCEL | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE;
                //NotificationDetails.setLatestEventInfo(context, ContentTitle, ContentText, intent);
                //mNotificationManager.notify(SIMPLE_NOTFICATION_ID, NotificationDetails);
                Context context = getApplicationContext();
                CharSequence ContentTitle = getResources().getString(R.string.ContentTitle);
                CharSequence ContentText = getResources().getString(R.string.ContentText);

                Intent NotifyIntent = new Intent(this, EnquireActivity.class);

                PendingIntent intent = PendingIntent.getActivity(this, 0, NotifyIntent,PendingIntent.FLAG_CANCEL_CURRENT);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                builder.setAutoCancel(false);
                builder.setContentTitle(ContentTitle);
                builder.setContentText(ContentText);
                //builder.setSmallIcon(R.drawable.silverware);
                builder.setContentIntent(intent);
                builder.setOngoing(false);
*//*				String s = "ring";
				int res_sound_id = context.getResources().getIdentifier(s, "raw", context.getPackageName());
				Uri u = Uri.parse("android.resource://" + context.getPackageName() + "/" + res_sound_id);
				builder.setSound(u);*//*

                mNotificationManager.notify(SIMPLE_NOTIFICATION_ID, builder.build());
                vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                vibrator.vibrate(500);
            }
        }



    }*/


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            switch(requestCode){

                case 1:
                    if (resultCode == RESULT_OK){
                        String CHFID = data.getStringExtra("SCAN_RESULT");
                        etCHFID.setText(CHFID);

                        Escape escape = new Escape();
                        if (!escape.CheckCHFID(etCHFID.getText().toString())) return;

                        pd = ProgressDialog.show(EnquireActivity.this, "", getResources().getString(R.string.GetingInsuuree));
                        new Thread(){
                            public void run(){
                                getInsureeInfo();

                                pd.dismiss();
                            }
                        }.start();

                    }
                    break;

            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void isSDCardAvailable(){

        if (_General.isSDCardAvailable() == 0){
            //Toast.makeText(this, "SD Card is in read only mode.", Toast.LENGTH_LONG);
            new AlertDialog.Builder(this)
                    .setMessage(getResources().getString(R.string.ReadOnly))
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.ForceClose), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).show();

        }else if(_General.isSDCardAvailable() == -1){
            new AlertDialog.Builder(this)
                    .setMessage(getResources().getString(R.string.NoSDCard))
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.ForceClose), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).create().show();
        }else{

        }
    }

    protected AlertDialog ShowDialog(final TextView tv,String msg){
        return new AlertDialog.Builder(this)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(R.string.Ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tv.requestFocus();
                    }
                }).show();
    }

    @SuppressLint("WrongConstant")
    private String getDataFromDb(String chfid){

        try{

            result = "[{";

            db = openOrCreateDatabase(Path +"ImisData.db3",SQLiteDatabase.OPEN_READONLY, null);

            String[] columns = {"CHFID" ,"Photo" , "InsureeName", "DOB", "Gender","ProductCode", "ProductName", "ExpiryDate", "Status", "DedType", "Ded1", "Ded2", "Ceiling1", "Ceiling2"};

            Cursor c = db.query("tblPolicyInquiry", columns, "Trim(CHFID)=" + "\'"+ chfid +"\'" , null, null, null, null);

            int i = 0;
            boolean _isHeadingDone = false;

            for(c.moveToFirst();!c.isAfterLast();c.moveToNext()){
                for(i=0;i<5;i++){
                    if (!_isHeadingDone){
                        if (c.getColumnName(i).equalsIgnoreCase("photo")){
                            byte[] photo = c.getBlob(i);
                            if (photo != null){
                                ByteArrayInputStream is = new ByteArrayInputStream(photo);
                                theImage = BitmapFactory.decodeStream(is);
                            }
                            continue;
                        }
                        result = result + "\"" + c.getColumnName(i) + "\":" + "\"" + c.getString(i) + "\",";
                    }else{

                    }
                }
                _isHeadingDone = true;

                if (c.isFirst())
                    result = result + "\"" + "Details" + "\":[{" ;
                else
                    result = result + "{";

                for(i=5;i<c.getColumnCount();i++){

                    result = result + "\"" + c.getColumnName(i) + "\":" + "\"" + c.getString(i) + "\"";
                    if(i < c.getColumnCount() - 1)
                        result = result + ",";
                    else{
                        result = result + "}";
                        if (!c.isLast())result = result + ",";
                    }

                }
                //result = result + "]}";
            }

            result = result + "]}]";

        }catch(Exception e){
            result = e.toString();
        }

        return result;

    }

    private void getInsureeInfo(){
        String chfid = etCHFID.getText().toString();
        result = "";

        if(_General.isNetworkAvailable(this)){
            try {
                ToRestApi rest = new ToRestApi();
                String res = rest.getObjectFromRestApiToken("insuree/" + chfid + "/enquire");
                JSONObject obj = new JSONObject(res);
                JSONArray arr = new JSONArray();
                arr.put(obj);

                result = arr.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            //TODO: yet to be done
            result = getDataFromDb(etCHFID.getText().toString());
        }

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                try {

                    JSONArray jsonArray = new JSONArray(result);


                    if(jsonArray.length() == 0 ){

                        ShowDialog(getResources().getString(R.string.RecordNotFound));

                        return;
                    }

                    ll.setVisibility(View.VISIBLE);

                    int i = 0;
                    for(i = 0;i< jsonArray.length();i++){
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        if (!etCHFID.getText().toString().trim().equals(jsonObject.getString("chfid").trim())) continue;

                        tvCHFID.setText(jsonObject.getString("chfid"));
                        tvName.setText(jsonObject.getString("insureeName"));
                        tvDOB.setText(jsonObject.getString("dob"));//Adjust
                        tvGender.setText(jsonObject.getString("gender"));



                        if (_General.isNetworkAvailable(EnquireActivity.this)){

                            String photo_url_str =_General.getDomain() + "/" + jsonObject.getString("photoPath");

                            iv.setImageResource(R.drawable.person);
                            Picasso.with(getApplicationContext())
                                    .load(photo_url_str)
                                    .placeholder(R.drawable.person)
                                    .error(R.drawable.person).into(iv);

                        }else{

                            if(theImage != null){
                                iv.setImageBitmap(theImage);
                            }else {
                                byte[] photo = jsonObject.getString("photoPath").getBytes();
                                ByteArrayInputStream is = new ByteArrayInputStream(photo);
                                theImage = BitmapFactory.decodeStream(is);
                                if(theImage != null){
                                    iv.setImageBitmap(theImage);
                                }else{
                                    iv.setImageResource(R.drawable.person);
                                }
                            }


                        }


                        jsonArray = new JSONArray(jsonObject.getString("details"));


                        for(i = 0;i< jsonArray.length();i++){
                            jsonObject = jsonArray.getJSONObject(i);


                            HashMap<String, String> Policy = new HashMap<>();
                            jsonObject = jsonArray.getJSONObject(i);
                            double iDedType = 0;
                            if(!jsonObject.getString("DedType").equalsIgnoreCase("null"))iDedType = Double.valueOf(jsonObject.getString("dedType"));

                            String Ded = "",Ded1= "",Ded2 = "";
                            String Ceiling = "",Ceiling1 = "", Ceiling2 ="";

                            String jDed1 = "",jDed2 = "",jCeiling1 = "",jCeiling2 = "";

                            if(jsonObject.getString("ded1").equalsIgnoreCase("null")) jDed1 = "";else jDed1 = jsonObject.getString("ded1");
                            if(jsonObject.getString("ded2").equalsIgnoreCase("null")) jDed2 = "";else jDed2 = jsonObject.getString("ded2");
                            if(jsonObject.getString("ceiling1").equalsIgnoreCase("null")) jCeiling1 = "";else jCeiling1 = jsonObject.getString("ceiling1");
                            if(jsonObject.getString("ceiling2").equalsIgnoreCase("null")) jCeiling2 = "";else jCeiling2 = jsonObject.getString("ceiling2");

                            //Get the type

                            if(iDedType == 1 | iDedType == 2 | iDedType == 3){
                                if (!jDed1.equals(""))Ded1 = jsonObject.getString("ded1");
                                if (!jCeiling1.equals(""))Ceiling1 = jsonObject.getString("ceiling1");

                                if(!Ded1.equals("")) Ded = "Deduction: " + Ded1;
                                if(!Ceiling1.equals("")) Ceiling = "Ceiling: " + Ceiling1;

                            }else if(iDedType == 1.1 | iDedType == 2.1 | iDedType == 3.1){

                                if (jDed1.length() > 0)Ded1 = " IP:" + jsonObject.getString("ded1");
                                if (jDed2.length() > 0)Ded2 = " OP:" + jsonObject.getString("ded2");
                                if (jCeiling1.length() > 0)Ceiling1 = " IP:" +  jsonObject.getString("ceiling1");
                                if (jCeiling2.length() > 0)Ceiling2 = " OP:" +  jsonObject.getString("ceiling2");

                                if (!(Ded1 + Ded2).equals("")) Ded = "Deduction: "+ Ded1 + Ded2;
                                if (!(Ceiling1 + Ceiling2).equals("")) Ceiling = "Ceiling: "+ Ceiling1 + Ceiling2;

                            }


                            Policy.put("Heading", jsonObject.getString("productCode"));
                            Policy.put("Heading1", jsonObject.getString("expiryDate")+ "  " + jsonObject.getString("status"));
                            Policy.put("SubItem1", jsonObject.getString("productName"));
                            Policy.put("SubItem2",Ded);
                            Policy.put("SubItem3",Ceiling);
                            PolicyList.add(Policy);
                            etCHFID.setText("");
                            //break;
                        }
                    }
                    ListAdapter adapter = new SimpleAdapter(EnquireActivity.this,
                            PolicyList, R.layout.policylist,
                            new String[]{"Heading","Heading1","SubItem1","SubItem2","SubItem3"},
                            new int[]{R.id.tvHeading, R.id.tvHeading1, R.id.tvSubItem1, R.id.tvSubItem2, R.id.tvSubItem3}
                    );

                    lv.setAdapter(adapter);


                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    result = "";
                }catch(Exception e){
                    Log.e("Error", e.toString());
                    result = "";
                }

            }
        });

    }
    public AlertDialog ShowDialog(String msg) {
        return new AlertDialog.Builder(this)
                .setMessage(msg)
                .setCancelable(false)

                .setPositiveButton(R.string.Ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).show();
    }

    private void ClearForm(){
        tvCHFID.setText(getResources().getString(R.string.CHFID));
        tvName.setText(getResources().getString(R.string.InsureeName));
        tvDOB.setText(getResources().getString(R.string.DOB));
        tvGender.setText(getResources().getString(R.string.Gender));
        iv.setImageResource(R.drawable.noimage);
        ll.setVisibility(View.INVISIBLE);
        PolicyList.clear();
        lv.setAdapter(null);
    }

    public boolean onOptionsItemSelected(MenuItem item){
        onBackPressed();
//        Intent myIntent = new Intent(getApplicationContext(), MainActivity.class);
//        startActivityForResult(myIntent, 0);
//        finish();
        return true;
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
