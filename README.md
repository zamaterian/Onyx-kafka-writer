make file requires gnu make


lein uberjar
docker-compose build 

# do each step in a new terminal

##
docker-compose up

## consume from topic with  kafka console consumer, writes into a log file
make consume


## submit the job to the zookeeper 
make submit-job

## checks the output there should be 0 duplicates no chance of more since only 1 peer
## counts the lines in the log, must be 10.000
make check_output

# Alternative use, for example without msg loss

make submit-job-no-loss 

