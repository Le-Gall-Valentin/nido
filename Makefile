.PHONY: up down clean reset logs ps

up:
	docker compose --env-file .env up -d

down:
	docker compose down

clean:
	docker compose down -v --remove-orphans

reset: clean up

logs:
	docker compose logs -f

ps:
	docker compose ps