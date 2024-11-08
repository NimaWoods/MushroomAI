import torch
from datasets import load_dataset
from transformers import AutoTokenizer, AutoModelForCausalLM, Trainer, TrainingArguments

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

model_name = "distilgpt2"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForCausalLM.from_pretrained(model_name).to(device)

dataset = load_dataset("ParlAI/blended_skill_talk")

def tokenize_function(examples):
    texts = []
    if "text" in examples:
        texts = examples["text"]
    elif "dialog" in examples:
        texts = [" ".join(dialog) if isinstance(dialog, list) else str(dialog) for dialog in examples["dialog"]]

    texts = [str(text) for text in texts if isinstance(text, (str, list))]

    return tokenizer(texts, padding="max_length", truncation=True, max_length=128)

tokenized_dataset = dataset.map(tokenize_function, batched=True, batch_size=100)

training_args = TrainingArguments(
    output_dir="./results",
    evaluation_strategy="epoch",
    learning_rate=2e-5,
    per_device_train_batch_size=2,
    per_device_eval_batch_size=2,
    num_train_epochs=3,
    weight_decay=0.01,
)

# Trainer
trainer = Trainer(
    model=model,
    args=training_args,
    train_dataset=tokenized_dataset["train"],
    eval_dataset=tokenized_dataset["test"],
)

# Train the model
trainer.train()

# Save the model
model.save_pretrained("./trained_model")
tokenizer.save_pretrained("./trained_model")
