package tw.edu.stust.slm.findmoto;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableRow;
import android.widget.TextView;


public class BeaconDetailActivity extends AppCompatActivity {

    EditText edt_name,edt_detail;
    TextView tV_mac,tV_dist;
    TableRow BtnLayout_detail,BtnLayout_rename;
    Button   btnToRename,btnDelete,btnBack, btnUpdate,btnCancel,btnSetDefault;

    SQLiteDatabase db;

    int position;
    String getId,name, detail,mac,defaul;
    Cursor cursor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon_detail);
        setTitle("詳細資料");
        //引用參考
        edt_name            = findViewById(R.id.edt_name);
        edt_detail          = findViewById(R.id.edt_detail);

        tV_mac              = findViewById(R.id.tV_mac);
        tV_dist             = findViewById(R.id.tV_dist);

        BtnLayout_detail    = findViewById(R.id.BtnLayout_detail);
        BtnLayout_rename    = findViewById(R.id.BtnLayout_rename);

        btnToRename         = findViewById(R.id.btnToRename);
        btnDelete           = findViewById(R.id.btnDelete);
        btnBack             = findViewById(R.id.btnBack);
        btnUpdate           = findViewById(R.id.btnChangeName);
        btnCancel           =  findViewById(R.id.btnCancel);
        btnSetDefault       = findViewById(R.id.btnSetDefault);

        //事件設定
        btnDelete.     setOnClickListener(deleteClick);
        btnBack.       setOnClickListener(backClick);
        btnUpdate.     setOnClickListener(updateClick);
        btnCancel.     setOnClickListener(cancelClick);
        btnSetDefault. setOnClickListener(setDefaultClick);

        btnToRename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToRename();
            }
        });

        //Beacon database建立
        db= openOrCreateDatabase("test2DB.db",MODE_PRIVATE,null);

        String createTable = "create table if not exists test " +
                "(_id integer primary key autoincrement, " +
                "name text, " +
                "detail text, " +
                "mac text, " +
                "defaul text)";

        db.execSQL(createTable);

        //取出點選的資料
        Intent it = getIntent();
        position = it.getIntExtra("position",-1000);
        cursor = db.rawQuery("select * from test ", null);
        cursor.move(position+1);
        getId = cursor.getString(0);
        name = cursor.getString(1);
        detail = cursor.getString(2);
        mac = cursor.getString(3);
        defaul = cursor.getString(4);
        //丟到畫面上
        edt_name.setText(name);
        edt_detail.setText(detail);
        tV_mac.setText(mac);

        //檢查是不是預設
        if(defaul.equals("true")) {
            //是的話把預設按鈕變成已設置狀態
            btnSetDefault.setBackgroundResource(R.drawable.default_icon_style);
            btnSetDefault.setText("　已 為 預 設　");
            btnSetDefault.setEnabled(false);
        }
    }

    /** ================================================ */
    void goToRename() {
        BtnLayout_detail.setVisibility(View.GONE);
        BtnLayout_rename.setVisibility(View.VISIBLE);
        btnSetDefault.setVisibility(View.GONE);

        edt_name.setEnabled(true);
        edt_detail.setEnabled(true);
        setTitle("更改資料");
    }

    /** ================================================ */
    Button.OnClickListener deleteClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            AlertDialog dialog = new AlertDialog.Builder(BeaconDetailActivity.this)
                    .setTitle("刪除裝置")
                    .setMessage("請問確定要刪除此裝置嗎?(刪除後無法恢復)")
                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            db.delete("test", "_id=?", new String[]{String.valueOf(getId)});
                            finish();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(BeaconDetailActivity.this,R.color.colorPrimary));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
        }
    };

    /** ================================================ */
    Button.OnClickListener backClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            finish();
        }
    };

    /** ================================================ */
    Button.OnClickListener updateClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String newName      = edt_name.getText().toString();
            String newDetail    = edt_detail.getText().toString();

            //判斷edt_name內容是否為空 或大於8個字元
            if (!"".equals(newName) && newName.length() > 8) {
                new AlertDialog.Builder(BeaconDetailActivity.this)
                        .setTitle("更改錯誤")
                        .setMessage("裝置名稱不可為空或超過8個字元！")
                        .setPositiveButton("確定",null)
                        .show()//改選項顏色
                        .getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(BeaconDetailActivity.this,R.color.colorPrimary));
            }else if(newDetail.length()>30){
                new AlertDialog.Builder(BeaconDetailActivity.this)
                        .setTitle("更改錯誤")
                        .setMessage("裝置描述不可超過30個字元！")
                        .setPositiveButton("確定",null)
                        .show()//改選項顏色
                        .getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(BeaconDetailActivity.this,R.color.colorPrimary));
            }else{
                ContentValues cv = new ContentValues();
                cv.put("name", edt_name.getText().toString());
                cv.put("detail", edt_detail.getText().toString());
                db.update("test", cv, "_id=?", new String[]{String.valueOf(getId)});

                new AlertDialog.Builder(BeaconDetailActivity.this)
                        .setTitle("更改成功")
                        .setMessage("您的裝置訊息已成功更改")
                        .setPositiveButton("確定",null)
                        .show()//改選項顏色
                        .getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(BeaconDetailActivity.this,R.color.colorPrimary));

                backToDetail();
            }
        }
    };

    /** ================================================ */
    Button.OnClickListener cancelClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            //先恢復資料
            edt_name.setText(name);
            edt_detail.setText(detail);
            tV_mac.setText(mac);

            //再回到detail
            backToDetail();
        }
    };

    /** ================================================ */
    void backToDetail(){
        BtnLayout_detail.setVisibility(View.VISIBLE);
        BtnLayout_rename.setVisibility(View.GONE);
        btnSetDefault.setVisibility(View.VISIBLE);

        edt_name.setEnabled(false);
        edt_detail.setEnabled(false);
        setTitle("詳細資料");
    }

    /** ================================================ */
    Button.OnClickListener setDefaultClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            //先把其他裝置的預設取消
            if (cursor.getCount() > 0) {  //如果沒有資料則離開
                cursor.moveToLast(); //移至最後一筆，由後往前面讀取
                String id;
                do{
                    id =String.valueOf(cursor.getInt(0));
                    ContentValues cv = new ContentValues(1);
                    cv.put("defaul","false");
                    db.update("test", cv, "_id=?", new String[]{id});
                }while(cursor.moveToPrevious());
            }

            //把自己設為預設
            ContentValues cv = new ContentValues(1);
            cv.put("defaul","true");
            db.update("test", cv, "_id=?", new String[]{getId});

            btnSetDefault.setBackgroundResource(R.drawable.default_icon_style);
            btnSetDefault.setEnabled(false);
        }
    };

}
