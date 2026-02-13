.PHONY: docs-install docs-serve docs-build docs-clean

VENV_DIR := .venv
PYTHON := $(VENV_DIR)/bin/python
PIP := $(VENV_DIR)/bin/pip
MKDOCS := $(VENV_DIR)/bin/mkdocs

$(VENV_DIR):
	python3 -m venv $(VENV_DIR)

docs-install: $(VENV_DIR)
	$(PIP) install --upgrade pip
	$(PIP) install mkdocs mkdocs-material pymdown-extensions

docs-serve: $(VENV_DIR)
	$(MKDOCS) serve

docs-build: $(VENV_DIR)
	$(MKDOCS) build --strict

docs-clean:
	rm -rf site/
