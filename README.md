Navigate to Distributed_Content_Searching directory and issue
`mvn clean install`

Inside Distributed_Content_Searching/target,
distributed_content_searching-1.0-SNAPSHOT.jar will be available after a successful build

Execute as master:

`java -jar distributed_content_searching-1.0-SNAPSHOT.jar 1 
`
Execute as peer:

`java -jar distributed_content_searching-1.0-SNAPSHOT.jar 2`

Navigate to 'Distributed_Content_Searching/FileTransferApplication/ folder, one a terminal and issue,
`mvn clean install`
Then,
'filetransfer-0.0.1-SNAPSHOT.jar' file inside 'Distributed_Content_Searching/FileTransferApplication/target' folder should be copied to place where 'distributed_content_searching-1.0-SNAPSHOT.jar' resides