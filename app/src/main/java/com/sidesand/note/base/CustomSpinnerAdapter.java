// CustomSpinnerAdapter.java
package com.sidesand.note.base;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.sidesand.note.R;
import com.sidesand.note.entriy.NoteDBHelper;

import java.util.List;

public class CustomSpinnerAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final List<String> items;
    private final NoteDBHelper mNoteHelper;

    public CustomSpinnerAdapter(Context context, int resource, List<String> items) {
        super(context, resource, items);
        this.context = context;
        this.items = items;
        mNoteHelper = NoteDBHelper.getInstance(context);
    }

    @Override
    public int getCount() {
        return items.size() + 1; // 确保至少有一个条目，并且多一个用于“添加标签”
    }

    @Override
    public String getItem(int position) {
        if (position < items.size()) {
            return items.get(position);
        }
        return "添加标签"; // 最后一个位置返回“添加标签”
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.spinner_item, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.spinner_text); // 修改为实际的 ID

        if (textView != null) { // 添加空检查以避免 NullPointerException
            if (position < items.size()) {
                textView.setText(items.get(position));
            } else {
                textView.setText("请选择"); // 默认文本
            }
        }

        return convertView;
    }


    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        Log.d("CustomSpinnerAdapter", "getDropDownView Position: " + position + ", Items Size: " + items.size());

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.spinner_dropdown_item, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.tv_spinner_item);
        ImageView deleteIcon = convertView.findViewById(R.id.img_delete);

        if (position < items.size()) { // 普通标签项
            textView.setText(items.get(position));
            deleteIcon.setVisibility(View.VISIBLE); // 显示删除图标

            // 设置删除图标点击事件
            deleteIcon.setOnClickListener(v -> {
                showDeleteConfirmationDialog(position);
            });
        } else { // “添加标签”项
            textView.setText(getItem(position)); // 设置为“添加标签”
            deleteIcon.setVisibility(View.GONE); // 隐藏删除图标

            // 设置点击事件
            convertView.setOnClickListener(v -> {
                addItemDialog();
            });
        }

        return convertView;
    }

    private void showDeleteConfirmationDialog(int position) {
        if ("未分类".equals(items.get(position))) {
            Toast.makeText(context, "不能删除“未分类”标签", Toast.LENGTH_SHORT).show();
            return;
        }

        if (items.size() == 1) {
            Toast.makeText(context, "不能删除最后一个标签", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("确认删除");
        builder.setMessage("您确定要删除此标签吗？");

        // 设置“确定”按钮
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 删除标签
                String tagToDelete = items.remove(position);
                mNoteHelper.deleteTag(tagToDelete);
                notifyDataSetChanged();
                Toast.makeText(context, "标签已删除", Toast.LENGTH_SHORT).show();
            }
        });

        // 设置“取消”按钮
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // 显示对话框
        builder.show();
    }

    private void addItemDialog() {
        // 创建对话框构建器
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("添加新标签");

        // 添加输入框
        final EditText input = new EditText(context);
        builder.setView(input);

        // 设置“确定”按钮
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newTag = input.getText().toString().trim();
                if (!newTag.isEmpty()) {
                    if (items.contains(newTag)) {
                        Toast.makeText(context, "标签已存在", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mNoteHelper.insertTag(newTag);
                    // 添加新标签到数据源
                    items.add(newTag);
                    // 通知适配器数据已更改
                    notifyDataSetChanged();
                    Toast.makeText(context, "标签已添加", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "请输入有效的标签", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 设置“取消”按钮
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // 显示对话框
        builder.show();
    }
}
