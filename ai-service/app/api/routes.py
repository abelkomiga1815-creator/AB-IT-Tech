"""Research endpoints. Every route needs the service API key; none keeps any user data."""
from __future__ import annotations

from fastapi import APIRouter, Depends, Response

from app.api.deps import get_pipeline, require_api_key
from app.api.schemas import ProjectInput
from app.core.serialization import to_api
from app.pipeline import ResearchPipeline
from app.reports.builder import build_report
from app.reports.markdown import render_markdown

router = APIRouter(prefix="/api/v1/research", dependencies=[Depends(require_api_key)], tags=["research"])


def ok(data: object) -> dict:
    return {"success": True, "data": data}


@router.post("/keywords")
async def keywords(body: ProjectInput) -> dict:
    """Key terms and the search queries derived from them. Makes no external calls."""
    found, queries = ResearchPipeline.extract(body.title, body.abstract)
    return ok({"keywords": to_api(found), "queries": queries})


@router.post("/related-work")
async def related_work(body: ProjectInput, pipeline: ResearchPipeline = Depends(get_pipeline)) -> dict:
    result = await pipeline.analyze(body.title, body.abstract, body.max_results)
    return ok(build_report(result)["retrieved"])


@router.post("/similarity")
async def similarity(body: ProjectInput, pipeline: ResearchPipeline = Depends(get_pipeline)) -> dict:
    report = build_report(await pipeline.analyze(body.title, body.abstract, body.max_results))
    return ok({
        "similarity": report["analysis"]["similarity"],
        "sources": report["retrieved"]["sources"],
        "notices": report["notices"],
    })


@router.post("/gaps")
async def gaps(body: ProjectInput, pipeline: ResearchPipeline = Depends(get_pipeline)) -> dict:
    report = build_report(await pipeline.analyze(body.title, body.abstract, body.max_results))
    return ok({
        "gapAnalysis": report["analysis"]["gapAnalysis"],
        "sources": report["retrieved"]["sources"],
        "notices": report["notices"],
    })


@router.post("/titles")
async def titles(body: ProjectInput, pipeline: ResearchPipeline = Depends(get_pipeline)) -> dict:
    report = build_report(await pipeline.analyze(body.title, body.abstract, body.max_results))
    return ok({
        "alternativeTitles": report["analysis"]["alternativeTitles"],
        "relatedTerms": report["analysis"]["relatedTerms"],
        "sources": report["retrieved"]["sources"],
    })


@router.post("/report")
async def report_json(body: ProjectInput, pipeline: ResearchPipeline = Depends(get_pipeline)) -> dict:
    return ok(build_report(await pipeline.analyze(body.title, body.abstract, body.max_results)))


@router.post("/report/markdown")
async def report_markdown(body: ProjectInput, pipeline: ResearchPipeline = Depends(get_pipeline)) -> Response:
    report = build_report(await pipeline.analyze(body.title, body.abstract, body.max_results))
    return Response(content=render_markdown(report), media_type="text/markdown; charset=utf-8")
