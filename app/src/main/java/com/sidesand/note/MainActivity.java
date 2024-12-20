package com.sidesand.note;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sidesand.note.base.NoteAdapter;
import com.sidesand.note.entriy.Note;
import com.sidesand.note.entriy.NoteDBHelper;
import com.sidesand.note.page.BaseActivity;
import com.sidesand.note.page.EditActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity {

    private List<Note> noteList;  // 存储笔记的列表

    //    private Toolbar myToolbar;
    private TextView mEmptyView;
    private FloatingActionButton fab;
    private NoteAdapter noteAdapter;
    private NoteDBHelper mNoteHelper;
    private Toolbar myToolbar;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch content_switch;

    String[] list_String = {"before one month", "before three months", "before six months", "before one year"};
    private RecyclerView recyclerView;

    // 搜索词
    private String searchQuery = "";

    // 在类的成员变量中定义 Handler 和 Executor
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Executor executor = Executors.newSingleThreadExecutor();


/*    private DisplayMetrics metrics;
    private PopupWindow popupWindow; // 左侧弹出菜单
    private PopupWindow popupCover; // 菜单蒙版
    private ViewGroup coverView;
    private ViewGroup customView;*/


    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mNoteHelper = NoteDBHelper.getInstance(this);
        mNoteHelper.openReadLink();
        mNoteHelper.openWriteLink();

        initView();

        setSupportActionBar(myToolbar);
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //设置toolbar取代actionbar

        if (super.isNightMode()) {
            myToolbar.setNavigationIcon(getDrawable(R.drawable.ic_menu_white_24dp));
        } else {
            myToolbar.setNavigationIcon(getDrawable(R.drawable.ic_menu_black_24dp)); // 三道杠
        }

        myToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                showPopUpWindow();
            }
        });


    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();// 定义静态常量来存储 R.id.menu_clear 的值
        if (itemId == R.id.menu_clear) {
            if (!content_switch.isChecked()) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("Delete All Notes ?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                // 开启一个事务
                                SQLiteDatabase db = mNoteHelper.getWritableDatabase();
                                try {
                                    db.beginTransaction();

                                    // 删除所有笔记
                                    mNoteHelper.deleteAll();

                                    // 重置自增 ID
                                    db.execSQL("UPDATE sqlite_sequence SET seq=0 WHERE name='" + NoteDBHelper.TABLE_NOTE + "'");

                                    db.setTransactionSuccessful(); // 设置事务成功
                                } catch (Exception e) {
                                    Log.e(TAG, "删除所有notes错误: " + e.getMessage(), e);
                                } finally {
                                    db.endTransaction(); // 结束事务
                                }
                                refreshListView();
                            }
                        }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
            } else {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("Delete All Plans ?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                   /* planDbHelper = new PlanDatabase(context);
                                    SQLiteDatabase db = planDbHelper.getWritableDatabase();
                                    db.delete("plans", null, null);//delete data in table NOTES
                                    db.execSQL("update sqlite_sequence set seq=0 where name='plans'"); //reset id to 1*/
                                refreshListView();
                            }
                        }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
            }
        } else if (itemId == R.id.refresh) {
            myToolbar.setTitle("All Notes");
            recyclerView.setAdapter(noteAdapter);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        //search setting
        MenuItem mSearch = menu.findItem(R.id.action_search);
        SearchView mSearchView = (SearchView) mSearch.getActionView();

        Objects.requireNonNull(mSearchView).setQueryHint("Search");
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            private final Handler handler = new Handler();
            private final Runnable searchRunnable = () -> {
                if (!searchQuery.isEmpty()) {
                    List<Note> matchedNotes = mNoteHelper.searchNotesByTitleOrContent(searchQuery);
                    noteAdapter.updateNotes(matchedNotes);
                    recyclerView.scrollToPosition(0);
                }
            };

            @Override
            public boolean onQueryTextSubmit(String query) {
                searchQuery = query;
                handler.removeCallbacks(searchRunnable);
                handler.postDelayed(searchRunnable, 500); // 延迟 500ms 执行搜索
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    loadAllNotes();
                }
                return false;
            }
        });

        mSearchView.setOnCloseListener(() -> {
            // 当 SearchView 关闭时，重新加载所有笔记数据
            loadAllNotes();
            searchQuery = "";
            return false;
        });

        final int mode = (content_switch.isChecked() ? 2 : 1);
        final String itemName = (mode == 1 ? "notes" : "plans");
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                final View view = findViewById(R.id.menu_clear);

                if (view != null) {
                    view.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle("Delete all " + itemName);
                            builder.setIcon(R.drawable.ic_error_outline_black_24dp);
                            builder.setItems(list_String, new DialogInterface.OnClickListener() {//列表对话框；
                                @Override
                                public void onClick(DialogInterface dialog, final int which) {//根据这里which值，即可以指定是点击哪一个Item；
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setMessage("Do you want to delete all " + itemName + " " + list_String[which] + "? ")
                                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int a) {
                                                    Log.d(TAG, "onClick: " + which);
                                                    removeSelectItems(which, mode);
                                                    refreshListView();
                                                }

                                                //根据模式与时长删除对顶的计划s/笔记s
                                                @SuppressLint("Range")
                                                private void removeSelectItems(int which, int mode) {
                                                    int monthNum = 0;
                                                    switch (which) {
                                                        case 0:
                                                            monthNum = 1;
                                                            break;
                                                        case 1:
                                                            monthNum = 3;
                                                            break;
                                                        case 2:
                                                            monthNum = 6;
                                                            break;
                                                        case 3:
                                                            monthNum = 12;
                                                            break;
                                                    }
                                                    Calendar rightNow = Calendar.getInstance();
                                                    rightNow.add(Calendar.MONTH, -monthNum);//日期加3个月
                                                    Date selectDate = rightNow.getTime();
                                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                                    String selectDateStr = simpleDateFormat.format(selectDate);
                                                    Log.d(TAG, "removeSelectItems: " + selectDateStr);
                                                    switch (mode) {
                                                        case 1: //notes
                                                            // 删除 selectDateStr 对应的笔记
                                                            SQLiteDatabase db = mNoteHelper.getWritableDatabase();

                                                            try {
                                                                db.beginTransaction(); // 开始事务

                                                                // 查询所有 create_time 小于 selectDateStr 的笔记 ID
                                                                String query = "SELECT " + NoteDBHelper.NOTE_ID + " FROM " + NoteDBHelper.TABLE_NOTE +
                                                                        " WHERE " + NoteDBHelper.CREATE_TIME + " < ?";
                                                                Cursor cursor = db.rawQuery(query, new String[]{selectDateStr});

                                                                List<Long> noteIdsToDelete = new ArrayList<>();
                                                                while (cursor.moveToNext()) {
                                                                    long noteId = cursor.getLong(cursor.getColumnIndex(NoteDBHelper.NOTE_ID));
                                                                    noteIdsToDelete.add(noteId);
                                                                }
                                                                cursor.close(); // 关闭游标

                                                                // 构建 IN 子句参数
                                                                StringBuilder placeholders = new StringBuilder();
                                                                for (int i = 0; i < noteIdsToDelete.size(); i++) {
                                                                    placeholders.append("?");
                                                                    if (i < noteIdsToDelete.size() - 1) {
                                                                        placeholders.append(",");
                                                                    }
                                                                }

                                                                // 执行批量删除
                                                                String deleteQuery = "DELETE FROM " + NoteDBHelper.TABLE_NOTE +
                                                                        " WHERE " + NoteDBHelper.NOTE_ID + " IN (" + placeholders + ")";
                                                                db.execSQL(deleteQuery, noteIdsToDelete.toArray(new Long[0]));

                                                                // 重置自增 ID
                                                                db.execSQL("UPDATE sqlite_sequence SET seq=0 WHERE name='" + NoteDBHelper.TABLE_NOTE + "'");

                                                                db.setTransactionSuccessful(); // 设置事务成功
                                                            } catch (Exception e) {
                                                                Log.e(TAG, "删除所有notes错误: " + e.getMessage(), e);
                                                            } finally {
                                                                db.endTransaction(); // 结束事务
                                                            }

                                                            refreshListView();

                                                        case 2: //plans
                                                           /* planDbHelper = new PlanDatabase(context);
                                                            SQLiteDatabase pdb = planDbHelper.getWritableDatabase();
                                                            Cursor pcursor = pdb.rawQuery("select * from plans" ,null);
                                                            while(pcursor.moveToNext()){
                                                                if (pcursor.getString(pcursor.getColumnIndex(PlanDatabase.TIME)).compareTo(selectDateStr) < 0){
                                                                    pdb.delete("plans", PlanDatabase.ID + "=?", new String[]{Long.toString(pcursor.getLong(pcursor.getColumnIndex(PlanDatabase.ID)))});
                                                                }
                                                            }
                                                            pdb.execSQL("update sqlite_sequence set seq=0 where name='plans'");
                                                            refreshListView();*/
                                                            break;
                                                    }
                                                }
                                            }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            }).create().show();
                                }
                            });

                            AlertDialog dialog = builder.create();
                            dialog.show();
                            return true;
                        }
                    });
                }
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    // 使用 AsyncTask 加载数据
    private void loadAllNotes() {
        executor.execute(() -> {
            List<Note> notes = mNoteHelper.getAllNotes();
            handler.post(() -> {
                noteAdapter.updateNotes(notes);
                recyclerView.scrollToPosition(0);
            });
        });
    }


    private void initView() {
        mEmptyView = findViewById(R.id.emptyView); // search page
        fab = findViewById(R.id.fab);
        content_switch = findViewById(R.id.content_switch);
        myToolbar = findViewById(R.id.my_toolbar);

        addNoteList();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                intent.putExtra("mode", 4);     // MODE of 'new note'
                startActivityForResult(intent, 1);      //collect data from edit
                overridePendingTransition(R.anim.in_righttoleft, R.anim.out_righttoleft);
            }
        });

        // 初始时加载数据
        refreshListView();
    }

    private void addNoteList() {
        // 初始化 noteList
        noteList = new ArrayList<>();
        // 在 Activity 或 Fragment 中
        recyclerView = findViewById(R.id.lv);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));  // 或者 GridLayoutManager
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20); // 根据需要调整缓存大小

        // 创建 NoteAdapter 实例
        noteAdapter = new NoteAdapter(this, noteList, new NoteAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note) {
                // 处理笔记点击事件
                Toast.makeText(getApplicationContext(), "Clicked: " + note.getTitle(), Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                if ("".equals(searchQuery)) {
                    intent.putExtra("mode", 3);     // 点击进入编辑
                } else {
                    intent.putExtra("mode", 5);
                    intent.putExtra("searchQuery", searchQuery);// 搜索和编辑
                }

                intent.putExtra("title", note.getTitle());
                intent.putExtra("content", note.getContent());
                intent.putExtra("note_id", note.getNoteId());
                intent.putExtra("tag_name", note.getTagName());
//                Log.d(TAG, "点击标签:" + "note_id" + note.getNoteId() + ",title = " + note.getTitle() + ",content = " + note.getContent() + ",tag_name = " + note.getTagName());
                startActivityForResult(intent, 1);      //collect data from edit
                overridePendingTransition(R.anim.in_righttoleft, R.anim.out_righttoleft);

            }

            @Override
            public boolean onNoteLongClick(Note note) {
                Toast.makeText(getApplicationContext(), "LongClicked: " + note.getTitle(), Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        // 设置 Adapter
        recyclerView.setAdapter(noteAdapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNoteHelper.closeLink();
    }

    @Override
    protected void needRefresh() {
        // 可以在这里实现刷新逻辑
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data == null) {
            // 处理 data 为 null 的情况
            Log.e("MainActivity", "onActivityResult: data is null");
            return;
        }

        int returnMode;//0: create new note; 1: update current note; 2: delete current note
        long note_Id;
        //-1:nothing 0: create new note; 1: update current note; 2: delete current note
        returnMode = Objects.requireNonNull(data.getExtras()).getInt("mode", -1);
        note_Id = data.getExtras().getLong("note_id", 0);
        if (returnMode == 1) {  //update current note  更新
            String title = data.getExtras().getString("title");
            String content = data.getExtras().getString("content");
            String tag_name = data.getExtras().getString("tag_name");
            String update_time = data.getExtras().getString("update_time");

//            int tagId = data.getExtras().getInt("tagId", 1);

            Note note = new Note(title, content, tag_name);
//            note.setTagId(tagId);
            note.setNoteId(note_Id);
            note.setUpdateTime(update_time);
            mNoteHelper.update(note);
//            Log.d(TAG, "笔记更新成功:" + "note_id = " + note_Id + ",title = " + note.getTitle() + ",content = " + note.getContent());
//            achievement.editNote(op.getNote(note_Id).getContent(), content);
        } else if (returnMode == 2) {  //delete current note
            mNoteHelper.deleteById(note_Id);
//            achievement.deleteNote();
        } else if (returnMode == 0) {  // create new note
            String title = data.getExtras().getString("title");
            String content = data.getExtras().getString("content");
            String tag_name = data.getExtras().getString("tag_name");

            String create_time = data.getExtras().getString("create_time");

            Note note = new Note(title, content, tag_name);
            note.setNoteId(note_Id);
            note.setCreateTime(create_time);
            if (mNoteHelper.insert(note) > 0) {
//                Log.d(TAG, "笔记保存成功" + "note_id = " + note.getNoteId() + ",title = " + note.getTitle() + ",content = " + note.getContent());
            }
//            achievement.addNote(content);
        } else {
        }

        // 检查是否处于搜索状态
        if (!searchQuery.isEmpty()) {
            // 如果处于搜索状态，则重新加载搜索结果
            List<Note> matchedNotes = mNoteHelper.searchNotesByTitleOrContent(searchQuery);
            noteAdapter.updateNotes(matchedNotes);
            recyclerView.scrollToPosition(0);
        } else {
            // 否则，刷新整个列表
            refreshListView();
        }
    }

    private void refreshListView() {
        // 获取所有笔记的列表
        List<Note> updatedNoteList = mNoteHelper.getAllNotes();  // 假设这是你从数据库获取笔记数据的方式

        // 计算差异
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new NoteDiffCallback(noteList, updatedNoteList));

        // 更新本地笔记列表
        noteList.clear();
        noteList.addAll(updatedNoteList);

        // 应用差异
        if (noteAdapter != null) {
            diffResult.dispatchUpdatesTo(noteAdapter);
        }

        // 判断是否显示空视图
        if (noteList.isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);  // 显示空视图
        } else {
            mEmptyView.setVisibility(View.GONE);  // 隐藏空视图
        }
    }

    // 定义一个 DiffUtil.Callback 来计算差异
    private static class NoteDiffCallback extends DiffUtil.Callback {
        private final List<Note> oldList;
        private final List<Note> newList;

        public NoteDiffCallback(List<Note> oldList, List<Note> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getNoteId() == newList.get(newItemPosition).getNoteId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }

}
