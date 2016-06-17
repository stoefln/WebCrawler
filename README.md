# WebCrawler

This is just a dumb webcrawler proof of concept for using the Jade Agent plattform.
CrawlAgents can run on different servers and send their crawl information back to the DatabaseAgent which is collecting and storing all the information. To run a test start the Jade plattform first:
`java -cp lib/jade-bin/lib/jade.jar jade.Boot -gui`
and then start the DataBaseAgent. You can tell the the DatabaseAgent to spawn Crawlagents automatically so you don't have to do it by yourself.
Start it via `DatabaseAgent(startUrl,[filter],[number of CrawlAgents])`

### Example:

`java -cp lib/jade-bin/lib/jade.jar:out/production/WebCrawler:lib/jsoup-1.6.1.jar jade.Boot -container -host 127.0.0.1 -agents foo:net.microtrash.DatabaseAgent\(https://www.willhaben.at/iad/immobilien/mietwohnungen/mietwohnung-angebote,iad/immobilien/mietwohnungen/mietwohnung-angebote,3\)`