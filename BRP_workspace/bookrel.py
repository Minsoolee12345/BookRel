# bookrel.py
# ---------------------------------------------------------
# Project Gutenberg 텍스트(UTF-8) → 장 단위 인물/관계 그래프 생성
# - FastAPI 엔드포인트 + CLI 겸용
# - GraphResponse(JSON): nodes(id,name), edges(src,dst,type,weight,fromChapter,toChapter)
# ---------------------------------------------------------
import re
import json
import uuid
import math
import argparse
from collections import Counter, defaultdict
from typing import List, Dict, Tuple, Optional

import requests
import numpy as np

# spaCy 로딩 (영문 모델)
import spacy
try:
    nlp = spacy.load("en_core_web_sm")
except Exception as e:
    raise RuntimeError(
        "spaCy 모델 'en_core_web_sm'이 필요해요.\n"
        "설치: pip install spacy && python -m spacy download en_core_web_sm"
    ) from e

# ---------- 유틸: Gutenberg 텍스트 전처리/장 분할 ----------

START_MARK = "*** START OF THIS PROJECT GUTENBERG EBOOK"
END_MARK   = "*** END OF THIS PROJECT GUTENBERG EBOOK"

def fetch_text(url: str) -> str:
    r = requests.get(url, timeout=30)
    r.raise_for_status()
    r.encoding = "utf-8"
    return r.text

def strip_gutenberg_boilerplate(text: str) -> str:
    start_idx = text.find(START_MARK)
    end_idx = text.find(END_MARK)
    if start_idx != -1:
        text = text[start_idx + len(START_MARK):]
    if end_idx != -1:
        text = text[:end_idx]
    return text.strip()

CHAPTER_RE = re.compile(r'^\s*(CHAPTER|Chapter)\s+([IVXLCDM]+|\d+)\b.*$', re.MULTILINE)

def split_into_chapters(text: str) -> List[str]:
    """
    'Chapter 1' / 'CHAPTER I' 같은 헤더를 기준으로 분할.
    헤더 앞 내용은 프롤로그로 간주(있으면 챕터 0 취급).
    """
    parts = []
    indices = [m.start() for m in CHAPTER_RE.finditer(text)]
    if not indices:
        # 장 헤더가 없다면 전체를 1장으로
        return [text.strip()]

    # 프롤로그
    first_idx = indices[0]
    prologue = text[:first_idx].strip()
    if prologue:
        parts.append(prologue)

    # 각 장
    for i, idx in enumerate(indices):
        end = indices[i+1] if i+1 < len(indices) else len(text)
        chunk = text[idx:end].strip()
        parts.append(chunk)
    return parts

# ---------- 인물 추출/정규화 & 동시출현 계산 ----------

def normalize_person(name: str) -> str:
    """ 간단 정규화: 공백/줄바꿈/연속 공백 정리 """
    name = re.sub(r'\s+', ' ', name.strip())
    return name

def extract_persons(chapter_text: str) -> List[str]:
    doc = nlp(chapter_text)
    persons = [normalize_person(ent.text) for ent in doc.ents if ent.label_ == "PERSON"]
    return persons

def canonicalize_names(all_names: List[str]) -> List[str]:
    """
    간단한 별칭 병합:
      - 다단어 이름의 '마지막 단어(성)' 빈도를 집계
      - 단어 하나짜리 이름이 특정 성(유일)과 일치하면 병합 (예: 'Darcy' ↔ 'Mr. Darcy')
      - 성이 모호(여러 사람 공유)하면 병합하지 않음
    """
    multi = [n for n in all_names if len(n.split()) >= 2]
    last_names = [n.split()[-1] for n in multi]
    last_name_counts = Counter([ln for ln in last_names if ln.istitle()])

    # 대표 표기 선택: 다단어 이름은 그대로, 단어 하나짜리는 그대로 두되,
    # 나중에 페어 계산 시 매핑 테이블 사용
    return all_names, last_name_counts

def alias_map_from_counts(names: List[str], last_name_counts: Counter) -> Dict[str, str]:
    """
    단어 하나짜리 토큰이 '유일한 성(last name)'과 일치하면 그 다단어 이름으로 매핑
    예: 'Darcy' -> 'Mr. Darcy' (성 'Darcy'를 가진 다단어 이름이 한 명뿐일 때)
    """
    multi = [n for n in names if len(n.split()) >= 2]
    # 성 -> 대표 전체 이름(첫 번째)
    last_to_full = {}
    for n in multi:
        ln = n.split()[-1]
        if last_name_counts.get(ln, 0) == 1 and ln not in last_to_full:
            last_to_full[ln] = n

    amap = {}
    for n in names:
        toks = n.split()
        if len(toks) == 1:
            ln = toks[0]
            if last_name_counts.get(ln, 0) == 1 and ln in last_to_full:
                amap[n] = last_to_full[ln]
    return amap

def pairs_from_sentence(persons_in_sent: List[str]) -> List[Tuple[str, str]]:
    pairs = []
    uniq = sorted(set(persons_in_sent))
    for i in range(len(uniq)):
        for j in range(i+1, len(uniq)):
            pairs.append((uniq[i], uniq[j]))
    return pairs

