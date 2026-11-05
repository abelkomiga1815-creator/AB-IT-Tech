"""FastAPI application factory.

Run:  uvicorn app.main:create_app --factory --port 8000
"""
from __future__ import annotations

import logging
import re
import time
import uuid
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException
from starlette.responses import PlainTextResponse

from app.api.routes import router
from app.api.auth import router as auth_router
from app.core.cache import TTLCache
from app.core.config import Settings
from app.core.errors import ApiError, error_body
from app.core.logging_config import configure_logging
from app.pipeline import ResearchPipeline
from app.search.http import HttpxClient
from app.search.providers import default_providers
from app.search.service import SearchService

log = logging.getLogger(__name__)
_SAFE_REQUEST_ID = re.compile(r"^[A-Za-z0-9._-]{8,64}$")
_HTTP_ERRORS = {404: ("RESOURCE_NOT_FOUND", "The requested resource was not found"),
                405: ("METHOD_NOT_ALLOWED", "The HTTP method is not supported for this resource")}


def create_app(settings: Settings | None = None, search_service: SearchService | None = None) -> FastAPI:
    """`search_service` can be injected (tests do this); otherwise the real providers are used."""
    settings = settings or Settings.from_env()
    configure_logging(settings.log_level)

    http_client = None
    if search_service is None:
        http_client = HttpxClient()
        cache: TTLCache = TTLCache(settings.cache_ttl_seconds, settings.cache_max_entries)
        search_service = SearchService(default_providers(http_client, settings), cache, settings.request_timeout_seconds)

    @asynccontextmanager
    async def lifespan(_: FastAPI):
        yield
        if http_client is not None:
            await http_client.aclose()

    docs = "/docs" if settings.enable_docs else None
    app = FastAPI(
        title="AB IT TECH Research & Innovation Engine",
        version="0.1.0",
        lifespan=lifespan,
        docs_url=docs,
        redoc_url=None,
        openapi_url="/openapi.json" if settings.enable_docs else None,
    )
    app.state.settings = settings
    app.state.pipeline = ResearchPipeline(search_service)

    @app.middleware("http")
    async def request_context(request: Request, call_next):
        incoming = request.headers.get("x-request-id", "")
        request_id = incoming if _SAFE_REQUEST_ID.match(incoming) else str(uuid.uuid4())
        started = time.perf_counter()
        response = await call_next(request)
        response.headers["X-Request-Id"] = request_id
        elapsed = int((time.perf_counter() - started) * 1000)
        level = logging.DEBUG if request.url.path == "/health" else logging.INFO
        # path only: no query string, headers or body are ever logged
        log.log(level, "%s %s -> %s (%d ms) [%s]", request.method, request.url.path, response.status_code, elapsed, request_id)
        return response

    @app.exception_handler(ApiError)
    async def handle_api_error(_: Request, exc: ApiError) -> JSONResponse:
        return JSONResponse(error_body(exc.code, exc.message), status_code=exc.status_code)

    @app.exception_handler(RequestValidationError)
    async def handle_validation(_: Request, exc: RequestValidationError) -> JSONResponse:
        errors = [
            {"field": ".".join(str(part) for part in error["loc"] if part != "body"), "message": error["msg"]}
            for error in exc.errors()
        ]
        return JSONResponse(error_body("VALIDATION_ERROR", "Request validation failed", errors), status_code=400)

    @app.exception_handler(StarletteHTTPException)
    async def handle_http(_: Request, exc: StarletteHTTPException) -> JSONResponse:
        code, message = _HTTP_ERRORS.get(exc.status_code, ("BAD_REQUEST", "The request is invalid"))
        return JSONResponse(error_body(code, message), status_code=exc.status_code)

    @app.exception_handler(Exception)
    async def handle_unexpected(_: Request, exc: Exception) -> JSONResponse:
        log.exception("Unhandled error")
        return JSONResponse(error_body("INTERNAL_ERROR", "An unexpected error occurred. Please try again later"),
                            status_code=500)

    @app.get("/health")
    async def health() -> dict:
        return {"success": True, "status": "UP"}

    app.include_router(router)
    app.include_router(auth_router)
    @app.get("/sw.js")
    async def sw_js() -> PlainTextResponse:
        return PlainTextResponse(
            '/**\n'
            ' * Service Worker for AB IT TECH Research & Innovation Engine\n'
            ' */\n'
            'self.addEventListener(\"install\", (event) => {\n'
            '  event.waitUntil(\n'
            '    caches.open(\"static-cache\").then((cache) => {\n'
            '      return cache.addAll([\n'
            '        \"/\",\n'
            '        \"/sw.js\",\n'
            '        \"/docs\",\n'
            '        \"/health\",\n'
            '      ]);\n'
            '    })\n'
            '  );\n'
            '});\n'
            'self.addEventListener(\"fetch\", (event) => {\n'
            '  event.respondWith(\n'
            '    caches.match(event.request).then((response) => {\n'
            '      if (response) {\n'
            '        return response;\n'
            '      }\n'
            '      return fetch(event.request);\n'
            '    })\n'
            '  );\n'
            '});',
            media_type="application/javascript"
        )

    return app
