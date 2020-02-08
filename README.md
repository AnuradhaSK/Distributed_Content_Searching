Navigate to Distributed_Content_Searching directory and issue
`mvn clean install`

Inside Distributed_Content_Searching/target,
distributed_content_searching-1.0-SNAPSHOT.jar will be available after a successful build

Execute as master:

`java -jar distributed_content_searching-1.0-SNAPSHOT.jar 1 
`
Execute as peer:

`java -jar distributed_content_searching-1.0-SNAPSHOT.jar 2`

When Executing as peer nodes, you will be prompted to insert 'IP Address','Communication Port', and 'node username'.

Navigate to 'Distributed_Content_Searching/FileTransferApplication/ folder, one a terminal and issue,
`mvn clean install`
Then,
'filetransfer-0.0.1-SNAPSHOT.jar' file inside 'Distributed_Content_Searching/FileTransferApplication/target' folder should be copied to place where 'distributed_content_searching-1.0-SNAPSHOT.jar' resides

The following commands can be executed via netcat terminal to incoke several functions in Nodes.
First run the following in an terminal.
`nc -u <node_ip> <node_port>`

Then you can use following commands
    SEARCH <no_of_hops> <file_name> : Search a file in the overlay network 
    DOWNLOAD <file_name>            : Download a searched file
    GETSTATS                        : Get number of answered,forwarded,received queries by a node       
    CLEARSTATS                      : Reset answered,forwarded,received querie counts
    SHOWROUTES                      : Show routing table of the node
    LEAVENET                        : Gracefully depart the overlay network