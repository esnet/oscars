
# ███████╗███╗   ██╗██╗   ██╗    ██╗   ██╗ █████╗ ██████╗ ███████╗
# ██╔════╝████╗  ██║██║   ██║    ██║   ██║██╔══██╗██╔══██╗██╔════╝
# █████╗  ██╔██╗ ██║██║   ██║    ██║   ██║███████║██████╔╝███████╗
# ██╔══╝  ██║╚██╗██║╚██╗ ██╔╝    ╚██╗ ██╔╝██╔══██║██╔══██╗╚════██║
# ███████╗██║ ╚████║ ╚████╔╝      ╚████╔╝ ██║  ██║██║  ██║███████║
# ╚══════╝╚═╝  ╚═══╝  ╚═══╝        ╚═══╝  ╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝

##### ocd-stack makefile vars #####
# vars
STACK_REPO ?= ../ocd-stack
# Check if STACK_REPO exists before including
ifeq (,$(wildcard $(STACK_REPO)/makefiles/base.mk))
$(warning WARNING: STACK_REPO $(STACK_REPO) does not exist. ocd-stack make targets will not work.)
orch_compose :=

# This is the first, and therefore _default_ target. I.e., running a bare `make` will print the commands with properly
# formatted help docstrings (Example: `target: ## some help info`)
.PHONY: help
# $MAKEFILE_LIST is a special variable created by GNU make when it runs -- a list of all Makefiles called (include included)
# This function uses awk to parse a help descriptions for targets using comments starting with '##'
help:  ## Print descriptions of all targets in makefile
	@awk 'BEGIN {FS = ":.*?## "} /^[0-9a-zA-Z_-]+:.*?## / {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)
else
include $(STACK_REPO)/makefiles/base.mk
oscars_compose := $(ocd_compose) --profile oscars
endif


.PHONY: mirror-prod-db
mirror-prod-db:  ## Mirror prod db to local db (must be in "docker" group on prod server)
	@echo "creating dump from oscars-prod.es.net..."
	@ssh oscars-prod.es.net 'docker exec -i oscars-db pg_dumpall -U oscars > /tmp/oscars.sql'
	@ssh oscars-prod.es.net 'gzip /tmp/oscars.sql'
	@scp oscars-prod.es.net:/tmp/oscars.sql.gz /tmp/oscars.sql.gz
	@ssh oscars-prod.es.net 'rm /tmp/oscars.sql.gz'
	@echo "stopping local services..."
	@$(oscars_compose) stop oscars-backend
	@$(oscars_compose) start oscars-db
	@echo "clearing local oscars-db"
	@docker cp backend/data/clear.sql oscars-db:/tmp/clear.sql
	@docker exec -it oscars-db /usr/local/bin/psql -Uoscars -f /tmp/clear.sql
	@echo "loading data to local oscars-db"
	@gunzip /tmp/oscars.sql.gz
	@docker cp /tmp/oscars.sql oscars-db:/tmp/oscars.sql
	@docker exec -it oscars-db /usr/local/bin/psql -Uoscars -f /tmp/oscars.sql
	@docker exec -it oscars-db  /usr/local/bin/psql -Uoscars -c "ALTER USER oscars PASSWORD 'oscars';"
	@rm /tmp/oscars.sql
	@echo "restarting local services..."
	@$(oscars_compose) up -d oscars-backend
