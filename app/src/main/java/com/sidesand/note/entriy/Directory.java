package com.sidesand.note.entriy;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Directory {
    // 文件夹ID
    private long id;
    private String name;
    private String path;
    private boolean isDeleted;
    private boolean isHide;
}
