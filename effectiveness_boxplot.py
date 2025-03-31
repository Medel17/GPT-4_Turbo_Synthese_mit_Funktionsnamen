import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import matplotlib.ticker as mtick

# Einlesen der CSV-Datei und Umwandlung relevanter Spalten in numerische Werte
df = pd.read_csv("test_results.csv")
df["Percentage"] = pd.to_numeric(df["Percentage"], errors="coerce")
df["Passed"] = pd.to_numeric(df["Passed"], errors="coerce")
df["Failed"] = pd.to_numeric(df["Failed"], errors="coerce")

# Berechnung der Verifikationsquote
df["VerificationPassed"] = (df["Passed"] / (df["Passed"] + df["Failed"])) * 100

# Berechnung des mittleren Werts von VerificationPassed für jede Funktion, Methode und Prozent
grouped_means = df.groupby(["Function", "Method", "Percentage"])["VerificationPassed"].mean().reset_index()

# Separierung der Daten anhand der verwendeten Methoden
baseline_data = grouped_means[grouped_means["Method"] == "baseline"]
decomposition_data = grouped_means[grouped_means["Method"] == "decomposition"]

# Definition der gewünschten x-Achsen-Werte und entsprechende Labels
desired_ticks = [25, 37, 50, 62, 75, 87, 100]
desired_tick_labels = [f"{tick}%" for tick in desired_ticks]
palette = sns.color_palette("husl", len(desired_ticks))

# Erstellung des Boxplots für die Baseline-Methode
plt.figure(figsize=(10, 6))
ax = sns.boxplot(
    x="Percentage", 
    y="VerificationPassed", 
    data=baseline_data, 
    order=desired_ticks,
    hue="Percentage",
    palette=palette, 
    dodge=False,
    linewidth=1.2 
)
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.legend_.remove()   
ax.set_xticklabels(desired_tick_labels)
ax.yaxis.set_major_formatter(mtick.PercentFormatter(xmax=100))

plt.title("Effektivität der Baseline-Methode bei der Synthese", fontsize=16)
plt.xlabel("Synthese-Beispiele", fontsize=14, labelpad=10)
plt.ylabel("Verifikation bestanden", fontsize=14)
plt.ylim(0, 100)
plt.savefig("baseline_boxplot.png")
print("Der Boxplot wurde als baseline_boxplot.png gespeichert")
plt.show()

# Erstellung des Boxplots für die Dekompositionelle Methode
plt.figure(figsize=(10, 6))
ax = sns.boxplot(
    x="Percentage", 
    y="VerificationPassed", 
    data=decomposition_data, 
    order=desired_ticks,
    hue="Percentage",
    palette=palette, 
    dodge=False,
    linewidth=1.2
)
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)

ax.legend_.remove()
ax.set_xticklabels(desired_tick_labels)
ax.yaxis.set_major_formatter(mtick.PercentFormatter(xmax=100))
plt.title("Effektivität der Dekompositionelle-Methode bei der Synthese", fontsize=16)
plt.xlabel("Synthese-Beispiele", fontsize=14)
plt.ylabel("Verifikation bestanden", fontsize=14)
plt.ylim(0, 100)
plt.savefig("decomposition_boxplot.png")
print("Der Boxplot wurde als decomposition_boxplot.png gespeichert")
plt.show()