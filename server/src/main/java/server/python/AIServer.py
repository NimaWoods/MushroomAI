import logging
import os
import socket

import torch
from transformers import AutoTokenizer, AutoModelForCausalLM, LlamaTokenizer

# Set up model directory path
model_directory = "C:/Users/NIFA/IdeaProjects/MushroomAI/server/src/main/java/server/model/"

# Set up offline mode
os.environ["TRANSFORMERS_OFFLINE"] = "1"

# Set up logging (reduce to WARNING level to reduce logging overhead)
logging.basicConfig(level=logging.WARNING)

class AIServer:
    def __init__(self):
        if self.internet_accessible():
            logging.warning("Active internet connection detected. Ensure firewall restrictions if needed.")

        logging.info("Loading model...")

        try:
            self.tokenizer = LlamaTokenizer.from_pretrained(model_directory, local_files_only=True)
            logging.info("LlamaTokenizer loaded successfully.")
        except Exception as e:
            logging.error(f"Failed to load LlamaTokenizer: {e}")
            try:
                # Fall back to AutoTokenizer if LlamaTokenizer is not compatible
                self.tokenizer = AutoTokenizer.from_pretrained(model_directory, local_files_only=True)
                logging.info("AutoTokenizer loaded successfully.")
            except Exception as e:
                logging.critical(f"Error loading tokenizer: {e}")
                raise

        # Load the model with compatibility checks
        try:
            self.model = AutoModelForCausalLM.from_pretrained(model_directory, local_files_only=True).to(
                "cuda" if torch.cuda.is_available() else "cpu"
            )
            # Convert model to half-precision if using GPU
            if torch.cuda.is_available():
                self.model = self.model.half()
                torch.backends.cudnn.benchmark = True
            logging.info("Model loaded successfully!")
        except Exception as e:
            logging.critical(f"Error loading model: {e}")
            raise

    @staticmethod
    def internet_accessible():
        """Check for internet access to prevent any online downloads in offline mode."""
        try:
            socket.create_connection(("1.1.1.1", 53), timeout=1)
            return True
        except OSError:
            return False

    def generate_response(self, prompt, max_length=100):
        """Generate a response based on the given prompt using the loaded model."""
        try:
            # Tokenize input and move tensors to the model's device
            inputs = self.tokenizer(prompt, return_tensors="pt", truncation=True, max_length=512).to(self.model.device)
            outputs = self.model.generate(
                **inputs,
                max_length=max_length,
                pad_token_id=self.tokenizer.eos_token_id,
                do_sample=False  # Use greedy decoding for faster response
            )
            return self.tokenizer.decode(outputs[0], skip_special_tokens=True)
        except Exception as e:
            logging.error(f"Error generating response: {e}")
            return "An error occurred while generating the response."

if __name__ == "__main__":
    try:
        server = AIServer()
        while True:
            prompt = input("Enter a prompt: ")
            print(server.generate_response(prompt))
    except Exception as e:
        logging.critical("AIServer initialization failed. Please check the configuration and model files.")
