from transformers import AutoModelForCausalLM, AutoTokenizer, Trainer, TrainingArguments
from datasets import load_dataset
import sys

project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '../../../'))
sys.path.insert(0, project_root)

def train_model(model_directory="./models/llama_model"):
    print("Modell und Tokenizer laden...")
    tokenizer = AutoTokenizer.from_pretrained(model_directory)
    model = AutoModelForCausalLM.from_pretrained(model_directory)

    # Beispiel-Datensatz für das Training laden (dieser muss angepasst werden)
    dataset = load_dataset("yelp_review_full", split="train[:1%]")

    def tokenize_function(examples):
        return tokenizer(examples["text"], padding="max_length", truncation=True)

    tokenized_datasets = dataset.map(tokenize_function, batched=True)

    training_args = TrainingArguments(
        output_dir="./results",
        evaluation_strategy="epoch",
        per_device_train_batch_size=1,
        num_train_epochs=1,
        save_steps=10,
        save_total_limit=2,
    )

    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=tokenized_datasets,
    )

    print("Starte Training...")
    trainer.train()
    model.save_pretrained(model_directory)
    tokenizer.save_pretrained(model_directory)
    print("Training abgeschlossen und Modell gespeichert.")

if __name__ == "__main__":
    train_model()
