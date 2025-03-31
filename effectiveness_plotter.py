import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

# Einlesen der CSV-Datei und Umwandlung relevanter Spalten in numerische Werte
df = pd.read_csv("test_results.csv")

df["Percentage"] = pd.to_numeric(df["Percentage"], errors="coerce")
df["Passed"] = pd.to_numeric(df["Passed"], errors="coerce")
df["Failed"] = pd.to_numeric(df["Failed"], errors="coerce")

# Berechne VerificationPassed für jede Zeile
df["VerificationPassed"] = (df["Passed"] / (df["Passed"] + df["Failed"])) * 100

# Berechnung des mittleren Werts von VerificationPassed für jede Funktion, Methode und Prozent
grouped_means = df.groupby(["Function", "Method", "Percentage"])["VerificationPassed"].mean().reset_index()

# Berechnung des aggregierten Mittelwerts über alle Funktionen für jede Methode und jeden Prozentsatz
final_means = grouped_means.groupby(["Method", "Percentage"])["VerificationPassed"].mean().reset_index()

# Visualisierung der Ergebnisse
plt.figure(figsize=(10, 6))
for method in final_means["Method"].unique():
    method_data = final_means[final_means["Method"] == method]
    plt.plot(method_data["Percentage"], method_data["VerificationPassed"], marker='o', label=method)

plt.title("Evaluierungsergebnisse der Effektivität nach Methode", fontsize=16)
plt.xlabel("Synthese-Beispiele (%)", fontsize=14)
plt.ylabel("Verifikation bestanden (%)", fontsize=14)
plt.xticks(sorted(df["Percentage"].unique()))
plt.ylim(0, 100)
plt.legend(title="Methode")
plt.grid(axis='y')
sns.despine()
plt.savefig("effectiveness_plot.png")
print("Liniendiagramm als effectiveness_plot.png gespeichert.")
plt.show()