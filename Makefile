REGISTRY	:= docker.io
USERNAME	:= maritim
VERSION		?= dev
TMP_DIR		:= /tmp/webmention-build

IMAGES := webmention-cli webmention-service

.PHONY: all login clean $(IMAGES)

# Default target
all: $(IMAGES)

$(IMAGES):
	@echo "Building $@:$(VERSION)..."
	podman build \
		-f $@/Dockerfile \
		-t $(REGISTRY)/$(USERNAME)/$@:$(VERSION) \
		-t $(REGISTRY)/$(USERNAME)/$@:latest \
		.

push-%:
	podman push $(REGISTRY)/$(USERNAME)/$*:$(VERSION)
	podman push $(REGISTRY)/$(USERNAME)/$*:latest

release-%:
	rm -rf $(TMP_DIR)
	git clone --branch webmention-parent-$(VERSION) --depth 1 . $(TMP_DIR)
	podman build \
		-f $(TMP_DIR)/$*/Dockerfile \
		-t $(REGISTRY)/$(USERNAME)/$*:$(VERSION) \
		-t $(REGISTRY)/$(USERNAME)/$*:latest \
		$(TMP_DIR)
	rm -rf $(TMP_DIR)

login:
	podman login $(REGISTRY)

clean:
	rm -rf $(TMP_DIR)