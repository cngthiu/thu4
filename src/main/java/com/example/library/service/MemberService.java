package com.example.library.service;

import com.example.library.dto.MemberDto;
import com.example.library.jooq.enums.MemberStatus;
import com.example.library.jooq.tables.records.MemberRecord;
import java.util.Locale;
import java.util.List;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.library.jooq.tables.Member.MEMBER;

@Service
public class MemberService {
    private final DSLContext dsl;

    public MemberService(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Result<MemberRecord> search(String q, int page, int size) {
        Condition condition = DSL.trueCondition();
        if (q != null && !q.isBlank()) {
            String keyword = "%" + q.trim() + "%";
            condition = condition.and(
                    MEMBER.FULL_NAME.likeIgnoreCase(keyword)
                            .or(MEMBER.EMAIL.likeIgnoreCase(keyword))
            );
        }
        return dsl.selectFrom(MEMBER)
                .where(condition)
                .orderBy(MEMBER.MEMBER_ID.desc())
                .limit(size)
                .offset(page * size)
                .fetch();
    }

    public MemberRecord getById(Long id) {
        return dsl.selectFrom(MEMBER)
                .where(MEMBER.MEMBER_ID.eq(id))
                .fetchOne();
    }

    public List<MemberRecord> listActiveMembers() {
        return dsl.selectFrom(MEMBER)
                .where(MEMBER.STATUS.eq(MemberStatus.ACTIVE))
                .orderBy(MEMBER.FULL_NAME.asc())
                .fetch();
    }

    public long countAll() {
        return dsl.fetchCount(MEMBER);
    }

    public long countActive() {
        return dsl.fetchCount(
                dsl.selectFrom(MEMBER)
                        .where(MEMBER.STATUS.eq(MemberStatus.ACTIVE))
        );
    }

    @Transactional
    public Long create(MemberDto dto) {
        MemberRecord record = dsl.newRecord(MEMBER);
        record.setFullName(dto.fullName());
        record.setEmail(dto.email());
        record.setPhone(dto.phone());
        record.setStatus(resolveStatus(dto.status()));
        record.store();
        return record.getMemberId();
    }

    @Transactional
    public void update(Long id, MemberDto dto) {
        int rowsUpdated = dsl.update(MEMBER)
                .set(MEMBER.FULL_NAME, dto.fullName())
                .set(MEMBER.EMAIL, dto.email())
                .set(MEMBER.PHONE, dto.phone())
                .set(MEMBER.STATUS, resolveStatus(dto.status()))
                .where(MEMBER.MEMBER_ID.eq(id))
                .execute();
        if (rowsUpdated == 0) {
            throw new IllegalArgumentException("Member not found: " + id);
        }
    }

    @Transactional
    public void delete(Long id) {
        dsl.deleteFrom(MEMBER)
                .where(MEMBER.MEMBER_ID.eq(id))
                .execute();
    }

    private MemberStatus resolveStatus(String status) {
        if (status == null || status.isBlank()) {
            return MemberStatus.ACTIVE;
        }
        try {
            return MemberStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid member status: " + status, ex);
        }
    }
}
