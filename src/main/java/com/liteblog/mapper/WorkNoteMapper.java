package com.liteblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liteblog.entity.WorkNote;
import org.apache.ibatis.annotations.*;

@Mapper
public interface WorkNoteMapper extends BaseMapper<WorkNote> {
    // Compare-and-swap applies to every mutation, including archive and restore.
    @Update("""
        UPDATE work_note SET title = #{note.title}, main_problem = #{note.mainProblem},
          content = #{note.content,typeHandler=com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler},
          archived = #{note.archived}, version = version + 1, updated_at = #{note.updatedAt}
        WHERE id = #{note.id} AND owner_id = #{note.ownerId} AND version = #{note.version}
        """)
    int replace(@Param("note") WorkNote note);
}
