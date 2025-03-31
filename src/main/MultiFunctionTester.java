package main;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class MultiFunctionTester {
    public static void main(String[] args) throws IOException {
        // Verzeichnisse und Dateien definieren
        String projectDir = System.getProperty("user.dir");
        String resultsDir = Paths.get(projectDir, "results").toString();
        String baseSrcDir = Paths.get(projectDir, "src", "functions").toString();
        String resultsFile = "test_results.csv";
        // Erstelle die Datei zum Speichern der Testergebnisse
        try (FileWriter resultsWriter = new FileWriter(resultsFile)) {
            resultsWriter.write("Function,Method,Percentage,Run,Passed,Failed\n");
            // Iteriere durch alle Funktionen im "results"-Verzeichnis
            Files.list(Paths.get(resultsDir)).filter(Files::isDirectory).forEach(functionDir -> {
                String functionName = functionDir.getFileName().toString();

                try {
                    // Iteriere durch verschiedene Methoden für jede Funktion
                    Files.list(functionDir).filter(Files::isDirectory).forEach(modeDir -> {
                        String methodName = modeDir.getFileName().toString();

                        try {
                            // Iteriere durch verschiedene Prozentsätze
                            Files.list(modeDir).filter(Files::isDirectory).forEach(percentageDir -> {
                                String percentage = percentageDir.getFileName().toString().replace("_percent", "");

                                try (DirectoryStream<Path> stream = Files.newDirectoryStream(percentageDir)) {
                                    for (Path runFile : stream) {
                                        // Prüfen, ob es sich um eine Testdatei handelt (.txt)
                                        if (runFile.toString().endsWith(".txt")) {
                                            String run = runFile.getFileName().toString().replace("run_", "").replace(".txt", "");
                                            String synthesizedCode = Files.readString(runFile);
                                            // Testdatei für die Funktion finden
                                            Path testsFile = Paths.get(baseSrcDir, functionName, "tests_250.txt");
                                            if (!Files.exists(testsFile)) {
                                                System.err.println("Missing tests_250.txt for function: " + functionName);
                                                resultsWriter.write(String.format("%s,%s,%s,%s,%d,%d\n", functionName, methodName, percentage, run, 0, 250));
                                                resultsWriter.flush();
                                                continue;
                                            }

                                            try {
                                                // Führe den Test der synthetisierten Funktion durch
                                                testSynthesizedFunction(functionName, methodName, percentage, run, synthesizedCode, testsFile, resultsWriter);
                                            } catch (IllegalArgumentException e) {
                                                System.err.println("Skipping problematic synthesized function:");
                                                System.err.println("Function: " + functionName + ", Method: " + methodName + ", Percentage: " + percentage + ", Run: " + run);
                                                System.err.println("Error: " + e.getMessage());
                                            } catch (Exception e) {
                                                System.err.println("An unexpected error occurred while testing:");
                                                System.err.println("Function: " + functionName + ", Method: " + methodName + ", Percentage: " + percentage + ", Run: " + run);
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    // Hilfsmethode zum Aufteilen der Argumente aus der Testeingabe
    private static String[] splitArguments(String input) {
        List<String> argsList = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int bracketDepth = 0;
        boolean inQuotes = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            }
            if (!inQuotes) {
                if (c == '[') {
                    bracketDepth++;
                } else if (c == ']') {
                    bracketDepth--;
                }
            }
            if (c == ',' && bracketDepth == 0 && !inQuotes) {
                argsList.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            argsList.add(current.toString().trim());
        }
        return argsList.toArray(new String[argsList.size()]);
    }

    // Testet eine synthetisierte Funktion, indem sie kompiliert, ausgeführt und mit Testfällen validiert wird.
    private static void testSynthesizedFunction(String functionName, String method, String percentage, String run,
                                                String synthesizedCode, Path testsFile, FileWriter resultsWriter) throws IOException {
        String className = "TempFunction"; // Temporärer Klassenname für die synthetisierte Funktion
        String tempFile = className + ".java"; // Java-Datei für die temporäre Funktion

        // Extrahiere nur den Code zwischen der ```java-Markierung und dem abschließenden ```, wenn ChatGPT seine Antwort zwischen ```java und ``` gibt.
        if (synthesizedCode.contains("```java")) {
            int startIndex = synthesizedCode.indexOf("```java") + 7;
            int endIndex = synthesizedCode.lastIndexOf("```");
            if (endIndex > startIndex) {
                synthesizedCode = synthesizedCode.substring(startIndex, endIndex).trim();
            }
        }

        // Ersetze den Platzhalter "function" im Code mit dem echten Funktionsnamen
        String updatedCode = synthesizedCode.replace("function", functionName);
        // Extrahiere die Parameter der Funktion
        String signatureLine = updatedCode.split("\\{")[0].trim();
        String params = signatureLine.substring(signatureLine.indexOf('(') + 1, signatureLine.indexOf(')')).trim();
        String[] paramTypes = params.isEmpty() ? new String[0] : params.split(",");
        // Erstelle die main-Methode zur Laufzeit-Ausführung der Funktion
        StringBuilder mainMethod = new StringBuilder();
        mainMethod.append("    public static void main(String[] args) {\n");
        mainMethod.append("        try {\n");

        if (paramTypes.length > 0) {
            mainMethod.append("            if (args.length < ").append(paramTypes.length).append(") {\n");
            mainMethod.append("                throw new IllegalArgumentException(\"Insufficient arguments provided. Expected ")
                      .append(paramTypes.length).append(", got \" + args.length);\n");
            mainMethod.append("            }\n");
            // Konvertiere die Eingabeparameter in die entsprechenden Typen
            for (int i = 0; i < paramTypes.length; i++) {
                String[] parts = paramTypes[i].trim().split("\\s+");
                String type = parts[0];
                String varName = "param" + i;

                if (type.equals("int")) {
                    mainMethod.append("            int ").append(varName).append(" = Integer.parseInt(args[").append(i).append("]);\n");
                } else if (type.equals("double")) {
                    mainMethod.append("            double ").append(varName).append(" = Double.parseDouble(args[").append(i).append("]);\n");
                } else if (type.equals("int[]")) {
                    mainMethod.append("            int[] ").append(varName)
                              .append(" = args[").append(i).append("].equals(\"[]\") ? new int[0] : ")
                              .append("Arrays.stream(args[").append(i).append("].substring(1, args[").append(i)
                              .append("].length() - 1).split(\",\")")
                              .append(").map(String::trim).mapToInt(Integer::parseInt).toArray();\n");
                } else if (type.equals("double[]")) {
                    mainMethod.append("            double[] ").append(varName)
                              .append(" = args[").append(i).append("].equals(\"[]\") ? new double[0] : ")
                              .append("Arrays.stream(args[").append(i).append("].substring(1, args[").append(i)
                              .append("].length() - 1).split(\",\")")
                              .append(").map(String::trim).mapToDouble(Double::parseDouble).toArray();\n");
                } else if (type.equals("String")) {
                    mainMethod.append("            String ").append(varName).append(" = args[").append(i).append("];\n");
                } else {
                    throw new IllegalArgumentException("Unsupported parameter type: " + type);
                }
            }
            // Rufe die Funktion mit den konvertierten Parametern auf
            mainMethod.append("            System.out.println(").append(functionName).append("(");
            for (int i = 0; i < paramTypes.length; i++) {
                if (i > 0) mainMethod.append(", ");
                mainMethod.append("param").append(i);
            }
            mainMethod.append("));\n");
        } else {
            mainMethod.append("            System.out.println(").append(functionName).append("());\n");
        }

        // Ändere die Fehlerausgabe in System.out, damit sie vom Tester erfasst wird.
        mainMethod.append("        } catch (Exception e) {\n");
        mainMethod.append("            System.out.println(\"Error: \" + e.getMessage());\n");
        mainMethod.append("        }\n");
        mainMethod.append("    }\n");

        // Speichere den generierten Java-Code in einer temporären Datei
        try (FileWriter tempWriter = new FileWriter(tempFile)) {
            tempWriter.write("import java.util.*;\n");
            tempWriter.write("public class " + className + " {\n");
            tempWriter.write(updatedCode);
            tempWriter.write("\n");
            tempWriter.write(mainMethod.toString());
            tempWriter.write("}\n");
        }
        // Kompiliere die temporäre Datei
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int compileResult = compiler.run(null, null, null, tempFile);

        if (compileResult != 0) {
            System.err.println("Failed to compile: " + tempFile);
            resultsWriter.write(String.format("%s,%s,%s,%s,%d,%d\n", functionName, method, percentage, run, 0, 250));
            resultsWriter.flush();
            Files.deleteIfExists(Paths.get(tempFile));
            return;
        }
        // Führe die Tests für die synthetisierte Funktion aus
        int passed = 0, failed = 0;
        List<String> tests = Files.readAllLines(testsFile);
        for (String test : tests) {
            try {
                String input = test.substring(test.indexOf('(') + 1, test.indexOf(") ")).trim();
                String expected = test.split("==")[1].trim().replace("\"", "");

                // Verwende den neuen Argument-Splitter, der Arrays zusammenhält.
                String[] parsedArgs = input.isEmpty() ? new String[0] : splitArguments(input);

                // Erstelle den Befehl zum Starten des Java-Prozesses
                List<String> command = new ArrayList<>();
                command.add("java");
                command.add("-cp");
                command.add(".");
                command.add(className);
                Collections.addAll(command, parsedArgs);

                ProcessBuilder processBuilder = new ProcessBuilder(command);
                Process runTest = processBuilder.start();
                boolean completed = runTest.waitFor(1000, TimeUnit.MILLISECONDS); // Timeout in 8 sec

                if (!completed) {
                    failed++;
                    runTest.destroy();
                    System.out.printf("Test timed out: Input = %s%n", input);
                    continue;
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(runTest.getInputStream()))) {
                    String outputLine = reader.readLine();
                    if (outputLine != null) {
                        try {
                            double expectedValue = Double.parseDouble(expected.replace(",", "."));
                            double actualValue = Double.parseDouble(outputLine.trim());
                            double tolerance = 1e-6;
                            if (Math.abs(expectedValue - actualValue) <= tolerance) {
                                passed++;
                            } else {
                                failed++;
                                System.out.printf("Test failed: Input = %s, Expected = %s, Output = %s%n", input, expected, outputLine);
                            }
                        } catch (NumberFormatException e) {
                            if (outputLine.trim().equals(expected)) {
                                passed++;
                            } else {
                                failed++;
                                System.out.printf("Test failed: Input = %s, Expected = %s, Output = %s%n", input, expected, outputLine);
                            }
                        }
                    } else {
                        failed++;
                        System.out.printf("Test failed: Input = %s, Expected = %s, Output = null%n", input, expected);
                    }
                }
            } catch (Exception e) {
                failed++;
                System.out.printf("Test failed with exception: %s%n", e.getMessage());
            }
        }
        resultsWriter.flush();
        resultsWriter.write(String.format("%s,%s,%s,%s,%d,%d\n", functionName, method, percentage, run, passed, failed));
        Files.deleteIfExists(Paths.get(tempFile));
        Files.deleteIfExists(Paths.get(className + ".class"));
    }
}