from transformers import AutoModelForCausalLM, AutoTokenizer
from tqdm import tqdm
import requests
import os
from huggingface_hub import login

# Login bei Hugging Face
login(token="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")

# Funktion zum Herunterladen mit Fortschrittsanzeige
def download_with_progress(url, destination):
    response = requests.get(url, stream=True)
    if response.status_code != 200:
        print(f"Fehler beim Herunterladen von {url}")
        return False

    total_size = int(response.headers.get('content-length', 0))
    with open(destination, 'wb') as file, tqdm(
            desc=destination,
            total=total_size,
            unit='B',
            unit_scale=True,
            unit_divisor=1024,
    ) as bar:
        for data in response.iter_content(chunk_size=1024):
            file.write(data)
            bar.update(len(data))
    return True

# Modell und Tokenizer herunterladen und speichern
def download_model(model_name="mistralai/Mistral-7B-v0.3", save_directory="./model"):
    print(f"Lade das Modell {model_name} herunter...")
    os.makedirs(save_directory, exist_ok=True)

    try:
        # Tokenizer herunterladen und speichern
        tokenizer = AutoTokenizer.from_pretrained(model_name)
        tokenizer.save_pretrained(save_directory)
        print("Tokenizer erfolgreich heruntergeladen und gespeichert.")

        # Modell mit Fortschrittsanzeige herunterladen und speichern
        model = AutoModelForCausalLM.from_pretrained(
            model_name,
            cache_dir=save_directory
        )
        model.save_pretrained(save_directory)
        print("Download abgeschlossen und Modell gespeichert.")
    except Exception as e:
        print(f"Fehler beim Herunterladen des Modells oder Tokenizers: {e}")

if __name__ == "__main__":
    download_model()
