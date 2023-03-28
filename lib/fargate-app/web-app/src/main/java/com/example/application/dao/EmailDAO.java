package com.example.application.dao;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.application.Toolkit;
import com.example.application.data.Email;
import com.example.application.security.DataSourceImpl;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.GetIdentityVerificationAttributesRequest;
import software.amazon.awssdk.services.ses.model.GetIdentityVerificationAttributesResponse;
import software.amazon.awssdk.services.ses.model.IdentityVerificationAttributes;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import software.amazon.awssdk.services.ses.model.VerifyEmailAddressRequest;
import software.amazon.awssdk.services.ses.model.VerifyEmailAddressResponse;

@VaadinSessionScope
@Component
public class EmailDAO {
	private final static Logger logger = LogManager.getLogger(EmailDAO.class);
	private static final String region = System.getenv("AWS_REGION");
	private static Region REGION = Region.of(region);

	@Autowired
	private DataSourceImpl dataSource;
/*
	public void sendEmail(Email email) {
		try {
			SesClient sesClient = SesClient.builder().region(REGION).build();
			Content subject = Content.builder().data(email.getSubject()).build();
			Content bodyContent = Content.builder().data(email.getBody()).build();
			Body body = Body.builder().html(bodyContent).build();
			Message message = Message.builder().subject(subject).body(body).build();
			Destination destination = Destination.builder().ccAddresses(email.getRecipients()).build();

			SendEmailRequest sesRequest = SendEmailRequest.builder()
					.destination(destination)
					.source(Toolkit.SENDER)
					.replyToAddresses(Toolkit.SENDER)
					.message(message).build();
			
			
			
			SendEmailResponse response = sesClient.sendEmail(sesRequest);

		} catch (Exception ex) {
			logger.error(ex.getMessage());

		}
		email.setDateSent(Toolkit.getSanDiegoTime());
		insertToDb(email);
	}*/
	
	public static void sendEmail(Email email) throws MessagingException {
		Session session = Session.getDefaultInstance(new Properties());
		

		 // Create a new MimeMessage object.
		 MimeMessage message = new MimeMessage(session);

		 // Add subject, from and to lines.
		 message.setSubject(email.getSubject(), "UTF-8");
		 message.setFrom(new InternetAddress(Toolkit.SENDER));
		 //message.setRecipients(RecipientType.TO, InternetAddress.parse(RECIPIENT));
		 // Create a multipart/alternative child container.
		 MimeMultipart msg_body = new MimeMultipart("alternative");

		 // Create a wrapper for the HTML and text parts.
		 MimeBodyPart wrap = new MimeBodyPart();

		 // Define the text part.
//		 MimeBodyPart textPart = new MimeBodyPart();
	//	 textPart.setContent(BODY_TEXT, "text/plain; charset=UTF-8");

		 // Define the HTML part.
		 MimeBodyPart htmlPart = new MimeBodyPart();
		 htmlPart.setContent(email.getBody(),"text/html; charset=UTF-8");

		 // Add the text and HTML parts to the child container.
		// msg_body.addBodyPart(textPart);
		 msg_body.addBodyPart(htmlPart);

		 // Add the child container to the wrapper object.
		 wrap.setContent(msg_body);

		 // Create a multipart/mixed parent container.
		 MimeMultipart msg = new MimeMultipart("mixed");

		 // Add the parent container to the message.
		 message.setContent(msg);

		 // Add the multipart/alternative part to the message.
		 msg.addBodyPart(wrap);

		 
		 if (email.getAttachment()!=null) {
			 // Define the attachment
			 MimeBodyPart att = new MimeBodyPart();
			 DataSource fds = new FileDataSource(email.getAttachment().getAbsolutePath());
			 att.setDataHandler(new DataHandler(fds));
			 att.setFileName(fds.getName());

			 // Add the attachment to the message.
			 msg.addBodyPart(att);

		 }
		 // Try to send the email.
		 try {
		 System.out.println("Attempting to send an email through Amazon SES "
		 +"using the AWS SDK for Java...");
		 // Instantiate an Amazon SES client, which will make the service
		 // call with the supplied AWS credentials.
		 SesClient sesClient = SesClient.builder().region(REGION).build();
			
		 //AmazonSimpleEmailService client =AmazonSimpleEmailServiceClientBuilder.standard()
		 // Replace US_WEST_2 with the AWS Region you're using for
		 // Amazon SES.
		 //.withRegion(Regions.US_WEST_2).build();

		 // Print the raw email content on the console (useful for troubleshooting).
	//	 PrintStream out = System.out;
		// message.writeTo(out);
		 // Send the email.
		 ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		 message.writeTo(outputStream);
	//	 RawMessage rawMessage =new RawMessage(ByteBuffer.wrap(outputStream.toByteArray()));
	     RawMessage rawMessage = RawMessage.builder().data(SdkBytes.fromByteBuffer(ByteBuffer.wrap(outputStream.toByteArray()))).build();

	     
	    // Destination destination = Destination.builder().ccAddresses(email.getRecipients()).build();

	     SendRawEmailRequest rawEmailRequest = SendRawEmailRequest.builder()
	     	.rawMessage(rawMessage)
	     	.destinations(email.getRecipients())
	     	.build();
	     /*
		 SendRawEmailRequest rawEmailRequest =
		 new SendRawEmailRequest(rawMessage)
		 .withConfigurationSetName(CONFIGURATION_SET);
	*/
		 sesClient.sendRawEmail(rawEmailRequest);
		 System.out.println("Email sent!");
		 
		 if (email.getAttachment()!=null) {
				//File file = new File(email.getAttachmentPath());
				//file.delete();
			 email.getAttachment().delete();
		 }
		 // Display an error if something goes wrong.
		 } catch (Exception ex) {
		 System.out.println("Email Failed");
		 System.err.println("Error message: " + ex.getMessage());
		 ex.printStackTrace();
		 }
		 }

	
	/*
	public static void sendMail(String subject, String message, byte[] attachement, String fileName, String contentType, String from, String[] to) {
	    try {
	        Session s = Session.getInstance(new Properties(), null);
	        MimeMessage mimeMessage = new MimeMessage(s);

	        mimeMessage.setFrom(new InternetAddress(from));
	        for (String toMail : to) {
	            mimeMessage.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(toMail));
	        }

	        mimeMessage.setSubject(subject);

	        MimeMultipart mimeBodyPart = new MimeMultipart();
	        BodyPart part = new MimeBodyPart();
	        part.setContent(message, MediaType.TEXT_HTML);
	        
	        mimeBodyPart.addBodyPart(part);

	        part = new MimeBodyPart();
	        DataSource source = new ByteArrayDataSource(attachement, contentType);
	        part.setDataHandler(new DataHandler(source));
	        part.setFileName(fileName);
	        mimeBodyPart.addBodyPart(part);

	        mimeMessage.setContent(mimeBodyPart);
	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        mimeMessage.writeTo(outputStream);
	        RawMessage rawMessage = RawMessage.builder().data(SdkBytes.fromByteBuffer(ByteBuffer.wrap(outputStream.toByteArray()))).build();
	        
	        SendRawEmailRequest.builder()
	        	.destinations(to)
	        SendRawEmailRequest rawEmailRequest = new SendRawEmailRequest(rawMessage);
	        rawEmailRequest.setDestinations(Arrays.asList(to));
	        rawEmailRequest.setSource(from);
	        client.sendRawEmail(rawEmailRequest);

	    } catch (IOException | MessagingException e) {
	        e.printStackTrace();
	    }
	}*/
	
	
	
	
	
