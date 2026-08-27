import logging
import os
from functools import lru_cache
from io import BytesIO
from urllib.parse import urlparse

from fastapi import FastAPI, HTTPException
from minio import Minio
from pydantic import BaseModel

from docling.datamodel.base_models import DocumentStream, InputFormat
from docling.datamodel.pipeline_options import PdfPipelineOptions
from docling.document_converter import DocumentConverter, PdfFormatOption

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("markdown-worker")

app = FastAPI(title="book2quiz markdown worker")


class ConvertRequest(BaseModel):
    bucket: str
    objectKey: str


class ConvertResponse(BaseModel):
    markdown: str


@lru_cache(maxsize=1)
def get_minio_client() -> Minio:
    internal_url = os.environ.get("MINIO_INTERNAL_URL", "http://minio:9000")
    parsed = urlparse(internal_url)
    endpoint = parsed.netloc or parsed.path
    secure = parsed.scheme == "https"
    return Minio(
        endpoint,
        access_key=os.environ["MINIO_ACCESS_KEY"],
        secret_key=os.environ["MINIO_SECRET_KEY"],
        secure=secure,
    )


@lru_cache(maxsize=1)
def get_converter() -> DocumentConverter:
    pipeline_options = PdfPipelineOptions()
    pipeline_options.do_ocr = True
    pipeline_options.do_table_structure = True
    return DocumentConverter(
        format_options={
            InputFormat.PDF: PdfFormatOption(pipeline_options=pipeline_options)
        }
    )


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.post("/convert", response_model=ConvertResponse)
def convert(request: ConvertRequest) -> ConvertResponse:
    logger.info("Converting %s/%s", request.bucket, request.objectKey)

    try:
        response = get_minio_client().get_object(request.bucket, request.objectKey)
        try:
            pdf_bytes = response.read()
        finally:
            response.close()
            response.release_conn()
    except Exception as exc:
        logger.exception("Failed to download %s/%s", request.bucket, request.objectKey)
        raise HTTPException(status_code=502, detail=f"MinIO download failed: {exc}") from exc

    try:
        source = DocumentStream(name=request.objectKey, stream=BytesIO(pdf_bytes))
        result = get_converter().convert(source)
        markdown = result.document.export_to_markdown()
    except Exception as exc:
        logger.exception("Docling conversion failed for %s/%s", request.bucket, request.objectKey)
        raise HTTPException(status_code=500, detail=f"Conversion failed: {exc}") from exc

    logger.info("Converted %s/%s -> %d markdown chars",
                request.bucket, request.objectKey, len(markdown))
    return ConvertResponse(markdown=markdown)
