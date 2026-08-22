from collections import Counter
from fastapi import FastAPI
from natasha import (
    Segmenter,
    NewsEmbedding,
    NewsMorphTagger,
    NewsSyntaxParser,
    NewsNERTagger,
    MorphVocab,
    Doc,
)
from pydantic import BaseModel

app = FastAPI(title="Natasha Text Analysis Service")
segmenter = Segmenter()
embedding = NewsEmbedding()
morph_tagger = NewsMorphTagger(embedding)
syntax_parser = NewsSyntaxParser(embedding)
ner_tagger = NewsNERTagger(embedding)
morph_vocab = MorphVocab()


class TextRequest(BaseModel):
    text: str


def extract_entities(doc):
    result = []

    for span in doc.spans:

        if span.type not in {
            "PER",
            "ORG",
            "LOC",
        }:
            continue

        try:
            span.normalize(morph_vocab)
        except Exception:
            pass

        result.append({
            "text": span.text,
            "normalized": (
                span.normal
                if span.normal
                else span.text.lower()
            ),
            "type": span.type
        })

    return result


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


def analyze(text):
    doc = Doc(text)

    doc.segment(segmenter)

    doc.tag_morph(morph_tagger)

    doc.parse_syntax(syntax_parser)

    doc.tag_ner(ner_tagger)

    entities = extract_entities(doc)

    nouns = extract_nouns(doc)

    return {
        "entities": entities,
        "nouns": nouns
    }


@app.get("/health")
def health():
    return {
        "status": "ok"
    }


@app.post("/analyze")
def analyze_endpoint(request: TextRequest):
    return analyze(request.text)


@app.post("/debug")
def debug_endpoint(request: TextRequest):
    doc = Doc(request.text)
    doc.segment(segmenter)
    doc.tag_morph(morph_tagger)
    doc.parse_syntax(syntax_parser)
    doc.tag_ner(ner_tagger)

    tokens = []

    for token in doc.tokens:
        morph_vocab.lemmatize(
            token
        )

        tokens.append({
            "text": token.text,
            "lemma": token.lemma,
            "pos": token.pos,
            "rel": token.rel,
            "head_id": token.head_id,
            "start": token.start,
            "stop": token.stop
        })

    return {
        "tokens": tokens
    }
