package api;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;


// Kommunikation mit der OpenAI ChatGPT API
// Sendung von Anfragen an die API und der Verarbeitung der Antworten
public class ChatGPTClient {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions"; // API-Endpunkt
    private final String apiKey; // API-Schlüssel zur Authentifizierung
    private final ObjectMapper objectMapper;

    // Konstruktor für den ChatGPTClient mit dem API-Schlüssel
    public ChatGPTClient(String apiKey) {
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
    }

    public String sendPrompt(String prompt, String model) throws Exception {
        // Erstelle die JSON-Anfrage für die API
        String payload = objectMapper.writeValueAsString(createRequest(prompt, model));

        // Stelle eine Verbindung zur API her
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);

        // Sende die Anfrage
        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload.getBytes());
            os.flush();
        }

        // Lese die Antwort der API
        int responseCode = connection.getResponseCode();
        InputStream is = (responseCode == 200) ? connection.getInputStream() : connection.getErrorStream();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            // Ausgabe der vollständigen Antwort zur Debugging-Zwecken
            System.out.println("Response Code: " + responseCode);
            System.out.println("Response Body: " + responseBuilder);

            // Falls ein Fehler aufgetreten ist, eine Ausnahme werfen
            if (responseCode != 200) {
                throw new Exception("Error from API: " + responseBuilder.toString());
            }
            // Die Antwort der API analysieren und zurückgeben
            return parseResponse(responseBuilder.toString());
        }
    }

    // Erstellt eine Anfrage für die ChatGPT API
    private ChatGPTRequest createRequest(String prompt, String model) {
        ChatGPTMessage systemMessage = new ChatGPTMessage("system", "You are a Java program synthesis assistant"); 
        ChatGPTMessage userMessage = new ChatGPTMessage("user", prompt); 

        return new ChatGPTRequest(model, new ChatGPTMessage[]{systemMessage, userMessage}, 1500); // Erstelle das Anfrageobjekt mit Modell, Nachrichten und Token-Grenze
    }

    // Analysiert die Antwort der ChatGPT API und gibt die Antwort zurück
    private String parseResponse(String response) throws Exception {
        JsonNode jsonResponse = objectMapper.readTree(response);
        JsonNode choices = jsonResponse.get("choices");

        // Prüfen, ob die Antwort gültig ist und die Antwort zurückgeben
        if (choices != null && choices.isArray() && choices.size() > 0) {
            return choices.get(0).get("message").get("content").asText();
        } else {
            throw new Exception("Invalid response from ChatGPT API: " + response);
        }
    }

    // Innere Klasse zur Darstellung einer API-Anfrage an ChatGPT.
    private static class ChatGPTRequest {
        public String model; // Das zu verwendende Modell (z. B. "gpt-4", "gpt-3.5-turbo")
        public ChatGPTMessage[] messages; // Die übermittelten Nachrichten
        public int max_tokens; // Maximale Tokenanzahl für die Antwort

        public ChatGPTRequest(String model, ChatGPTMessage[] messages, int maxTokens) {
            this.model = model;
            this.messages = messages;
            this.max_tokens = maxTokens;
        }
    }

    // Innere Klasse zur Darstellung einer Nachricht für ChatGPT.
    private static class ChatGPTMessage {
        public String role;
        public String content; // Inhalt der Nachricht

        //Konstruktor für eine API-Nachricht
        public ChatGPTMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
