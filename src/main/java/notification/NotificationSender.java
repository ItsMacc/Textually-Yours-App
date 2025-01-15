package notification;

import com.AppState.io.AppStateManager;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class NotificationSender {

    private static final String USERNAME = "textuallyyoursco@gmail.com";
    private static final String APP_PASSWORD = "yrla jznc vvgt slog";
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    private static final String DEFAULT_SUBJECT = "Guess Who's Missing You " +
            "Right Now? \uD83D\uDC8C\uD83D\uDC95";
    private static final String DEFAULT_BODY =
            "We don't want to spill the beans, but %s can't stop thinking about you\uD83D\uDC93\uD83D\uDC93.<br>" +
                    "Don't keep them waiting...<br><br>Best,<br>Textually " +
                    "Yours";

    private static final String ADDRESS_SUBJECT = "Address Update Alert";

    private static final String ADDRESS_BODY =
            "Hello,<br><br>" +
                    "We’ve detected that your registered address has recently been updated.<br>" +
                    "To ensure uninterrupted service, please verify and " +
                    "update your address to the following:<br>%s<br><br>Best," +
                    "<br>Textually Yours";

    private static final String EVENT_SUBJECT = "Your Special Date is Confirmed! \uD83D\uDC95";

    private static final String EVENT_BODY =
            "Hello %s,<br>" +
                    "Great News! Your plans with %s have been confirmed.<br>" +
                    "Here are the details for the event:<br>" + "<br>" +
                    "Event: %s<br>" +
                    "Time: %s<br><br>" +
                    "We hope you both have an unforgettable time together! " +
                    "Remember, moments like these are what make life special\uD83D\uDC96.<br><br>" +
                    "Best,<br>Textually Yours";

    public static void sendEmail(String recipient) {
        sendEmail(recipient, "default");
    }

    public static void sendEmail(String recipient, String text, String... args) {
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", "true");
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
            String SUBJECT = "";
            String BODY = "";

            switch (text) {
                case "default" -> {
                    SUBJECT = DEFAULT_SUBJECT;
                    BODY = DEFAULT_BODY.formatted(AppStateManager.fetchProperty("username"));
                }
                case "address" -> {
                    SUBJECT = ADDRESS_SUBJECT;
                    BODY = ADDRESS_BODY.formatted(args[0]);
                }
                case "event" -> {
                    SUBJECT = EVENT_SUBJECT;
                    BODY = EVENT_BODY.formatted(args[0], args[1], args[2], args[3]);
                }
            }

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(
                    Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(SUBJECT);

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(BODY, "text/html; charset=UTF-8");

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
