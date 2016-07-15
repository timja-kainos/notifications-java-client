import org.jose4j.json.internal.json_simple.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class NotificationClient {

    private String secret; //"12f2459c-7402-4796-ac70-8278b7a16b4c"
    private String issuer; //"49c0b4bf-abf8-41ec-8470-a0955cb91aaa"
    private String baseUrl; // https://api.notifications.service.gov.uk

    public NotificationClient(String secret, String issuer, String baseUrl) {
        this.secret = secret;
        this.issuer = issuer;
        this.baseUrl = baseUrl;
    }

    public NotificationResponse sendEmail(String templateId, String to, String personalisation) throws NotificationClientException {
        return postRequest("email", templateId, to, personalisation);
    }
    public NotificationResponse sendSms(String templateId, String to, String personalisation) throws NotificationClientException {
        return postRequest("sms", templateId, to, personalisation);
    }

    private NotificationResponse postRequest(String messageType, String templateId, String to, String personalisation) throws NotificationClientException {
        HttpsURLConnection conn = null;
        try {
            JSONObject body = setPersonalisation(templateId, to, personalisation);

            Authentication tg = new Authentication();
            String token = tg.create(issuer, secret);
            URL url = new URL(baseUrl + "/notifications/" + messageType);
            conn = postConnection(token, url);

            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(body.toString());
            wr.flush();

            int httpResult = conn.getResponseCode();
            if (httpResult == HttpsURLConnection.HTTP_CREATED) {
                StringBuilder sb = readStream(new InputStreamReader(conn.getInputStream(), "utf-8"));
                return new NotificationResponse(sb.toString());
            } else {
                StringBuilder sb = readStream(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                throw new NotificationClientException(httpResult, sb.toString());
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
           if (conn != null){
               conn.disconnect();
           }
        }
        return null;
    }

    public Notification getNotificationById(String notificationId) throws NotificationClientException {
        StringBuilder stringBuilder;
        HttpsURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + "/notifications/"+ notificationId);
            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            Authentication authentication = new Authentication();
            String token = authentication.create(issuer, secret);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.connect();
            int httpResult = conn.getResponseCode();
            if(httpResult == 200) {
                stringBuilder = readStream(new InputStreamReader(conn.getInputStream()));
                conn.disconnect();
                return new Notification(stringBuilder.toString());
            }
            else{
                stringBuilder = readStream(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                throw new NotificationClientException(httpResult, stringBuilder.toString());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(conn != null){
                conn.disconnect();
            }
        }
        return null;
    }

    private HttpsURLConnection postConnection(String token, URL url) throws IOException {
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");

        conn.connect();
        return conn;
    }

    private JSONObject setPersonalisation(String templateId, String to, String personalisation) {
        JSONObject body = new JSONObject();
        body.put("to", to); //"+447515349060"
        body.put("template", templateId); //"5151207f-c46f-442a-a627-bf6dd37166cd"
        if(personalisation != null && !personalisation.isEmpty()){
            System.out.println("personalisation: " + personalisation);
            body.put("personalisation", personalisation);
        }
        return body;
    }
    private StringBuilder readStream(InputStreamReader streamReader) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(streamReader);
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();
        System.out.println("" + sb.toString());
        return sb;
    }

}