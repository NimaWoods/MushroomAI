import os

import requests
from huggingface_hub import login
from tqdm import tqdm
from transformers import AutoModelForCausalLM, AutoTokenizer


def huggingface_login():
    token = "YOUR_HUGGING" # TODO Get by Config
    if token is None:
        raise ValueError("Hugging Face token is missing. Set it as an environment variable (HF_TOKEN).")
    login(token=token)

# Function to download files with a progress bar
def download_with_progress(url, destination):
    response = requests.get(url, stream=True)
    if response.status_code != 200:
        print(f"Error downloading {url}")
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

# Function to download and save the model and tokenizer
def download_model(model_name="mistralai/Mistral-7B-v0.3", save_directory="server/src/main/java/server/model/"):
    print(f"Downloading model {model_name}...")
    os.makedirs(save_directory, exist_ok=True)

    try:
        # Download tokenizer with caching to the specified directory
        print("Downloading Tokenizer...")
        tokenizer = AutoTokenizer.from_pretrained(model_name, cache_dir=save_directory, local_files_only=False)
        tokenizer.save_pretrained(save_directory)
        print("Tokenizer downloaded and saved successfully.")

        # Download model with caching to the specified directory
        print("Downloading Model...")
        model = AutoModelForCausalLM.from_pretrained(
            model_name,
            cache_dir=save_directory,
            local_files_only=False
        )
        model.save_pretrained(save_directory)
        print("Model downloaded and saved successfully.")
    except Exception as e:
        print(f"Error downloading the model or tokenizer: {e}")

if __name__ == "__main__":
    try:
        huggingface_login()
        download_model()
    except Exception as e:
        print(f"An error occurred: {e}")
