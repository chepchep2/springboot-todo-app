package com.chep.demo.todo.service.email;

public class InvitationEmailTemplate {
    public record EmailContent(String subject, String html) {}

    public static EmailContent invite(String workspaceName, String inviteUrl) {
        String subject = "[PM SaaS] " + workspaceName + " 초대";
        String html = """
                <div>
                  <p><b>%s</b> 워크스페이스에 초대되었어요.</p>
                  <p><a href="%s">초대 수락하기</a></p>
                  <p style="color:#666;">링크가 안 열리면 아래 주소를 복사해 붙여넣어 주세요.</p>
                  <p>%s</p>
                </div>
            """.formatted(escape(workspaceName), inviteUrl, inviteUrl);
        return new EmailContent(subject, html);
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("<", "&lt;").replace(">", "&gt;");
    }
}
