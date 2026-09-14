from fastapi import FastAPI
from natasha import (
    Segmenter,
    NewsEmbedding,
    NewsMorphTagger,
    NewsSyntaxParser,
    MorphVocab,
    Doc,
)
from pydantic import BaseModel

app = FastAPI(title="Natasha Text Analysis Service")
segmenter = Segmenter()
embedding = NewsEmbedding()
morph_tagger = NewsMorphTagger(embedding)
syntax_parser = NewsSyntaxParser(embedding)
morph_vocab = MorphVocab()

class TextRequest(BaseModel):
    text: str

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

def analyze(text):
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

@app.post("/analyze")
def analyze_endpoint(request: TextRequest):
    return analyze(request.text)

@app.post("/lemmas")
def lemmas_endpoint(request: TextRequest):
    return get_lemmas(request.text)