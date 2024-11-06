import os
import socket
import torch
from transformers import AutoTokenizer, AutoModelForCausalLM
import logging

# Set up offline mode
os.environ["TRANSFORMERS_OFFLINE"] = "1"

# Set up logging
logging.basicConfig(level=logging.INFO)

class AIServer:
    def __init__(self, model_directory="../model"):
        if self.internet_accessible():
            logging.warning("Active internet connection detected. Ensure firewall restrictions if needed.")

        logging.info("Loading model...")
        self.tokenizer = AutoTokenizer.from_pretrained(model_directory, local_files_only=True)
        self.model = AutoModelForCausalLM.from_pretrained(model_directory, local_files_only=True).to(
            "cuda" if torch.cuda.is_available() else "cpu")
        logging.info("Model loaded!")

    @staticmethod
    def internet_accessible():
        try:
            socket.create_connection(("1.1.1.1", 53), timeout=1)
            return True
        except OSError:
            return False

    def generate_response(self, prompt, max_length=100):
        try:
            inputs = self.tokenizer(prompt, return_tensors="pt").to(self.model.device)
            outputs = self.model.generate(**inputs, max_length=max_length, pad_token_id=self.tokenizer.eos_token_id)
            return self.tokenizer.decode(outputs[0], skip_special_tokens=True)
        except Exception as e:
            logging.error(f"Error generating response: {e}")
            return "An error occurred while generating the response."

if __name__ == "__main__":
    server = AIServer()
    while True:
        prompt = input("Enter a prompt: ")
        print(server.generate_response(prompt))
