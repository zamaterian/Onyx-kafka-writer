make file requires gnu make


1) lein uberjar
2) docker-compose build 

## do each step in a new terminal

1) docker-compose up

## consume from topic with  kafka console consumer, writes into a log file
1) make consume


### submit the job to the zookeeper 
1) make submit-job

## checks the output there should be 0 duplicates no chance of more since only 1 peer
## counts the lines in the log, must be 10.000
make check_output


