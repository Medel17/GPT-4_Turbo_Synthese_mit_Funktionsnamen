package main;

import utils.FileUtils;
import utils.TestSubsetGenerator;

import api.ChatGPTClient;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.DirectoryStream;
import java.util.Map;
import java.util.List;
import java.util.stream.IntStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/*
 Hauptklasse zur Durchführung der Code-Synthese und Evaluation mit der ChatGPT-API.
 Der Code unterstützt zwei Modus: "baseline" und "decomposition".
 "baseline": Direktes Generieren der vollständigen Funktion basierend auf Unit-Tests.
 "decomposition": Generierung einzelner Codeblöcke (Holes) mit zugehörigen Tests.
*/
public class App {
    public static void main(String[] args) throws Exception {
        // Überprüfen, ob der Benutzer einen Modus ("baseline" oder "decomposition") angegeben hat
        if (args.length < 1) {
            System.out.println("Usage: java main.App <mode>");
            return;
        }

        String mode = args[0]; // "baseline" oder "decomposition"
        String apiKey = "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"; // ChatGPT API-Schlüssel
        ChatGPTClient client = new ChatGPTClient(apiKey);

        // Liste aller zu testenden Funktionen abrufen
        List<String> functions = getFunctions("src/functions");

        // Iteriere über alle Funktionen und führe die Code-Synthese durch
        for (String functionName : functions) {
            System.out.println("Processing function: " + functionName);

            String skeleton = Files.readString(Paths.get("src/functions/" + functionName + "/skeleton.txt")); // Lade die Skelettdatei für die Decompositionsmodus Methode
            String Baseline_Skeleton = Files.readString(Paths.get("src/functions/" + functionName + "/baseline_skeleton.txt")); // Lade die Skelettdatei für die Baselinemodus Methode
            String baselineTests = Files.readString(Paths.get("src/functions/" + functionName + "/baseline_tests.txt")); // Lade die Baseline-Unittests

            // Lade spezifische Testfälle für Lücken in der Funktion (falls Decompositionsmodus aktiv ist)
            Map<String, String> holeTests = FileUtils.readHoleTests("src/functions/" + functionName + "/decomposition_tests/"); 

            // Definiere Prozentsätze der Testfälle, die verwendet werden sollen
            int[] percentages = {25, 37, 50, 62, 75, 87, 100};

            // Iteriere über alle Prozentsätze und führe die Code-Synthese durch
            for (int percentage : percentages) {
                if (mode.equalsIgnoreCase("baseline")) {
                    // Erstelle eine Teilmenge der Tests für den Baseline-Modus
                    List<String> testSubset = TestSubsetGenerator.generateSubset(baselineTests, percentage);

                    // Führe 10 Testdurchläufe für jede Prozentstufe aus
                    IntStream.rangeClosed(1, 10).forEach(run -> {
                        try {
                            String prompt = buildBaselinePrompt(Baseline_Skeleton, testSubset); // Erstelle den Prompt für die Baseline-Code-Synthese
                            String response = client.sendPrompt(prompt, "gpt-4-turbo"); // Sende den Prompt an die ChatGPT-API
                            saveResponse(functionName, "baseline", percentage, run, response); // Speichere die Antwort für spätere Evaluierung
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } else if (mode.equalsIgnoreCase("decomposition")) {
                    // Erstelle eine Teilmenge der Tests für den Decomposition-Modus
                    Map<String, List<String>> testSubsets = TestSubsetGenerator.generateDecompositionSubsets(holeTests, percentage);

                    // Führe 10 Testdurchläufe für jede Prozentstufe aus
                    IntStream.rangeClosed(1, 10).forEach(run -> {
                        try {
                            String prompt = buildDecompositionPrompt(skeleton, testSubsets); // Erstelle den Prompt für die Decomposition-Code-Synthese
                            String response = client.sendPrompt(prompt, "gpt-4-turbo"); // Sende den Prompt an die ChatGPT-API
                            saveResponse(functionName, "decomposition", percentage, run, response); // Speichere die Antwort für spätere Evaluierung
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        }
    }
    // Ruft die Liste der Funktionsnamen aus dem gegebenen Verzeichnis ab.
    private static List<String> getFunctions(String functionsDir) throws IOException {
        List<String> functions = new ArrayList<>();
        try (DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(Paths.get(functionsDir))) {
            for (java.nio.file.Path path : stream) {
                if (Files.isDirectory(path)) {
                    functions.add(path.getFileName().toString());
                }
            }
        }
        return functions;
    }


    /**
      Erstellt eine Eingabeaufforderung (Prompt) für den Baseline-Modus.
      param skeleton Das Funktionsskelett.
      param testSubset Die ausgewählte Teilmenge der Unit-Tests.
     */
    private static String buildBaselinePrompt(String Baseline_Skeleton, List<String> testSubset) {
        return String.format("""
            **Task Overview**:
            You are a program synthesis model tasked with filling in missing sections of a partially written Java program.
            you should provide the code from scratch using all the Unit-tests you have.
            The code you need to complete starts with // ### Missing section and ends with // ### End of Missing section.
            
            **Function Skeleton**:
            Here is the Java code that needs to be completed:

            %s

            **Unit Tests**:
            %s
            
            **Important Note**: give me the function directly do not start it with ```java and end with ```
            **Important Note**: Provide the complete code with all missing sections filled.
            **Important Note**: Do not skip any holes, and return the code directly without using code block markers.
            
            """, Baseline_Skeleton, String.join("\n", testSubset));
    }

    /**
      Erstellt eine Eingabeaufforderung (Prompt) für den Decomposition-Modus.
      param skeleton Das Funktionsskelett.
      param testSubsets Die Testfälle für jede Lücke (Hole).
     */
    private static String buildDecompositionPrompt(String skeleton, Map<String, List<String>> testSubsets) {
        StringBuilder testsBuilder = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : testSubsets.entrySet()) {
            testsBuilder.append(String.format("Hole: %s\n%s\n\n", entry.getKey(), String.join("\n", entry.getValue())));
        }
        return String.format("""
            **Task Overview**:
            You are a program synthesis model tasked with filling in missing sections of a partially written Java program. The code contains several marked holes (`HOLE BEGIN` and `HOLE END`). Your goal is to provide a solution that completes the program according to the problem specification, while passing all provided unit tests.

            **Function Skeleton**:
            %s

            **Unit Tests**:
            For each Hole, ensure the code passes the following unit tests:

            %s

            **Important Note**: give me the function directly do not start it wit ```java and end with ```
            **Important Note**: Provide the complete code with all missing sections filled.
            **Important Note**: Do not skip any holes, and return the code directly without using code block markers.
            **Important Note**: do not give me explanations or comments. Give me just the code that passes the tests directly.
            **Important Note**: Ensure that the solution for each hole passes the associated tests and that the function is logically correct.
            """, skeleton, testsBuilder.toString());
    }

    /**
      Speichert die von der LLM generierte Antwort in einer Datei.
      param functionName Name der Funktion.
      param mode Verwendeter Modus ("baseline" oder "decomposition").
      param percentage Der Prozentsatz der verwendeten Tests.
      param run Die aktuelle Testdurchlauf.
      param response Die von ChatGPT generierte Antwort.
     */
    private static void saveResponse(String functionName, String mode, int percentage, int run, String response) {
        String dir = String.format("results/%s/%s/%d_percent", functionName, mode, percentage);
        try {
            Files.createDirectories(Paths.get(dir));
            try (FileWriter writer = new FileWriter(String.format("%s/run_%d.txt", dir, run))) {
                writer.write(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}