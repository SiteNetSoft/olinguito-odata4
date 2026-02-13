# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.
#
# Copyright 2026 SiteNetSoft - MkDocs documentation build targets

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
