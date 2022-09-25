build:
	cd lambda && ./mvnw clean package

deploy: build
	cd terraform && terraform init && terraform plan -out=plan.out && terraform apply plan.out
