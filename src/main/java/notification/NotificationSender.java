package notification;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class NotificationSender {

    private static final String USERNAME = "textuallyyoursco@gmail.com";
    private static final String APP_PASSWORD = "yrla jznc vvgt slog";
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    private static final String DEFAULT_SUBJECT = "Guess Who's Missing You " +
            "Right Now?";
    private static final String DEFAULT_BODY = """
            We don't want to spill the beans, but they can't stop thinking\s
            about you! Don't keep them waiting...
           """;

    private static final String ADDRESS_SUBJECT = "Address Update Alert";

    private static final String ADDRESS_BODY = """
        Hello,
        
        We’ve detected that your registered address has recently been updated.
        To ensure uninterrupted service, please verify and update your address to the following:
        """;

    public static void sendEmail(String recipient) {
        sendEmail(recipient, "default");
    }

    public static void sendEmail(String recipient, String text) {
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", true);
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.host", SMTP_HOST);
        prop.put("mail.smtp.port", SMTP_PORT);
        prop.put("mail.smtp.ssl.trust", SMTP_HOST);

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, APP_PASSWORD);
            }
        });

        try {
            String SUBJECT;
            String BODY;

            if (text.equals("default")){
                SUBJECT = DEFAULT_SUBJECT;
                BODY = DEFAULT_BODY;
            } else {
                SUBJECT = ADDRESS_SUBJECT;
                BODY = ADDRESS_BODY + "\n" + text + "\n Best Regards," +
                        "\nTextually Yours Support";
            }

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(
                    Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(SUBJECT);

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(BODY, "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            message.setContent(multipart);

            Transport.send(message);

            System.out.println("Email sent successfully to " + recipient);
        } catch (MessagingException e) {
            System.err.println("Failed to send email to " + recipient);
        }
    }
}
