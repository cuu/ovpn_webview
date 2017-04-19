/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.app.AlertDialog
 *  android.app.AlertDialog$Builder
 *  android.app.ListActivity
 *  android.content.Context
 *  android.content.DialogInterface
 *  android.content.DialogInterface$OnClickListener
 *  android.content.Intent
 *  android.os.Bundle
 *  android.os.IBinder
 *  android.text.Editable
 *  android.view.KeyEvent
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.view.inputmethod.InputMethodManager
 *  android.widget.Button
 *  android.widget.EditText
 *  android.widget.LinearLayout
 *  android.widget.ListAdapter
 *  android.widget.ListView
 *  android.widget.SimpleAdapter
 *  android.widget.TextView
 */
package net.openvpn.openvpn;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class FileDialog extends ListActivity {
    public static final String CAN_SELECT_DIR = "CAN_SELECT_DIR";
    public static final String FORMAT_FILTER = "FORMAT_FILTER";
    private static final String ITEM_IMAGE = "image";
    private static final String ITEM_KEY = "key";
    public static final int MODE_CREATE = 0;
    public static final int MODE_OPEN = 1;
    public static final String OPTION_CURRENT_PATH_IN_TITLEBAR = "OPTION_CURRENT_PATH_IN_TITLEBAR";
    public static final String OPTION_ONE_CLICK_SELECT = "OPTION_ONE_CLICK_SELECT";
    public static final String OPTION_PROMPT = "OPTION_PROMPT";
    public static final String RESULT_PATH = "RESULT_PATH";
    private static final String ROOT = "/";
    public static final String SELECTION_MODE = "SELECTION_MODE";
    public static final String START_PATH = "START_PATH";
    private boolean canSelectDir = false;
    private String currentPath = "/";
    private String[] formatFilter = null;
    private InputMethodManager inputManager;
    private HashMap<String, Integer> lastPositions = new HashMap();
    private LinearLayout layoutCreate;
    private LinearLayout layoutSelect;
    private EditText mFileName;
    private ArrayList<HashMap<String, Object>> mList;
    private boolean m_bOneClickSelect = false;
    private boolean m_bTitlebarFolder = false;
    private TextView myPath;
    private TextView myPrompt;
    private String parentPath;
    private List<String> path = null;
    private Button selectButton;
    private File selectedFile;
    private int selectionMode = 0;

    private void addItem(String string2, int n) {
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put("key", string2);
        hashMap.put("image", n);
        this.mList.add(hashMap);
    }

    private void getDir(String string2) {
        boolean bl = string2.length() < this.currentPath.length();
        Integer n = this.lastPositions.get(this.parentPath);
        this.getDirImpl(string2);
        if (n == null) return;
        if (!bl) return;
        this.getListView().setSelection(n.intValue());
    }

    private void getDirImpl(String dirPath) {
        this.currentPath = dirPath;
        List<String> item = new ArrayList();
        this.path = new ArrayList();
        this.mList = new ArrayList();
        File f = new File(this.currentPath);
        File[] files = f.listFiles();
        if (files == null) {
            this.currentPath = ROOT;
            f = new File(this.currentPath);
            files = f.listFiles();
        }
        showLocation(R.string.file_dialog_location, this.currentPath);
        if (!this.currentPath.equals(ROOT)) {
            item.add(ROOT);
            addItem(ROOT, R.drawable.file_dialog_folder);
            this.path.add(ROOT);
            item.add("../");
            addItem("../", R.drawable.file_dialog_folder);
            this.path.add(f.getParent());
            this.parentPath = f.getParent();
        }
        TreeMap<String, String> dirsMap = new TreeMap();
        TreeMap<String, String> dirsPathMap = new TreeMap();
        TreeMap<String, String> filesMap = new TreeMap();
        TreeMap<String, String> filesPathMap = new TreeMap();
        File[] arr$ = files;
        int len$ = arr$.length;
        for (int i$ = MODE_CREATE; i$ < len$; i$ += MODE_OPEN) {
            File file = arr$[i$];
            if (file.isDirectory()) {
                String dirName = file.getName();
                dirsMap.put(dirName, dirName);
                dirsPathMap.put(dirName, file.getPath());
            } else {
                String fileName = file.getName();
                String fileNameLwr = fileName.toLowerCase();
                if (this.formatFilter != null) {
                    boolean contains = false;
                    for (int i = MODE_CREATE; i < this.formatFilter.length; i += MODE_OPEN) {
                        if (fileNameLwr.endsWith(this.formatFilter[i].toLowerCase())) {
                            contains = true;
                            break;
                        }
                    }
                    if (contains) {
                        filesMap.put(fileName, fileName);
                        filesPathMap.put(fileName, file.getPath());
                    }
                } else {
                    filesMap.put(fileName, fileName);
                    filesPathMap.put(fileName, file.getPath());
                }
            }
        }
        item.addAll(dirsMap.tailMap("").values());
        List<String> list = item;
        list.addAll(filesMap.tailMap("").values());
        this.path.addAll(dirsPathMap.tailMap("").values());
        this.path.addAll(filesPathMap.tailMap("").values());
        SimpleAdapter fileList = new SimpleAdapter(this, this.mList, R.layout.file_dialog_row, new String[]{ITEM_KEY, ITEM_IMAGE}, new int[]{R.id.fdrowtext, R.id.fdrowimage});
        for (String dir : dirsMap.tailMap("").values()) {
            addItem(dir, R.drawable.file_dialog_folder);
        }
        for (String addItem : filesMap.tailMap("").values()) {
            addItem(addItem, R.drawable.file_dialog_file);
        }
        fileList.notifyDataSetChanged();
        setListAdapter(fileList);
    }

    private void setCreateVisible(View view) {
        this.layoutCreate.setVisibility(0);
        this.layoutSelect.setVisibility(8);
        this.inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        this.selectButton.setEnabled(false);
    }

    private void setSelectVisible(View view) {
        if (this.m_bOneClickSelect) {
            return;
        }
        this.layoutCreate.setVisibility(8);
        this.layoutSelect.setVisibility(0);
        this.inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        this.selectButton.setEnabled(false);
    }

    private void showLocation(int n, String string2) {
        if (this.m_bTitlebarFolder) {
            this.setTitle((CharSequence)string2);
            return;
        }
        this.myPath.setText((CharSequence)(this.getText(n) + ": " + string2));
    }

    public void onCreate(Bundle bundle) {
        String string2;
        super.onCreate(bundle);
        this.setResult(0, this.getIntent());
        this.setContentView(R.layout.file_dialog_main);
        this.myPath = (TextView)this.findViewById(R.id.fdPath);
        this.myPrompt = (TextView)this.findViewById(R.id.fdPrompt);
        this.mFileName = (EditText)this.findViewById(R.id.fdEditTextFile);
        this.m_bOneClickSelect = this.getIntent().getBooleanExtra("OPTION_ONE_CLICK_SELECT", this.m_bOneClickSelect);
        this.m_bTitlebarFolder = this.getIntent().getBooleanExtra("OPTION_CURRENT_PATH_IN_TITLEBAR", this.m_bTitlebarFolder);
        if (this.m_bTitlebarFolder) {
            this.myPath.setVisibility(8);
        }
        if ((string2 = this.getIntent().getStringExtra("OPTION_PROMPT")) != null) {
            this.myPrompt.setText((CharSequence)string2);
            this.myPrompt.setVisibility(0);
        } else {
            this.myPrompt.setVisibility(8);
        }
        this.inputManager = (InputMethodManager)this.getSystemService("input_method");
        this.selectButton = (Button)this.findViewById(R.id.fdButtonSelect);
        this.selectButton.setEnabled(false);
        this.selectButton.setOnClickListener(new View.OnClickListener(){

            public void onClick(View view) {
                if (FileDialog.this.selectedFile == null) return;
                FileDialog.this.getIntent().putExtra("RESULT_PATH", FileDialog.this.selectedFile.getPath());
                FileDialog.this.setResult(-1, FileDialog.this.getIntent());
                FileDialog.this.finish();
            }
        });
        Button button = (Button)this.findViewById(R.id.fdButtonNew);
        button.setOnClickListener(new View.OnClickListener(){

            public void onClick(View view) {
                FileDialog.this.setCreateVisible(view);
                FileDialog.this.mFileName.setText((CharSequence)"");
                FileDialog.this.mFileName.requestFocus();
            }
        });
        this.selectionMode = this.getIntent().getIntExtra("SELECTION_MODE", 0);
        this.formatFilter = this.getIntent().getStringArrayExtra("FORMAT_FILTER");
        this.canSelectDir = this.getIntent().getBooleanExtra("CAN_SELECT_DIR", false);
        if (this.selectionMode == 1) {
            button.setVisibility(8);
        }
        this.layoutSelect = (LinearLayout)this.findViewById(R.id.fdLinearLayoutSelect);
        this.layoutCreate = (LinearLayout)this.findViewById(R.id.fdLinearLayoutCreate);
        this.layoutCreate.setVisibility(8);
        if (this.selectionMode == 1 && this.m_bOneClickSelect) {
            this.layoutSelect.setVisibility(8);
        }
        ((Button)this.findViewById(R.id.fdButtonCancel)).setOnClickListener(new View.OnClickListener(){

            public void onClick(View view) {
                FileDialog.this.setResult(0, FileDialog.this.getIntent());
                FileDialog.this.finish();
            }
        });
        ((Button)this.findViewById(R.id.fdButtonCreate)).setOnClickListener(new View.OnClickListener(){

            public void onClick(View view) {
                if (FileDialog.this.mFileName.getText().length() <= 0) return;
                FileDialog.this.getIntent().putExtra("RESULT_PATH", FileDialog.this.currentPath + "/" + (Object)FileDialog.this.mFileName.getText());
                FileDialog.this.setResult(-1, FileDialog.this.getIntent());
                FileDialog.this.finish();
            }
        });
        String string3 = this.getIntent().getStringExtra("START_PATH");
        if (string3 == null) {
            string3 = "/";
        }
        if (this.canSelectDir) {
            this.selectedFile = new File(string3);
            this.selectButton.setEnabled(true);
        }
        this.getDir(string3);
    }

    public boolean onKeyDown(int n, KeyEvent keyEvent) {
        if (n != 4) return super.onKeyDown(n, keyEvent);
        this.selectButton.setEnabled(false);
        if (this.layoutCreate.getVisibility() == 0) {
            this.layoutCreate.setVisibility(8);
            this.layoutSelect.setVisibility(0);
            return true;
        }
        if (this.currentPath.equals("/")) return super.onKeyDown(n, keyEvent);
        this.getDir(this.parentPath);
        return true;
    }

    protected void onListItemClick(ListView listView, View view, int n, long l) {
        File file = new File(this.path.get(n));
        this.setSelectVisible(view);
        if (!file.isDirectory()) {
            this.selectedFile = file;
            view.setSelected(true);
            this.selectButton.setEnabled(true);
            this.showLocation(R.string.file_dialog_select, file.getPath());
            if (!this.m_bOneClickSelect) return;
            this.selectButton.performClick();
            return;
        }
        this.selectButton.setEnabled(false);
        if (file.canRead()) {
            this.lastPositions.put(this.currentPath, n);
            this.getDir(this.path.get(n));
            if (!this.canSelectDir) return;
            this.selectedFile = file;
            view.setSelected(true);
            this.selectButton.setEnabled(true);
            return;
        }
        new AlertDialog.Builder((Context)this).setIcon(R.drawable.error).setTitle((CharSequence)("[" + file.getName() + "] " + this.getText(R.string.file_dialog_cant_read_folder))).setPositiveButton((CharSequence)"OK", new DialogInterface.OnClickListener(){

            public void onClick(DialogInterface dialogInterface, int n) {
            }
        }).show();
    }

}

