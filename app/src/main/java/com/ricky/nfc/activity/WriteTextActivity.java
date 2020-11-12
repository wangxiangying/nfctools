package com.ricky.nfc.activity;

import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Base64;
import android.widget.TextView;
import android.widget.Toast;

import com.ricky.nfc.R;
import com.ricky.nfc.base.BaseNfcActivity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


public class WriteTextActivity extends BaseNfcActivity {
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_text);
        textView = (TextView) findViewById(R.id.text1);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onNewIntent(final Intent intent) {


        Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        NdefMessage ndefMessage = new NdefMessage(createTextRecord());

        TextView text0 = (TextView) findViewById(R.id.text0);

        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < ndefMessage.getRecords().length; i++) {
            sb.append(ndefMessage.getRecords()[i].toString()).append("\n");
        }

        text0.setText(sb.toString());

        writeTag(ndefMessage, detectedTag);

    }


    public static String code0 = "HL00761769510001";
    public static String code1 = "HL00761769510002";
    public static String code2 = "HL00761769510003";
    public static String code3 = "0076";
    public static String[] datas = new String[]{
            code0, code1, code2, code3
    };

    public static String currentCode = datas[1];

    /**
     * 创建NDEF文本数据
     *
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static NdefRecord[] createTextRecord() {
        byte[] TYPE_HW = {0x68, 0x77};

        byte[] bytes = Base64.decode("gHwyMDB8MUVDQ0QzMTk5NzRFOThBNzc1MDdGQTc" +
                        "1RDRBN0RCMDU2ODBBREE2NDQwOEZF" +
                        "NzVFNkFBQzBBNTgzODQ4Nzg3Mnw0NzMwOTg4" +
                        "MTU0MTU1MTcxODR8aHR0cHM6Ly9oc2JweS5" +
                        "odWFsYWxhLmNvbS9odWF3ZWl8dGV4dC9wbGFpbnw=",
                Base64.NO_WRAP);

        String[] slplts = new String(bytes, StandardCharsets.UTF_8).split("\\|", -1);

        return
                new NdefRecord[]
                        {

//                                https://hsbpy.hualala.com/groupId_1/shopId_1?nfcCode=1234


                                NdefRecord.createUri("https://hsbpy.hualala.com/groupId_1/shopId_1?nfcCode=" + currentCode),


                                NdefRecord.createUri("https://hsbpy.hualala.com/huawei"),


                                new NdefRecord(NdefRecord.TNF_MIME_MEDIA, TYPE_HW, new byte[0], bytes),


                                NdefRecord.createApplicationRecord("com.huawei.hwid"),

                                NdefRecord.createApplicationRecord("com.huawei.hms"),

                                new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "text/plain".getBytes(StandardCharsets.UTF_8), new byte[0], ("code=" + currentCode).getBytes(StandardCharsets.UTF_8)),
                                NdefRecord.createUri("https://hsbpy.hualala.com/app"),
                        };

    }

    public boolean writeTag(NdefMessage ndefMessage, Tag tag) {
        int size = ndefMessage.toByteArray().length;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                textView.setText("现有：" + ndef.getMaxSize() + " 需要 " + size);
                if (!ndef.isWritable()) {
                    textView.setText("NFC Tag是只读的！");
                    Toast.makeText(this, "NFC Tag是只读的！", Toast.LENGTH_LONG)
                            .show();
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    textView.setText("现有：" + ndef.getMaxSize() + " 需要 " + size + "    NFC Tag的空间不足！");
                    Toast.makeText(this, "现有：" + ndef.getMaxSize() + " 需要 " + size + "    NFC Tag的空间不足！", Toast.LENGTH_LONG)
                            .show();
                    return false;
                }
                ndef.writeNdefMessage(ndefMessage);
                Toast.makeText(this, "已成功写入数据！", Toast.LENGTH_LONG).show();
                textView.setText("已成功写入数据！");
                return true;

            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(ndefMessage);
                        Toast.makeText(this, "已成功写入数据！", Toast.LENGTH_LONG)
                                .show();

                        textView.setText("已成功写入数据！");

                        return true;

                    } catch (Exception e) {
                        Toast.makeText(this, "写入NDEF格式数据失败！", Toast.LENGTH_LONG)
                                .show();

                        textView.setText("写入NDEF格式数据失败！");
                        return false;

                    }
                } else {
                    Toast.makeText(this, "NFC标签不支持NDEF格式！", Toast.LENGTH_LONG)
                            .show();
                    textView.setText("NFC标签不支持NDEF格式！");
                    return false;
                }
            }
        } catch (FormatException ex) {
            ex.printStackTrace();
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();

            textView.setText(ex.getMessage());
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            textView.setText(ex.getMessage());
            return false;
        }

    }
}
