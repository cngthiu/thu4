//src/main/java/com/example/library/service/MemberService.java
package com.example.library.service;

import com.example.library.dto.MemberDto;
import com.example.library.jooq.tables.records.MemberRecord;
import org.jooq.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.example.library.jooq.tables.Member.MEMBER;

@Service
public class MemberService {
    private final DSLContext dsl;
    public MemberService(DSLContext dsl) { this.dsl = dsl; }

    public Result<Record> search(String q, int page, int size) {
        Condition cond = DSL.trueCondition();
        if (q != null && !q.isBlank()) {
            cond = cond.and(MEMBER.FULL_NAME.likeIgnoreCase("%" + q.trim() + "%")
                    .or(MEMBER.EMAIL.likeIgnoreCase("%" + q.trim() + "%")));
        }
        return dsl.select().from(MEMBER).where(cond)
                .orderBy(MEMBER.MEMBER_ID.desc())
                .limit(size).offset(page * size).fetch();
    }

    public MemberRecord getById(Long id) {
        return dsl.selectFrom(MEMBER).where(MEMBER.MEMBER_ID.eq(id)).fetchOne();
    }

    @Transactional
    public Long create(MemberDto dto) {
        MemberRecord r = dsl.newRecord(MEMBER);
        r.setFullName(dto.fullName());
        r.setEmail(dto.email());
        r.setPhone(dto.phone());
        r.setStatus(dto.status() == null ? "ACTIVE" : dto.status());
        r.store();
        return r.getMemberId();
    }

    @Transactional
    public void update(Long id, MemberDto dto) {
        int rows = dsl.update(MEMBER)
                .set(MEMBER.FULL_NAME, dto.fullName())
                .set(MEMBER.EMAIL, dto.email())
                .set(MEMBER.PHONE, dto.phone())
                .set(MEMBER.STATUS, dto.status() == null ? "ACTIVE" : dto.status())
                .where(MEMBER.MEMBER_ID.eq(id)).execute();
        if (rows == 0) throw new IllegalArgumentException("Member not found: " + id);
    }

    @Transactional
    public void delete(Long id) {
        dsl.deleteFrom(MEMBER).where(MEMBER.MEMBER_ID.eq(id)).execute();
    }
}
