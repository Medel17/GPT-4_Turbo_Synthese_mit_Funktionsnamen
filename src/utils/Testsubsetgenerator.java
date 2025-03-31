package utils;

import java.util.*;
import java.util.stream.Collectors;


/*
  Die Klasse `TestSubsetGenerator` dient dazu, Teilmengen (Subsets) von Unit-Tests zu erzeugen,
  um einen prozentualen Anteil der Unit-Tests für die Evaluierung auszuwählen.
 */
public class TestSubsetGenerator {

    
     // Erzeugt ein zufälliges Subset aus allen gegebenen Unit-Tests basierend auf dem gewünschten Prozentsatz
    public static List<String> generateSubset(String allTests, int percentage) {
        List<String> tests = Arrays.asList(allTests.split("\n")); // Zerlege den Unit-Test zeilenweise in eine Liste
        int subsetSize = (int) Math.ceil(tests.size() * (percentage / 100.0)); // Berechne die Anzahl der Unit-Tests für das Subset
        Collections.shuffle(tests); //  die Liste Mischen für eine zufällige Auswahl
        return tests.subList(0, subsetSize); // Gibt eine Teilmenge der Unit-Tests zurück
    }

    
     // Erzeugt zufällige Teilmengen von Unit-Tests für jede einzelne Hole im Code.
    public static Map<String, List<String>> generateDecompositionSubsets(Map<String, String> holeTests, int percentage) {
        Map<String, List<String>> subsets = new HashMap<>();

        // Iteriere durch jedes Hole und erstelle ein zufälliges Unit-Test-Subset 
        holeTests.forEach((holeName, testsContent) -> {
            List<String> tests = Arrays.asList(testsContent.split("\n")); // Zerlege die Unit-Tests in eine Liste
            int subsetSize = (int) Math.ceil(tests.size() * (percentage / 100.0)); // Berechne die Anzahl der auszuwählenden Unit-Tests
            Collections.shuffle(tests); // Durchmische die Unit-Tests für eine zufällige Auswahl
            subsets.put(holeName, tests.subList(0, subsetSize)); // Speichere das zufällige Teilset der Tests für die aktuelle Hole
        });
        return subsets;
    }
}
