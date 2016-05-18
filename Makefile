CONTAINER_ID  = $(shell docker ps --filter ancestor=wurstmeister/kafka -q)
KAFKA_PATH = "/opt/kafka_2.11-0.9.0.1"

consume:
	docker exec -t -i $(CONTAINER_ID) /bin/bash -c "$(KAFKA_PATH)/bin/kafka-console-consumer.sh --from-beginning --zookeeper zk:2181 --topic beregn" | tee kafka-out.log

stop:
	docker-compose down

clean:
	docker rm -f $$(docker ps -a -q) || true
	rm kafka-out.log
	rm -rf logs/*

clean-stop: stop clean

submit-job:
	docker exec -it  batchvur_batch_vur_1  bash -c 'BATCH_VUR_TENANCY_ID=52a36193-7942-4606-a783-15e098f15f0f java -cp batch-vur-0.1.0-SNAPSHOT-standalone.jar batch_vur.core'

check_output:
	cat kafka-out.log| grep -oe ':[0-9]*,' | grep -oe '[0-9]*' | sort | uniq -d
	cat kafka-out.log| grep -oe ':[0-9]*,' | grep -oe '[0-9]*' | sort | uniq -d | wc -l
	cat kafka-out.log| grep -oe ':[0-9]*,' | grep -oe '[0-9]*' | wc -l

reset:
	lein uberjar
	docker-compose build
	docker-compose down
	docker-compose down
	rm -rf logs/*
	docker-compose up
#	docker-compose scale batch_vur=3
