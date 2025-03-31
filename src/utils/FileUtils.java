package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;


/*
  Hilfsklasse für Dateioperationen, insbesondere das Lesen von Dateien
  und das Einlesen von Testfällen für spezifische Codeabschnitte (Holes).
 */
public class FileUtils {
    public static String readFile(String path) {
        try {
            return Files.readString(Paths.get(path)); // Lesen des Dateiinhalts
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + path, e);
        }
    }

        //Liest alle Testfälle für die Holes aus einer angegebenen Verzeichnisstruktur.
        public static Map<String, String> readHoleTests(String directoryPath) {
        Map<String, String> holeTests = new HashMap<>();
        try {
            // Durchlaufe alle Dateien im angegebenen Verzeichnis
            Files.list(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String holeName = path.getFileName().toString().replace(".txt", ""); // Hole-Name aus Dateiname extrahieren
                    String content = readFile(path.toString()); // Hole-Inhalt aus Datei lesen
                    holeTests.put(holeName, content); // Hole-Name und Inhalt in Map speichern
                });
        } catch (IOException e) {
            throw new RuntimeException("Error reading hole tests from directory: " + directoryPath, e);
        }
        return holeTests; // Gibt die Map mit den Hole-Testfällen zurück
    }
}