def chapter_cooccurrence(chapter_text: str, alias_map: Dict[str, str]) -> Tuple[Counter, Counter]:
    """
    문장 단위로 PERSON 동시출현 집계.
    returns:
      - name_counts: 인물 출현 수
      - pair_counts: 페어 동시출현 수
    """
    doc = nlp(chapter_text)
    name_counts = Counter()
    pair_counts = Counter()

    for sent in doc.sents:
        persons = [normalize_person(ent.text) for ent in sent.ents if ent.label_ == "PERSON"]
        if not persons:
            continue

        # 별칭 매핑 적용
        persons = [alias_map.get(p, p) for p in persons]
        for p in persons:
            name_counts[p] += 1
        for a, b in pairs_from_sentence(persons):
            pair_counts[(a, b)] += 1
    return name_counts, pair_counts

# ---------- 그래프 구성 ----------

def build_graph_from_chapters(book_id: int, chapters: List[str]) -> Dict:
    """
    장(List[str])을 받아 그래프 JSON(dict) 생성.
    - 노드 id = uuid4 문자열
    - 엣지 type = "CO_OCCUR"
    - weight = 페어 동시출현 수를 0~1 정규화
    - fromChapter, toChapter = (1부터 시작) 최초/마지막 동시출현 장
    """
    # 1차 스캔: 전체 인물명 수집
    all_names = []
    for ch in chapters:
        all_names.extend(extract_persons(ch))
    all_names = [normalize_person(n) for n in all_names if n.strip()]

    _, last_counts = canonicalize_names(all_names)
    amap = alias_map_from_counts(all_names, last_counts)

    # 2차 스캔: 챕터별 동시출현 집계
    chapter_pair_counts: List[Counter] = []
    chapter_name_counts: List[Counter] = []
    for ch in chapters:
        ncnt, pcnt = chapter_cooccurrence(ch, amap)
        chapter_name_counts.append(ncnt)
        chapter_pair_counts.append(pcnt)

    # 전역 합계
    total_pair = Counter()
    first_seen = {}
    last_seen = {}
    for idx, pcnt in enumerate(chapter_pair_counts, start=1):  # chapter index 1-based
        for pair, c in pcnt.items():
            total_pair[pair] += c
            if pair not in first_seen:
                first_seen[pair] = idx
            last_seen[pair] = idx

    if not total_pair:
        return {"nodes": [], "edges": []}

    max_count = max(total_pair.values())

    # 노드 집합
    people = set()
    for ncnt in chapter_name_counts:
        people.update(ncnt.keys())

    # 노드 id 부여(고정 위해 이름→UUIDv5도 가능, 여기선 간단히 uuid4)
    name_to_id = {name: str(uuid.uuid4()) for name in sorted(people)}

    nodes = [{"id": name_to_id[name], "name": name} for name in sorted(people)]

    # 엣지 구성
    edges = []
    for (a, b), cnt in total_pair.items():
        w = float(cnt) / float(max_count) if max_count > 0 else 0.0
        edges.append({
            "src": name_to_id[a],
            "dst": name_to_id[b],
            "type": "CO_OCCUR",
            "weight": round(w, 4),
            "fromChapter": first_seen[(a, b)],
            "toChapter": last_seen[(a, b)]
        })

    return {"nodes": nodes, "edges": edges}

# ---------- FastAPI ----------

from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="BookRel NLP", version="0.1.0")

class IngestURL(BaseModel):
    bookId: int
    url: str

class IngestText(BaseModel):
    bookId: int
    text: str

@app.post("/ingest/url")
def ingest_url(req: IngestURL):
    raw = fetch_text(req.url)
    body = strip_gutenberg_boilerplate(raw)
    chapters = split_into_chapters(body)
    graph = build_graph_from_chapters(req.bookId, chapters)
    return graph

@app.post("/ingest/text")
def ingest_text(req: IngestText):
    body = strip_gutenberg_boilerplate(req.text) if (START_MARK in req.text and END_MARK in req.text) else req.text
    chapters = split_into_chapters(body)
    graph = build_graph_from_chapters(req.bookId, chapters)
    return graph

# ---------- CLI ----------

def cli():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", type=str, required=False, help="Project Gutenberg plain text UTF-8 URL")
    parser.add_argument("--bookId", type=int, default=1)
    parser.add_argument("--out", type=str, default=None)
    args = parser.parse_args()

    if args.url:
        raw = fetch_text(args.url)
    else:
        raise SystemExit("예: python bookrel.py --url https://www.gutenberg.org/ebooks/1342.txt.utf-8 --bookId 1 --out graph.json")

    body = strip_gutenberg_boilerplate(raw)
    chapters = split_into_chapters(body)
    graph = build_graph_from_chapters(args.bookId, chapters)

    if args.out:
        with open(args.out, "w", encoding="utf-8") as f:
            json.dump(graph, f, ensure_ascii=False)
        print(f"saved: {args.out}")
    else:
        print(json.dumps(graph, ensure_ascii=False))

if __name__ == "__main__":
    # CLI로 실행하면 그래프를 파일로 저장/출력
    # 서버로 띄우려면: uvicorn bookrel:app --host 0.0.0.0 --port 8001
    cli()