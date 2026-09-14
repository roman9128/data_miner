from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

app = FastAPI()

model = SentenceTransformer("deepvk/USER2-small")


class EmbedRequest(BaseModel):
    inputs: list[str]


@app.get("/health")
def health():
    return {
        "status": "ok"
    }


@app.post("/embed")
def embed(request: EmbedRequest):
    embeddings = model.encode(request.inputs)

    return embeddings.tolist()
