package com.sidesand.note.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sidesand.note.R;
import com.sidesand.note.entriy.Note;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private final Context context;
    private final List<Note> noteList;  // 存储笔记的列表
    private final OnNoteClickListener onNoteClickListener;// 点击事件监听器

    public NoteAdapter(Context context, List<Note> noteList, OnNoteClickListener onNoteClickListener) {
        this.context = context;
        this.noteList = noteList;
        this.onNoteClickListener = onNoteClickListener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 创建每个列表项的视图
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(NoteViewHolder holder, int position) {
        Note note = noteList.get(position);

        // 设置笔记的标题
        holder.titleTextView.setText(note.getTitle());

        // 设置笔记的时间（如创建时间）
        holder.timeTextView.setText(note.getCreateTime());

        // 设置笔记的预览内容
        holder.previewTextView.setText(note.getContent());

        // 设置笔记的标签（如果有的话）
        holder.tagTextView.setText(note.getTagName());

        // 如果笔记被标记为删除，可以显示删除标识
        if (note.isDeleted()) {
            holder.deleteTextView.setVisibility(View.VISIBLE);
        } else {
            holder.deleteTextView.setVisibility(View.GONE);
        }

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> onNoteClickListener.onNoteClick(note));
        // 设置长按事件
        holder.itemView.setOnLongClickListener(v -> onNoteClickListener.onNoteLongClick(note));
    }

    @Override
    public int getItemCount() {
        return noteList.size();  // 返回笔记的数量
    }

    // 自定义一个接口，来监听笔记点击事件
    public interface OnNoteClickListener {
        void onNoteClick(Note note);

        boolean onNoteLongClick(Note note);
    }

    // ViewHolder 类：用来持有每个列表项的视图
    public static class NoteViewHolder extends RecyclerView.ViewHolder {

        TextView titleTextView;
        TextView timeTextView;
        TextView previewTextView;
        TextView tagTextView;
        TextView deleteTextView;

        public NoteViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.note_title);
            timeTextView = itemView.findViewById(R.id.note_time);
            previewTextView = itemView.findViewById(R.id.note_preview);
            tagTextView = itemView.findViewById(R.id.note_tag);
            deleteTextView = itemView.findViewById(R.id.note_deleted);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateNotes(List<Note> notes) {
        this.noteList.clear();
        this.noteList.addAll(notes);
        notifyDataSetChanged();
    }

}