	public void verifyEmail(String email) {
		VerifyEmailAddressRequest request = VerifyEmailAddressRequest.builder().emailAddress(email).build();
		SesClient sesClient = SesClient.builder().region(REGION).build();
		VerifyEmailAddressResponse response = sesClient.verifyEmailAddress(request);
		int responseStatus = response.sdkHttpResponse().statusCode();
		logger.info("Verifying email address " + email + ", response code=" + responseStatus);
	}

	public boolean validEmailAddress(String emailAddress) {
		SesClient sesClient = SesClient.builder().region(REGION).build();
		GetIdentityVerificationAttributesRequest getIdentityVerificationAttributesRequest = GetIdentityVerificationAttributesRequest
				.builder()
				.identities(emailAddress)
				.build();
		GetIdentityVerificationAttributesResponse response = sesClient
				.getIdentityVerificationAttributes(getIdentityVerificationAttributesRequest);
		if (!response.hasVerificationAttributes()) {
			return false;
		}
		Map<String, IdentityVerificationAttributes> map = response.verificationAttributes();

		boolean valid = false;
		for (String s : map.keySet()) {
			IdentityVerificationAttributes attributes = map.get(s);
			String token = attributes.verificationToken();
			String status = attributes.verificationStatusAsString();

			valid = status.equalsIgnoreCase("Success");
			// System.out.println(s + "," + status + ", " + token);

		}
		return valid;
	}

	public void insertToDb(Email email) {
		String sql = "INSERT INTO emailLog (subject, body, dateSent, recipients) VALUES (?,?,?,?)";
		try (
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);

		) {
			ps.setString(1, email.getSubject());
			ps.setString(2, email.getBody());
			ps.setTimestamp(3, java.sql.Timestamp.valueOf(email.getDateSent()));
			ps.setString(4, String.join(", ", email.getRecipients()));
			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				rs.next();
				long id = rs.getLong(1);
				email.setId(id);
			}

		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}
}
