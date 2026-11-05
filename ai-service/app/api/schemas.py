"""Request models (validation happens here, before any work is done)."""
from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field, field_validator


class ProjectInput(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="ignore")

    title: str = Field(min_length=3, max_length=300)
    abstract: str | None = Field(default=None, max_length=5000)
    max_results: int = Field(default=10, ge=1, le=20, alias="maxResults",
                             description="Maximum number of papers requested from each source")

    @field_validator("title")
    @classmethod
    def title_not_blank(cls, value: str) -> str:
        value = value.strip()
        if len(value) < 3:
            raise ValueError("title must contain at least 3 characters")
        return value

    @field_validator("abstract")
    @classmethod
    def blank_abstract_is_none(cls, value: str | None) -> str | None:
        return value.strip() or None if value is not None else None
