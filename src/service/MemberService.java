package service;

import dao.MemberDAO;
import model.Member;
import util.PasswordUtil;

public class MemberService {
    private final MemberDAO memberDAO = new MemberDAO();

    public void register(String email, String password, String birthDate) {
        String encryptedPassword = PasswordUtil.encrypt(password);
        Member member = new Member(0, email, encryptedPassword, birthDate);
        memberDAO.addMember(member);
    }

    public Member login(String email, String password) {
        // 一般用戶與管理員皆由資料庫驗證
        Member member = memberDAO.getMemberByEmail(email);
        if (member != null && member.getPassword().equals(PasswordUtil.encrypt(password))) {
            return member;
        }
        return null;
    }
}