FROM python:3.12-alpine3.23

WORKDIR /app

# Install build dependencies
RUN apk add --no-cache curl-dev gcc musl-dev libffi-dev openssl-dev

# Copy and install Python requirements
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY app.py .

ENV PORT=5000
EXPOSE 5000

# Health check using curl
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:5000/health || exit 1

# Run with gunicorn (single process to keep sessions in-memory)
CMD ["gunicorn", "--bind", "0.0.0.0:5000", "--workers", "1", "--timeout", "60", "app:app"]
