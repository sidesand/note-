package com.sidesand.note.entriy;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Note {
    private long noteId;
    // 创建时间
    private String createTime;
    // 更新时间
    private String updateTime;

    private String title;
    private String content;
    // 上一个版本内容
    private String lastContent;
    private String tagName;
    // 是否删除
    private boolean isDeleted;
    private int mode;
    private int version;

    public Note() {}

    public Note(String title, String content, String tagName) {
        this.title = title;
        this.content = content;
        this.tagName = tagName;
    }
}
