Navigate to Distributed_Content_Searching directory and issue
`mvn clean install`

Inside Distributed_Content_Searching/target,
distributed_content_searching-1.0-SNAPSHOT.jar will be available after a successful build

Execute as master:

`java -jar distributed_content_searching-1.0-SNAPSHOT.jar 1 
`
Execute as peer:

`java -jar distributed_content_searching-1.0-SNAPSHOT.jar 2 <ip> <port> <username>`