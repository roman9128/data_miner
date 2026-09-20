from fastapi import FastAPI
from pydantic import BaseModel

from natasha import (
    Segmenter,
    NewsEmbedding,
    NewsMorphTagger,
    NewsSyntaxParser,
    MorphVocab,
    Doc,
)

from sentence_transformers import SentenceTransformer


app = FastAPI(title="Telegram Data Miner AI Service")

segmenter = Segmenter()
embedding = NewsEmbedding()
morph_tagger = NewsMorphTagger(embedding)
syntax_parser = NewsSyntaxParser(embedding)
morph_vocab = MorphVocab()

embedding_model = SentenceTransformer("deepvk/USER2-small")

class TextRequest(BaseModel):
    text: str

class EmbedRequest(BaseModel):
    inputs: list[str]

def extract_nouns(doc):
    nouns = {}
    for token in doc.tokens:
        if token.pos not in {"NOUN", "PROPN"}:
            continue
        token.lemmatize(morph_vocab)
        lemma = token.lemma
        if not lemma:
            continue
        lemma = lemma.strip().lower()
        if len(lemma) < 2:
            continue
        if lemma not in nouns:
            nouns[lemma] = {
                "lemma": lemma,
                "text": token.text,
                "count": 1
            }
        else:
            nouns[lemma]["count"] += 1
    return list(nouns.values())


def extract_lemmas(doc):
    lemmas = []
    for token in doc.tokens:
        token.lemmatize(morph_vocab)
        lemma = token.lemma
        if not lemma:
            continue
        lemma = lemma.strip().lower()
        if len(lemma) < 2:
            continue
        lemmas.append(lemma)
    return lemmas


def get_nouns(text):
    doc = Doc(text)
    doc.segment(segmenter)
    doc.tag_morph(morph_tagger)
    doc.parse_syntax(syntax_parser)
    nouns = extract_nouns(doc)
    return {
        "nouns": nouns
    }

def get_lemmas(text):
    doc = Doc(text)
    doc.segment(segmenter)
    doc.tag_morph(morph_tagger)
    return {
        "lemmas": extract_lemmas(doc)
    }

@app.get("/health")
def health():
    return {
        "status": "ok"
    }

@app.post("/nouns")
def nouns_endpoint(request: TextRequest):
    return get_nouns(request.text)

@app.post("/lemmas")
def lemmas_endpoint(request: TextRequest):
    return get_lemmas(request.text)

@app.post("/embed")
def embed(request: EmbedRequest):
    embeddings = embedding_model.encode(request.inputs)
    return embeddings.tolist()