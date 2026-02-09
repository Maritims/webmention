# Makefile for webmention

PROJECT_NAME	:= webmention-service
REGISTRY		:= docker.io
REPOSITORY		:= maritim
TMP_DIR			:= /tmp/webmention-build

.PHONY: build-dev-image build-prod-image clean-image

build-dev-image:
	podman build -f $(PROJECT_NAME)/Dockerfile -t $(PROJECT_NAME):dev .

build-prod-image:
	@if [ -z "$(VERSION)" ]; then \
  		echo "Error: You must provide a VERSION (e.g., make build-prod-image VERSION=0.0.1-alpha.1" \
  		exit 1; \
	else \
		rm -rf $(TMP_DIR); \
		mkdir -p $(TMP_DIR); \
		git clone --branch "webmention-parent-$(VERSION)" --depth 1 . $(TMP_DIR); \
		podman build \
			-f $(PROJECT_NAME)/Dockerfile \
			-t $(REGISTRY)/$(REPOSITORY)/$(PROJECT_NAME):$(VERSION) \
			$(TMP_DIR); \
		podman build \
        			-f $(PROJECT_NAME)/Dockerfile \
        			-t $(REGISTRY)/$(REPOSITORY)/$(PROJECT_NAME):latest \
        			$(TMP_DIR); \
		rm -rf $(TMP_DIR); \
	fi

run-image:
	@if [ -z "$(VERSION)" ]; then \
  		echo "Error: You must provide a VERSION (e.g., make run-image VERSION=0.0.1-alpha.1 to run 0.0.1-alpha.1, or just make run-image VERSION=dev to run the current development image" \
  		exit 1; \
	else \
		podman run \
			-e WEBMENTION_SUPPORTED_DOMAINS=localhost \
			-e WEBMENTION_DB_CONNECTION_STRING=jdbc:sqlite:/app/data/webmentions.db \
			-e WEBMENTION_SENDER_EMAIL_ADDRESS=no-reply@localhost \
			-e WEBMENTION_RECIPIENT_EMAIL_ADDRESS=no-reply@localhost \
			-e WEBMENTION_RESEND_API_KEY=foobar \
			$(PROJECT_NAME):$(VERSION); \
	fi

clean:
	rm -rf $(TMP_DIR)