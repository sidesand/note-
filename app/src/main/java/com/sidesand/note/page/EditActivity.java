package com.sidesand.note.page;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.sidesand.note.R;
import com.sidesand.note.base.CustomSpinnerAdapter;
import com.sidesand.note.entriy.NoteDBHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class EditActivity extends BaseActivity {
    private EditText etContent;
    private EditText etTitle;

    private String old_content = "";
    private String old_tag_name = "";
    private String old_title;
    private long note_id = 0;
    private int openMode = 0;// 4:新建 3:打开已存在的
    private boolean tagChange = false;
    private NoteDBHelper mNoteHelper;
    private Spinner mySpinner;
    private List<String> tagList;
    private CustomSpinnerAdapter myAdapter;
    private String tag_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_layout);
        mNoteHelper = NoteDBHelper.getInstance(this);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(true);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mySpinner = (Spinner) findViewById(R.id.spinner);


        if (isNightMode())
            myToolbar.setNavigationIcon(getDrawable(R.drawable.ic_keyboard_arrow_left_white_24dp));
        else myToolbar.setNavigationIcon(getDrawable(R.drawable.ic_keyboard_arrow_left_black_24dp));


        // 点击返回按钮
        myToolbar.setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(RESULT_OK, checkNote(intent));
                finish();//返回
                overridePendingTransition(R.anim.in_lefttoright, R.anim.out_lefttoright);
            }
        });

        etContent = (EditText) findViewById(R.id.et_content);
        etTitle = (EditText) findViewById(R.id.et_title);

        Intent getIntent = getIntent();

        openMode = getIntent.getIntExtra("mode", 0);
        if (openMode == 3) {        // 打开已存在的note
            note_id = getIntent.getLongExtra("note_id", 0);
            old_content = getIntent.getStringExtra("content");
            old_title = getIntent.getStringExtra("title");
//            old_time = getIntent.getStringExtra("time");
            old_tag_name = getIntent.getStringExtra("tag_name");
//            Log.d(TAG, "编辑标签为:" + old_tag_name);
            etContent.setText(old_content);
            etTitle.setText(old_title);
            etContent.setSelection(old_content.length());
        } else if (openMode == 5) { // 搜索状态的note
            note_id = getIntent.getLongExtra("note_id", 0);
            old_title = getIntent.getStringExtra("title");
            old_content = getIntent.getStringExtra("content");
            old_tag_name = getIntent.getStringExtra("tag_name");
            String searchQuery = getIntent.getStringExtra("searchQuery"); //searchQuery
//            old_time = getIntent.getStringExtra("time");
//            Log.d(TAG, "编辑标签为:" + old_tag_name);
            etContent.setText(old_content);
            etTitle.setText(old_title);
            etContent.setSelection(old_content.length());

            // 高亮显示搜索词
            highlightSearchTerm(etContent, searchQuery);
        }

        refreshSpinner(this, old_tag_name);
        tag_name = mySpinner.getSelectedItem().toString();
        mySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tag_name = (String) parent.getItemAtPosition(position);
                tagChange = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

    }

    private void highlightSearchTerm(EditText editText, String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            Log.d(TAG, "Search term is empty or null.");
            return;
        }

        Log.d(TAG, "Highlighting search term: " + searchTerm);

        SpannableString spannableString = new SpannableString(editText.getText());
        int index = 0;

        while ((index = spannableString.toString().toLowerCase().indexOf(searchTerm.toLowerCase(), index)) != -1) {
            Log.d(TAG, "Found match at index: " + index);
            ForegroundColorSpan span = new ForegroundColorSpan(Color.RED); // 设置高亮颜色
            spannableString.setSpan(span, index, index + searchTerm.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            index += searchTerm.length(); // 继续查找下一个匹配项
        }

        editText.setText(spannableString);
    }


    private void refreshSpinner(EditActivity context, String tag_name) {
        // 刷新标签列表
        tagList = mNoteHelper.getAllTags();
        if (tagList != null) {
            tagList.removeIf(tag -> tag == null || tag.isEmpty()); // 移除空标签和 null 值
        } else {
            tagList = new ArrayList<>(); // 如果 tagList 为 null，初始化为空列表
        }

        // 添加默认标签“未分类”如果它不在列表中
        if (!tagList.contains("未分类")) {
            tagList.add("未分类");
        }

        myAdapter = new CustomSpinnerAdapter(context, R.layout.spinner_item, tagList);
        myAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        mySpinner.setAdapter(myAdapter);

        // 设置默认选中项
        if (tag_name == null || tag_name.isEmpty()) {
            tag_name = "未分类";
        }

//        Log.d(TAG, "设置spinner标签" + tag_name);
        int spinnerPosition = myAdapter.getPosition(tag_name);
        if (spinnerPosition >= 0) {
            mySpinner.setSelection(spinnerPosition);
        } else {
            mySpinner.setSelection(myAdapter.getPosition("未分类")); // 默认选中“未分类”
        }
    }

    @Override
    protected void needRefresh() {
        setNightMode();
        startActivity(new Intent(this, EditActivity.class));
        overridePendingTransition(R.anim.night_switch, R.anim.night_switch_over);
        finish();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            Intent intent = new Intent();
            setResult(RESULT_OK, checkNote(intent));
            finish();
            overridePendingTransition(R.anim.in_lefttoright, R.anim.out_lefttoright);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private Intent checkNote(Intent intent) {
//        Log.d(TAG, "checkNote:前期id= " + note_id);
        if (openMode == 4) {// 新建模式
            if (etContent.getText().toString().isEmpty()) {
                intent.putExtra("mode", -1); //未创建内容
            } else {
                intent.putExtra("mode", 0); // new one note;
                intent.putExtra("title", etTitle.getText().toString());
                intent.putExtra("content", etContent.getText().toString());
                intent.putExtra("create_time", dateToStr());
                intent.putExtra("tag_name", tag_name);
//                Log.d(TAG, "newNote: " + "title: " + etTitle.getText().toString() + ",content: " + etContent.getText().toString());
//                Log.d(TAG, "新建Note: " + "tag_name = " + tagName);

            }
        } else {//openMode == 3,编辑模式
            if (etContent.getText().toString().equals(old_content) && etTitle.getText().toString().equals(old_title) && !tagChange)
                intent.putExtra("mode", -1); // edit nothing
            else {
                intent.putExtra("mode", 1); //edit the content 笔记内容发生变化
                intent.putExtra("note_id", note_id);
                intent.putExtra("title", etTitle.getText().toString());
                intent.putExtra("content", etContent.getText().toString());
                intent.putExtra("update_time", dateToStr());
                intent.putExtra("tag_name", tag_name);
//                Log.d(TAG, "更新Note: " + "tag_name = " + tagName);
//                Log.d(TAG, "updateNote: " + "note_id: " + note_id + "，title: " + etTitle.getText().toString() + "，content: " + etContent.getText().toString());
            }
        }
        return intent;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_menu, menu);
        MenuItem delete = menu.findItem(R.id.delete);
        MenuItem save = menu.findItem(R.id.menu_save);
        MenuItem undo = menu.findItem(R.id.menu_undo);
        MenuItem redo = menu.findItem(R.id.menu_redo);

        delete.setVisible(true);
        save.setVisible(true);
        undo.setVisible(true);
        redo.setVisible(true);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Intent intent = new Intent();
        if (item.getItemId() == R.id.delete) {
            new AlertDialog.Builder(EditActivity.this)
                    .setMessage("Delete this Note ?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (openMode == 4) {// 新建模式下删除，直接返回
                                intent.putExtra("mode", -1); // delete the note
                                setResult(RESULT_OK, intent);
                            } else {// 编辑模式下，传递id进行删除
                                intent.putExtra("mode", 2); // delete the note
                                intent.putExtra("note_id", note_id);
                                setResult(RESULT_OK, intent);
                            }
                            finish();
                        }
                    }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();
        } else if (item.getItemId() == R.id.menu_save) {
            setResult(RESULT_OK, checkNote(intent));
            finish();
        } else if (item.getItemId() == R.id.menu_undo) {

        } else if (item.getItemId() == R.id.menu_redo) {

        }
        return super.onOptionsItemSelected(item);
    }

    public String dateToStr() {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDateFormat.format(date);
    }

}
