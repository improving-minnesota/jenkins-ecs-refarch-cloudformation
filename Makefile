SHELL=/bin/bash

# === Name of the Cloudformation Stack
stack_name=$(shell whoami)-jenkins-ecs

# === Bucket containing S3 objects.
s3_bucket=jenkins-ecs-refarch-cloudformation

# === Bucket URL.
s3_bucket_url=https://s3.amazonaws.com/$(s3_bucket)

# === File containing Template Parameters
parameter_file?=stack_params.json


AWS:=$(shell command -v aws)
UPLOAD_COMMAND=$(AWS) s3 cp
SOURCE_FILES=$(shell find . -type f -name "*.cf.yml" -print)
CFN_TEMPLATES := $(patsubst %.cf.yml,%.yml,$(SOURCE_FILES))


AWS_ACCOUNT_ID?=$(shell $(AWS) sts get-caller-identity --query 'Account' --output text)
AWS_REGION?=us-east-1

export AWS_ACCOUNT_ID
export AWS_REGION

DOCKER:=$(shell command -v docker)
docker_image_tag=1.0.0
DOCKER_TAG?=$(shell whoami)/jenkins:$(docker_image_tag)

vpath %.cf.yml infrastructure
vpath %.cf.yml jenkins

define changeset_id
$(shell echo "$$(whoami)-$$($(AWS) cloudformation describe-stack-events \
				--stack-name $(stack_name) \
				--query 'StackEvents[0].EventId' \
				--output json | cut -d'-' -f5 | cut -d'"' -f1)")
endef


.PHONY: lint upload plan apply create changeset-delete docker docker-publish
.DEFAULT_GOAL: lint


%.yml: %.cf.yml
	$(UPLOAD_COMMAND) $< s3://$(s3_bucket)/$@


create plan apply changeset-delete: changeset_name?=$(changeset_id)


lint: $(SOURCE_FILES)
	for i in $(SOURCE_FILES); do \
		$(AWS) cloudformation validate-template \
			--template-body file://$$i \
			--output table \
			--query 'Description' || echo "$$i Failed"; \
	done


docker:
	$(DOCKER) build \
		--force-rm \
		--compress \
		--tag $(DOCKER_TAG) \
		jenkins


docker-publish: ECR_REPO_URL=$(AWS_ACCOUNT_ID).dkr.ecr.$(AWS_REGION).amazonaws.com/$(ECR_REPO):$(docker_image_tag)
docker-publish:
	$(DOCKER) tag $(DOCKER_TAG) $(ECR_REPO_URL)
	$(DOCKER) push $(ECR_REPO_URL)


create:
	$(AWS) cloudformation create-change-set \
		--stack-name '$(stack_name)' \
		--template-url "$(s3_bucket_url)/stack.yml" \
		--change-set-name '$(changeset_name)' \
		--change-set-type 'CREATE' \
		--description "Changeset: $(changeset_name)" \
		--capabilities 'CAPABILITY_NAMED_IAM' \
		--parameters file://$(parameter_file)


changeset-delete:
	# Delete current changeset if it exists
	$(AWS) cloudformation delete-change-set \
		--change-set-name '$(changeset_name)' \
		--stack-name '$(stack_name)' || true


plan: changeset-delete
	# Create a new changeset
	$(AWS) cloudformation create-change-set \
		--stack-name '$(stack_name)' \
		--template-url "$(s3_bucket_url)/stack.yml" \
		--change-set-name '$(changeset_name)' \
		--description "Changeset: $(changeset_name)" \
		--change-set-type 'UPDATE' \
		--capabilities 'CAPABILITY_NAMED_IAM' \
		--parameters file://$(parameter_file)


apply:
	$(AWS) cloudformation execute-change-set \
		--stack-name '$(stack_name)' \
		--change-set-name '$(changeset_name)'


upload: $(CFN_TEMPLATES)
