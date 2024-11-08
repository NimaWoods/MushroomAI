from datasets import load_dataset, DatasetDict, concatenate_datasets
from transformers import AutoModelForCausalLM, AutoTokenizer, Trainer, TrainingArguments


def load_and_prepare_datasets():
    print("Lade Datensätze...")

    daily_dialog = load_dataset("li2017dailydialog/daily_dialog", trust_remote_code=True)['train']
    blended_skill_talk = load_dataset("ParlAI/blended_skill_talk")['train']

    combined_train_dataset = concatenate_datasets([daily_dialog, blended_skill_talk])

    combined_dataset = DatasetDict({
        'train': combined_train_dataset
    })
    return combined_dataset

def train_model(model_directory="C:/Users/NIFA/IdeaProjects/MushroomAI/server/src/main/java/server/model/"):
    print("Lade Modell und Tokenizer...")
    tokenizer = AutoTokenizer.from_pretrained(model_directory)
    model = AutoModelForCausalLM.from_pretrained(model_directory)

    if tokenizer.pad_token is None:
        tokenizer.pad_token = tokenizer.eos_token

    dataset = load_and_prepare_datasets()

    def tokenize_function(examples):
        # Tokenize the text
        texts = [" ".join(dialog) if isinstance(dialog, list) else dialog for dialog in examples["dialog"]]
        tokenized_inputs = tokenizer(texts, padding="max_length", truncation=True, max_length=128)

        # Set `labels` equal to `input_ids` for causal language modeling
        tokenized_inputs["labels"] = tokenized_inputs["input_ids"].copy()
        return tokenized_inputs

    tokenized_dataset = dataset.map(tokenize_function, batched=True, batch_size=100)

    training_args = TrainingArguments(
        output_dir="./results",
        eval_strategy="epoch",
        save_strategy="epoch",
        per_device_train_batch_size=2,
        num_train_epochs=3,
        save_steps=10,
        save_total_limit=2,
        load_best_model_at_end=True,
        gradient_accumulation_steps=4
    )

    eval_dataset = load_dataset("ParlAI/blended_skill_talk", split="validation")

    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=tokenized_dataset["train"],
        eval_dataset=eval_dataset
    )

    print("Starte Training...")
    trainer.train()
    model.save_pretrained(model_directory)
    tokenizer.save_pretrained(model_directory)
    print("Training abgeschlossen und Modell gespeichert.")

if __name__ == "__main__":
    train_model()
