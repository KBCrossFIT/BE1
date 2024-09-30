package com.be.member.service;

import com.be.exception.CustomException;
import com.be.member.domain.Member;
import com.be.member.domain.MemberRole;
import com.be.member.domain.type.Role;
import com.be.member.dto.req.MemberLoginReqDto;
import com.be.member.dto.req.MemberRegisterReqDto;
import com.be.member.dto.req.MemberResponseReqDto;
import com.be.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.be.common.code.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {


    private final MemberMapper memberMapper;

    @Bean
    private PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder();}

    public Member registerMember(MemberRegisterReqDto reqDto) {

        validateMemberID(reqDto.getMemberID());
        validateMemberEmail(reqDto.getEmail());
        checkPasswordMatching(reqDto.getPassword(), reqDto.getReEnteredPassword());
        String encodedPassword = encodePassword(reqDto.getPassword());
        Member member = reqDto.toMember(encodedPassword);

        saveMember(member);
        member = fineOneMemberById(member.getMemberID());

        MemberRole memberRole = createUserRole(member);
        saveUserRole(memberRole);

        return member;
    }


    public Member login(MemberLoginReqDto memberLoginReqDto) {
        Member member = fineOneMemberById(memberLoginReqDto.getMemberID());

        boolean isVerified = verifyPassword(member, memberLoginReqDto.getPassword());
        if (!isVerified) {
            throw new CustomException(LOGIN_UNAUTHENTICATED);
        }

        return member;
    }

    private Member fineOneMemberById(String memberId) {
        Optional<Member> member = Optional.ofNullable(memberMapper.findOneByMemberID(memberId));

        if (member.isEmpty()) {
            log.info("사용자가 존재하지 않습니다.");
            throw new CustomException(LOGIN_UNAUTHENTICATED);
        }

        return member.get();
    }

    private boolean verifyPassword(Member user, String requestPassword) {
        // 로그인 시 비밀번호 일치여부 확인
        return passwordEncoder().matches(requestPassword, user.getPassword());

    }

    public void validateMemberID(String memberID) {

        isExistID(memberID);
    }

    public void validateMemberEmail(String Email) {

        isExistMemberEmail(Email);
    }

    public void checkPasswordMatching(String password, String reEnteredPassword) {
        // 회원가입 시 비밀번호 일치 여부 확인
        if (!password.equals(reEnteredPassword))
            throw new CustomException(PASSWORD_MATCH_INVALID);
    }


    public String encodePassword(String password) {
        return passwordEncoder().encode(password);
    }

    public void isExistID(String memberID) {
        Optional<Member> member = Optional.ofNullable(memberMapper.findOneByMemberID(memberID));

        if (member.isPresent()) {
            log.info("ID already exists: {}", memberID);
            throw new CustomException(EXISTING_MEMBER_ID);
        }
    }

    public void isExistMemberEmail(String email) {
        Optional<Member> member = Optional.ofNullable(memberMapper.findOneByMemberEmail(email));

        if (member.isPresent()) {
            throw new CustomException(EXISTING_EMAIL);
        }
    }

    /**
     * 회원 권한 생성
     */
    private MemberRole createUserRole(Member member) {
        return MemberRole.builder()
                .member(member)
                .role(Role.MEMBER)
                .build();
    }

    private void saveMember(Member member) {
        try {
            memberMapper.insert(member);
        } catch (RuntimeException e) {
            throw new CustomException(e, SERVER_ERROR);
        }
    }

    private void saveUserRole(MemberRole memberRole) {


    }


    // 투자 성향 점수와 성향을 업데이트하는 메서드
    public int updateMemberPreference(Long memberNum, Integer investScore) {
        // Null 체크
        if (memberNum == null || investScore == null) {
            throw new IllegalArgumentException("memberNum 또는 investScore가 null입니다.");
        }

        Integer preference = analyzeInvestmentPreference(investScore);

        // Mapper를 통해 DB 업데이트
        return memberMapper.updateInvestScoreAndPreference(memberNum, investScore, preference);
    }

    // 투자 성향 분석 로직 (기존)
    public int analyzeInvestmentPreference(int investScore) {
        if (investScore >= 1 && investScore <= 20) {
            return 1;  // 안정형
        } else if (investScore >= 21 && investScore <= 40) {
            return 2;  // 안정추구형
        } else if (investScore >= 41 && investScore <= 60) {
            return 3;  // 위험중립형
        } else if (investScore >= 61 && investScore <= 80) {
            return 4;  // 적극투자형
        } else if (investScore >= 81 && investScore <= 100) {
            return 5;  // 공격투자형
        } else {
            throw new IllegalArgumentException("잘못된 투자 점수입니다.");
        }
    }

    // 사용자의 투자 성향 점수와 성향을 조회하는 메서드
    public MemberResponseReqDto getMemberPreference(Long memberNum) {
        // Mapper를 통해 DB에서 사용자 데이터를 조회
        Member member = memberMapper.findOneByMemberNum(memberNum);

        // 만약 사용자가 존재하지 않으면 null을 반환
        if (member == null) {
            return null;
        }

        // 조회된 데이터를 MemberResponse 객체로 변환
        MemberResponseReqDto response = new MemberResponseReqDto();
        response.setMemberNum(member.getMemberNum());
        response.setInvestScore(member.getInvestScore());
        response.setPreference(member.getPreference());
        return response;
    }
}
