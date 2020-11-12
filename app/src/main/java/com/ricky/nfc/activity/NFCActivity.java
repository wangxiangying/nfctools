package com.ricky.nfc.activity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Selection;
import android.text.Spannable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class NFCActivity extends Activity {
    private static final DateFormat TIME_FORMAT = SimpleDateFormat
            .getDateTimeInstance();
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private NdefMessage mNdefPushMessage;
    private Tag intenTag;
    private String NdefRecordTypeByte01 = "http://";
    private String NdefRecordTypeByte02 = "www.baidu.com";
    private String NdefRecordTypeByte03 = "?";
    private int NFCDataType = 0;
    private final int MifareClassicType = 1, MifareUltralightType = 2, OtherType = 3;
    //扇位置
    private short sectorAddress = 6;
    //修改密码
    private byte[] myKeyA = {'h', '9', '^', 'a', '-', '8'};
    private int NFCBarCodeLength = 16 * 3;
    public EditText NFCEditText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resolveIntent(getIntent());
        // 获取默认的NFC控制器
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        //拦截系统级的NFC扫描，例如扫描蓝牙
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        mNdefPushMessage = new NdefMessage(new NdefRecord[]{newTextRecord("",
                Locale.ENGLISH, true)});
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter != null) {
            //隐式启动
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
            mAdapter.enableForegroundNdefPush(this, mNdefPushMessage);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAdapter != null) {
            //隐式启动
            mAdapter.disableForegroundDispatch(this);
            mAdapter.disableForegroundNdefPush(this);
        }
    }

    //  //16进制字符串转换为String
    //  private String hexString = "0123456789ABCDEF";
    //  public String decode(String bytes) {
    //      if (bytes.length() != 30) {
    //          return null;
    //      }
    //      ByteArrayOutputStream baos = new ByteArrayOutputStream(
    //              bytes.length() / 2);
    //      // 将每2位16进制整数组装成一个字节
    //      for (int i = 0; i < bytes.length(); i += 2)
    //          baos.write((hexString.indexOf(bytes.charAt(i)) << 4 | hexString
    //                  .indexOf(bytes.charAt(i + 1))));
    //      return new String(baos.toByteArray());
    //  }
    //   //字符序列转换为16进制字符串
    //    private String bytesToHexString(byte[] src) {
    //        StringBuilder stringBuilder = new StringBuilder("0x");
    //        if (src == null || src.length <= 0) {
    //            return null;
    //        }
    //        char[] buffer = new char[2];
    //        for (int i = 0; i < src.length; i++) {
    //            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
    //            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
    //            System.out.println(buffer);
    //            stringBuilder.append(buffer);
    //        }
    //        return stringBuilder.toString();
    //    }

    //  // 字符序列转换为16进制字符串
    //  private String bytesToHexString(byte[] src, boolean isPrefix) {
    //      StringBuilder stringBuilder = new StringBuilder();
    //      if (isPrefix == true) {
    //          stringBuilder.append("0x");
    //      }
    //      if (src == null || src.length <= 0) {
    //          return null;
    //      }
    //      char[] buffer = new char[2];
    //      for (int i = 0; i < src.length; i++) {
    //          buffer[0] = Character.toUpperCase(Character.forDigit(
    //                  (src[i] >>> 4) & 0x0F, 16));
    //          buffer[1] = Character.toUpperCase(Character.forDigit(src[i] & 0x0F,
    //                  16));
    //          System.out.println(buffer);
    //          stringBuilder.append(buffer);
    //      }
    //      return stringBuilder.toString();
    //  }


    public String TAG = "nfc";


    private Boolean isKeyMifareClassicEnable(MifareClassic mfc, int sectorIndex) {
        boolean auth = false;
        try {
            auth = mfc.authenticateSectorWithKeyA(sectorIndex,
                    MifareClassic.KEY_DEFAULT);
            if (!auth) {
                auth = mfc.authenticateSectorWithKeyA(sectorIndex,
                        myKeyA);
            }
            if (!auth) {
                auth = mfc.authenticateSectorWithKeyA(sectorIndex,
                        MifareClassic.KEY_NFC_FORUM);
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException while authenticateSectorWithKey MifareClassic...", e);
        }
        return auth;
    }

    /**
     * 将数据写入扇，扇中每块必须16位
     *
     * @param mfc
     * @param barCodeByte
     * @param sectorIndex
     * @return
     */
    private Boolean writeMifareClassicBarCode(MifareClassic mfc, byte[] barCodeByte, int sectorIndex) {
        Boolean flag = false;
        int bCount = mfc.getBlockCountInSector(sectorIndex);
        //获取block起始编号
        int bIndex = mfc.sectorToBlock(sectorIndex);
        int barIndexCount = barCodeByte.length / 16 + 1;
        for (int i = 0; i < bCount - 1 && i < barIndexCount; i++) {
            byte[] bar = new byte[16];
            if (barCodeByte.length >= 0) {
                for (int j = 0; j < 16; j++) {
                    if (j + i * 16 < barCodeByte.length) {
                        bar[j] = barCodeByte[j + i * 16];
                    } else {
                        bar[j] = 0;
                    }
                }
            }
            try {
                mfc.writeBlock(bIndex, bar);
                bIndex++;
                if (i == bIndex - 2) {
                    flag = true;
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException while writeMifareClassicBarCode MifareClassic...", e);
            }
        }
        return flag;
    }



    private void showToastMessage(String message)
    {

    }

    /**
     * 写入数据的方法
     *
     * @param barCode
     * @param otherStr
     */
    protected void WriteMothed(String barCode, String otherStr) {
        byte[] data = null;
        if (otherStr != null) {
            data = otherStr.getBytes(Charset.forName("UTF-8"));
        }
        if (barCode == null || barCode.length() < 4 || barCode.getBytes().length > NFCBarCodeLength) {
            showToastMessage("要写入的数据不正确！");
            return;
        }
//        if (data.length > DataValues.NFCWriteLength) {
//            showToastMessage("非条码数据不超过" + DataValues.NFCWriteLength + "！");
//            return;
//        }
        if (intenTag != null) {
            switch (NFCDataType) {
                case MifareClassicType:
                    //MifareClassicl类型写入
                    MifareClassic mfc = MifareClassic.get(intenTag);
                    int otherStrIndex = 0;
                    try {
                        int bAllCount = 0;
                        if (barCode != null) {
                            bAllCount = 1;
                        }
                        if (data != null) {
                            bAllCount += data.length / 16 + 1;
                        }

                        for (int j = sectorAddress; j < 16 && j < bAllCount + sectorAddress; j++) {
                            //在删去1 块4、5 写入数据
                            mfc.connect();
                            boolean auth = false;
                            byte[] KeyValue = null;
                            if (isKeyMifareClassicEnable(mfc, j)) {
                                KeyValue = mfc.readBlock(4 * j + 3);
                            }
                            mfc.close();
                            mfc.connect();
                            if (isKeyMifareClassicEnable(mfc, j) && KeyValue != null) {
                                if (j == sectorAddress) {
                                    writeMifareClassicBarCode(mfc, barCode.getBytes(Charset.forName("UTF-8")), j);
                                } else {
                                    int count = 16 * 3;
                                    byte[] strByte = new byte[count];
                                    for (int z = 0; z < count; z++) {
                                        if (otherStrIndex < data.length) {
                                            strByte[z] = data[otherStrIndex];
                                            otherStrIndex++;
                                        } else {
                                            break;
                                        }
                                    }
                                    writeMifareClassicBarCode(mfc, strByte, j);
                                }
                                //修改键值，keyA存放在每扇最后一块
                                for (int i = 0; i < 6; i++) {
                                    KeyValue[i] = myKeyA[i];
                                }
                                mfc.writeBlock(4 * j + 3, KeyValue);
                                showToastMessage("写入数据成功！");
                                mfc.close();
                            } else {
                                showToastMessage("块密码不正确！");
                            }
                        }
                    } catch (Exception e) {
                        showToastMessage("写入数据失败！");
                        Log.e(TAG, "IOException while write MifareClassic...", e);
                    } finally {
                        try {
                            mfc.close();
                        } catch (IOException e) {
                            Log.e(TAG, "IOException while closing MifareClassic...", e);
                        }
                    }
                    break;
                case MifareUltralightType:
                    MifareUltralight ultralight = MifareUltralight.get(intenTag);
                    try {
                        ultralight.connect();
                        //每页4个字符2个汉字，共16页
                        ultralight.writePage(4, "abcd".getBytes(Charset.forName("US-ASCII")));
                        ultralight.writePage(5, "efgh".getBytes(Charset.forName("US-ASCII")));
                        ultralight.writePage(6, "ijkl".getBytes(Charset.forName("US-ASCII")));
                        ultralight.writePage(7, "mnop".getBytes(Charset.forName("US-ASCII")));
                        showToastMessage("写入数据成功！");
                    } catch (Exception e) {
                        showToastMessage("写入数据失败！");
                        Log.e(TAG, "IOException while write MifareUltralight...", e);
                    } finally {
                        try {
                            ultralight.close();
                        } catch (IOException e) {
                            Log.e(TAG, "IOException while closing MifareUltralight...", e);
                        }
                    }
                    break;
                case OtherType:
                    //新建NdefRecord数组，本例中数组只有一个元素
                    NdefRecord re = null;
                    try {
                        re = createRecord(otherStr + barCode);
                    } catch (Exception e) {
                        showToastMessage("新建NdefRecord数组失败！");
                        Log.e(TAG, "IOException while create other NdefRecord...", e);
                        return;
                    }
                    NdefRecord[] records = {re};
                    //新建一个NdefMessage实例
                    NdefMessage message = new NdefMessage(records);
                    // 解析TAG获取到NDEF实例
                    Ndef ndef = Ndef.get(intenTag);
                    if (ndef != null) {
                        // 打开连接
                        try {
                            ndef.connect();
                            ndef.writeNdefMessage(message);
                            showToastMessage("写入数据成功！");
                        } catch (Exception e) {
                            showToastMessage("写入数据失败！");
                            Log.e(TAG, "IOException while write other...", e);
                        } finally {
                            try {
                                ndef.close();
                            } catch (Exception e) {
                                Log.e(TAG, "IOException while closing other...", e);
                            }
                        }
                    }
                    break;
            }
        } else {
            showToastMessage("设备与nfc卡连接断开，请重新连接...");
        }
    }

    /**
     * 创建NdefRecord实例
     *
     * @param str
     * @return
     * @throws UnsupportedEncodingException
     */
    protected NdefRecord createRecord(String str) throws UnsupportedEncodingException {
        NdefRecord textRecord = NdefRecord.createMime(NdefRecordTypeByte01 +
                        NdefRecordTypeByte02 +
                        NdefRecordTypeByte03,
                str.getBytes(Charset.forName("US-ASCII")));
        //      //组装字符串，准备好你要写入的信息
        //      String msg = str;
        //      //将字符串转换成字节数组
        //      byte[] textBytes = msg.getBytes();
        //      //将字节数组封装到一个NdefRecord实例中去
        //      NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
        //              NdefRecordTypeByte.getBytes(), new byte[] {}, textBytes);
        return textRecord;
    }

    private NdefRecord newTextRecord(String text, Locale locale,
                                     boolean encodeInUtf8) {
        byte[] langBytes = locale.getLanguage().getBytes(
                Charset.forName("UTF-8"));
        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset
                .forName("UTF-16");
        byte[] textBytes = text.getBytes(utfEncoding);
        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length,
                textBytes.length);
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT,
                new byte[0], data);
    }

    //  private void showWirelessSettingsDialog() {
    //      AlertDialog.Builder builder = new AlertDialog.Builder(this);
    //      builder.setMessage("提示");
    //      builder.setPositiveButton(android.R.string.ok,
    //              new DialogInterface.OnClickListener() {
    //          public void onClick(DialogInterface dialogInterface, int i) {
    //              Intent intent = new Intent(
    //                      Settings.ACTION_WIRELESS_SETTINGS);
    //              startActivity(intent);
    //          }
    //      });
    //      builder.setNegativeButton(android.R.string.cancel,
    //              new DialogInterface.OnClickListener() {
    //          public void onClick(DialogInterface dialogInterface, int i) {
    //              finish();
    //          }
    //      });
    //      builder.create().show();
    //      return;
    //  }


    /**
     * 初步判断是什么类型NFC卡
     *
     * @param intent
     */
    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent
                    .getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs;
            intenTag = intent
                    .getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[0];
                byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
                byte[] payload = dumpTagData(intenTag).getBytes();
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN,
                        empty, id, payload);
                NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
                msgs = new NdefMessage[]{msg};
            }
            // Setup the views
            getNFCData(msgs);
        }
    }

    /**
     * 解析读取的数据
     *
     * @param p
     * @return
     */
    private String dumpTagData(Parcelable p) {
        StringBuilder sb = new StringBuilder();
        Tag tag = (Tag) p;
        //      String prefix = "android.nfc.tech.";
        //      sb.append("Technologies: ");
        //      for (String tech : tag.getTechList()) {
        //          sb.append(tech.substring(prefix.length()));
        //          sb.append(", ");
        //      }
        NFCDataType = 3;
        //      sb.delete(sb.length() - 2, sb.length());
        for (String tech : tag.getTechList()) {
            if (tech.equals(MifareClassic.class.getName())) {
                MifareClassic mifareTag = MifareClassic.get(tag);
                String type = "Unknown";
                switch (mifareTag.getType()) {
                    case MifareClassic.TYPE_CLASSIC:
                        type = "Classic";
                        break;
                    case MifareClassic.TYPE_PLUS:
                        type = "Plus";
                        break;
                    case MifareClassic.TYPE_PRO:
                        type = "Pro";
                        break;
                }
                NFCDataType = 1;
                try {
                    //获取 扇sectorAddress 块barCdeBlock 的内容
                    mifareTag.connect();
                    int bCount;
                    int bIndex;
                    if (isKeyMifareClassicEnable(mifareTag, sectorAddress)) {
                        // 读取扇区中的块总数
                        bCount = mifareTag.getBlockCountInSector(sectorAddress);
                        //获取扇区块的起始编号
                        bIndex = mifareTag.sectorToBlock(sectorAddress);
                        for (int i = 0; i < bCount - 1; i++) {
                            byte[] data = mifareTag.readBlock(bIndex);
                            sb.append(new String(data, Charset.forName("UTF-8")));
                            bIndex++;
                        }
                    } else {
                        showToastMessage("验证失败");
                    }
                } catch (Exception e) {
                    showToastMessage("读取数据失败！");
                    Log.e(TAG, "IOException while read MifareClassic...", e);
                } finally {
                    try {
                        mifareTag.close();
                    } catch (IOException e) {
                        Log.e(TAG, "IOException while closing MifareClassic...", e);
                    }
                }
            }

            if (tech.equals(MifareUltralight.class.getName())) {
                sb.append('\n');
                MifareUltralight mifareUlTag = MifareUltralight.get(tag);
                String type = "Unknown";
                switch (mifareUlTag.getType()) {
                    case MifareUltralight.TYPE_ULTRALIGHT:
                        type = "Ultralight";
                        break;
                    case MifareUltralight.TYPE_ULTRALIGHT_C:
                        type = "Ultralight C";
                        break;
                }
                NFCDataType = 2;
                sb.append("Mifare Ultralight type: ");
                sb.append(type);
            }
        }
        return sb.toString();
    }

    //  private String getHex(byte[] bytes) {
    //      StringBuilder sb = new StringBuilder();
    //      for (int i = bytes.length - 1; i >= 0; --i) {
    //          int b = bytes[i] & 0xff;
    //          if (b < 0x10)
    //              sb.append('0');
    //          sb.append(Integer.toHexString(b));
    //          if (i > 0) {
    //              sb.append(" ");
    //          }
    //      }
    //      return sb.toString();
    //  }
    //  private long getDec(byte[] bytes) {
    //      long result = 0;
    //      long factor = 1;
    //      for (int i = 0; i < bytes.length; ++i) {
    //          long value = bytes[i] & 0xffl;
    //          result += value * factor;
    //          factor *= 256l;
    //      }
    //      return result;
    //  }
    //  private long getReversed(byte[] bytes) {
    //      long result = 0;
    //      long factor = 1;
    //      for (int i = bytes.length - 1; i >= 0; --i) {
    //          long value = bytes[i] & 0xffl;
    //          result += value * factor;
    //          factor *= 256l;
    //      }
    //      return result;
    //  }

    /**
     * 取出NFC扫描的数据
     *
     * @param msgs
     */
    private void getNFCData(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) {
            return;
        }
        //将字节数组转换成字符串
        String str = "";
        byte[] data = null;
        for (NdefMessage obj : msgs) {
            NdefRecord record[] = obj.getRecords();
            for (NdefRecord recordObj : record) {
                if (data == null) {
                    data = recordObj.getPayload();
                } else {
                    byte[] oldData = data;
                    data = new byte[oldData.length + recordObj.getPayload().length];
                    for (int i = 0; i < oldData.length; i++) {
                        data[i] = oldData[i];
                    }
                    for (int i = 0; i < recordObj.getPayload().length; i++) {
                        data[oldData.length + i] = recordObj.getPayload()[i];
                    }
                }
            }
        }
        if (data != null) {
            str = new String(data, Charset.forName("UTF-8"));
        }
        //截取部分字符findNumberFrom（）方法，并展示
        NFCEditTextEvent(str);
    }

    /**
     * 用NFCEditText展示数据，并触发keyEvent
     *
     * @param str
     */
    public void NFCEditTextEvent(String str) {
        if (NFCEditText != null) {
            if (str != null && str.length() > 3) {
                NFCEditText.setText(str);
                CharSequence text = NFCEditText.getText();
                if (text instanceof Spannable) {
                    Spannable spanText = (Spannable) text;
                    Selection.setSelection(spanText, text.length());
                }
                if (NFCEditText.getText().toString().trim().length() > 3) {
                    KeyEvent event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER);
                    NFCEditText.dispatchKeyEvent(event);
                }
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            /*隐藏软键盘*/
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager.isActive()) {
                inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    //获取系统隐式启动的
    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        resolveIntent(intent);
    }
}